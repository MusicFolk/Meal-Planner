package lt.vilniustech.meal_planner.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lt.vilniustech.meal_planner.ai.AiMealRequest;
import lt.vilniustech.meal_planner.ai.AiMealRequest.AllowedProduct;
import lt.vilniustech.meal_planner.ai.AiMealResponse;
import lt.vilniustech.meal_planner.ai.AiMealResponse.AiDish;
import lt.vilniustech.meal_planner.ai.AiMealResponse.AiIngredient;
import lt.vilniustech.meal_planner.ai.IAIAgent;
import lt.vilniustech.meal_planner.calories.ICalorieCalculator;
import lt.vilniustech.meal_planner.calories.MealTargets;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.GenerationProgressDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.GenerationStatusDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.MealPlanDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.MealProgressDto;
import lt.vilniustech.meal_planner.entity.AiGenerationRequest;
import lt.vilniustech.meal_planner.entity.Client;
import lt.vilniustech.meal_planner.entity.Dish;
import lt.vilniustech.meal_planner.entity.Ingredient;
import lt.vilniustech.meal_planner.entity.Meal;
import lt.vilniustech.meal_planner.entity.MealPlan;
import lt.vilniustech.meal_planner.entity.NutritionValue;
import lt.vilniustech.meal_planner.entity.PlanDay;
import lt.vilniustech.meal_planner.entity.PlanGeneration;
import lt.vilniustech.meal_planner.entity.Product;
import lt.vilniustech.meal_planner.enums.AiRequestResult;
import lt.vilniustech.meal_planner.enums.GenerationStatus;
import lt.vilniustech.meal_planner.enums.MealType;
import lt.vilniustech.meal_planner.enums.SubscriptionStatus;
import lt.vilniustech.meal_planner.exception.ApiException;
import lt.vilniustech.meal_planner.mapper.MealPlanMapper;
import lt.vilniustech.meal_planner.repository.AiGenerationRequestRepository;
import lt.vilniustech.meal_planner.repository.MealPlanRepository;
import lt.vilniustech.meal_planner.repository.PlanGenerationRepository;
import lt.vilniustech.meal_planner.repository.UserRepository;
import lt.vilniustech.meal_planner.service.IQuestionnaire.GenerationInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Mitybos plano generavimas su DI (PLN-2–PLN-5, FR-08–FR-16, 25 ir 41 pav.).
 *
 * <p>Trys valgymo tipai generuojami lygiagrečiai; netinkamo valgymo tipo užklausa kartojama iki 3 kartų.
 * DI kvietimai trunka iki 25 s, todėl klasė nėra transakcinė: generavimo būsena ir kiekvienas DI kvietimas
 * išsaugomi atskiromis trumpomis transakcijomis (ir nesėkmės atveju), o tinkamas planas – vienoje
 * transakcijoje (NFR-25).
 */
@Service
public class PlanGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PlanGenerationService.class);

    static final String NO_QUESTIONNAIRE = "Norėdami generuoti planą, pirmiausia užpildykite anketą.";
    static final String SUBSCRIPTION_EXPIRED =
            "Prenumerata nebegalioja. Norėdami generuoti planą, atnaujinkite prenumeratą.";
    static final String LIMIT_REACHED =
            "Šiandien planą galite generuoti dar 0 kartų. Vėl galėsite generuoti %s nuo 00:00.";
    static final String IN_PROGRESS = "Planas jau generuojamas. Palaukite, kol generavimas baigsis.";
    static final String GENERATION_FAILED = "Plano sugeneruoti nepavyko. Ankstesnis planas nepakeistas.";

    /** Kiekvienam valgymo tipui prašoma 7 skirtingų patiekalų – po vieną kiekvienai savaitės dienai (FR-10). */
    static final int DAYS = 7;

    private final PlanGenerationRepository generations;
    private final AiGenerationRequestRepository aiRequests;
    private final MealPlanRepository plans;
    private final UserRepository users;
    private final IQuestionnaire questionnaire;
    private final ISubscription subscription;
    private final ICalorieCalculator calculator;
    private final PlanCalculator planCalculator;
    private final IAIAgent agent;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final IAiConfiguration aiConfiguration;
    private final Duration callTimeout;
    private final Duration generationTimeout;
    private final int maxAttempts;
    private final ZoneId zone;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public PlanGenerationService(PlanGenerationRepository generations, AiGenerationRequestRepository aiRequests,
                                 MealPlanRepository plans, UserRepository users, IQuestionnaire questionnaire,
                                 ISubscription subscription, ICalorieCalculator calculator,
                                 PlanCalculator planCalculator, IAIAgent agent, IAiConfiguration aiConfiguration,
                                 TransactionTemplate tx, Clock clock,
                                 @Value("${app.ai.call-timeout}") Duration callTimeout,
                                 @Value("${app.ai.generation-timeout}") Duration generationTimeout,
                                 @Value("${app.ai.max-attempts}") int maxAttempts,
                                 @Value("${app.time-zone}") ZoneId zone) {
        this.generations = generations;
        this.aiRequests = aiRequests;
        this.plans = plans;
        this.users = users;
        this.questionnaire = questionnaire;
        this.subscription = subscription;
        this.calculator = calculator;
        this.planCalculator = planCalculator;
        this.agent = agent;
        this.tx = tx;
        this.clock = clock;
        this.aiConfiguration = aiConfiguration;
        this.callTimeout = callTimeout;
        this.generationTimeout = generationTimeout;
        this.maxAttempts = maxAttempts;
        this.zone = zone;
    }

    /**
     * Patikrina anketą, prenumeratą, dienos generavimų limitą ir ar nevyksta kitas generavimas (FR-08, PLN-2).
     * Į limitą įskaičiuojami tik šios kalendorinės dienos (Lietuvos laiku) sėkmingi generavimai (FR-15).
     */
    public GenerationStatusDto status(Long clientId) {
        return tx.execute(s -> {
            boolean questionnaireFilled = questionnaire.dailyCalories(clientId).isPresent();
            SubscriptionStatus status = subscription.status(clientId);
            boolean inProgress = runningGeneration(clientId).isPresent();
            LocalDate today = LocalDate.now(clock.withZone(zone));
            Instant startOfDay = today.atStartOfDay(zone).toInstant();
            // Limitą nustato administratorius; nauji parametrai taikomi nuo kito generavimo (FR-41)
            int dailyLimit = aiConfiguration.dailyGenerationLimit();
            long successfulToday = generations.countByClientIdAndStatusAndStartedAtGreaterThanEqual(clientId,
                    GenerationStatus.SUCCESS, startOfDay);
            int remaining = (int) Math.max(0, dailyLimit - successfulToday);
            String reason = null;
            if (!questionnaireFilled) {
                reason = NO_QUESTIONNAIRE;
            } else if (status == SubscriptionStatus.EXPIRED) {
                reason = SUBSCRIPTION_EXPIRED;
            } else if (inProgress) {
                reason = IN_PROGRESS;
            } else if (remaining == 0) {
                // Klientui nurodoma, kada jis vėl galės generuoti planą (NI-20)
                reason = LIMIT_REACHED.formatted(today.plusDays(1));
            }
            return new GenerationStatusDto(reason == null, reason, questionnaireFilled, status, remaining,
                    dailyLimit, inProgress);
        });
    }

    /** Paskutinio generavimo eiga pagal valgymo tipus – progreso indikatoriui (FR-09, 8 pav. b). */
    public Optional<GenerationProgressDto> progress(Long clientId) {
        return tx.execute(s -> generations.findFirstByClientIdOrderByStartedAtDesc(clientId).map(gen -> {
            Map<MealType, List<AiGenerationRequest>> byType = aiRequests.findByPlanGenerationIdOrderByIdAsc(gen.getId())
                    .stream().collect(Collectors.groupingBy(AiGenerationRequest::getMealType,
                            () -> new EnumMap<>(MealType.class), Collectors.toList()));
            List<MealProgressDto> meals = new ArrayList<>();
            for (MealType type : MealType.values()) {
                List<AiGenerationRequest> attempts = byType.getOrDefault(type, List.of());
                AiRequestResult last = attempts.isEmpty() ? null : attempts.getLast().getResult();
                meals.add(new MealProgressDto(type, attempts.size(), last, last == AiRequestResult.VALID));
            }
            return new GenerationProgressDto(gen.getId(), gen.getStatus(), gen.getStartedAt(), meals);
        }));
    }

    /**
     * Sugeneruoja naują savaitės planą su DI ir nustato jį aktualiu (FR-10–FR-16). Nepavykus aktualus planas
     * nekeičiamas, generavimas pažymimas nesėkmingu ir į dienos limitą neįskaičiuojamas (FR-14).
     */
    public MealPlanDto generate(Long clientId) {
        Long generationId = start(clientId);
        try {
            GenerationInput input = questionnaire.generationInput(clientId)
                    .orElseThrow(() -> new GenerationFailure("Nėra anketos"));
            MealTargets targets = calculator.mealTargets(input.dailyCalories());
            Map<Long, Product> allowed = input.allowedProducts().stream()
                    .collect(Collectors.toMap(Product::getId, Function.identity()));
            List<AllowedProduct> productList = input.allowedProducts().stream()
                    .map(p -> new AllowedProduct(p.getId(), p.getName(), p.getNutritionPer100g().getKcal()))
                    .toList();
            Instant deadline = clock.instant().plus(generationTimeout);

            // Trys valgymo tipų DI kvietimai vykdomi lygiagrečiai (NFR-31)
            Map<MealType, CompletableFuture<MealTypeOutcome>> futures = new EnumMap<>(MealType.class);
            for (MealType type : MealType.values()) {
                int target = target(targets, type);
                futures.put(type, CompletableFuture.supplyAsync(
                        () -> generateMealType(generationId, type, target, productList, allowed, deadline), executor));
            }
            Map<MealType, List<ScaledDish>> dishes = new EnumMap<>(MealType.class);
            for (var entry : futures.entrySet()) {
                MealTypeOutcome outcome = entry.getValue()
                        .get(generationTimeout.plusSeconds(5).toMillis(), TimeUnit.MILLISECONDS);
                if (outcome.dishes() == null) {
                    throw new GenerationFailure(entry.getKey() + ": " + outcome.lastResult() + " po "
                            + outcome.attempts() + " bandymų");
                }
                dishes.put(entry.getKey(), outcome.dishes());
            }

            MealPlan plan = buildPlan(input.dailyCalories(), targets, dishes);
            planCalculator.calculate(plan);
            String problem = checkPlan(plan, input);
            if (problem != null) {
                // ±10 % patikros nesėkmė – generavimas iš karto baigiamas klaida (FR-13)
                throw new GenerationFailure(problem);
            }
            return save(clientId, generationId, plan);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            String reason = e instanceof GenerationFailure ? e.getMessage() : e.getClass().getSimpleName() + ": "
                    + e.getMessage();
            log.info("Plano generavimas {} nepavyko: {}", generationId, reason);
            finishFailed(generationId, reason);
            throw new ApiException(HttpStatus.BAD_GATEWAY, GENERATION_FAILED);
        }
    }

    /** Sukuria generavimo įrašą su būsena IN_PROGRESS, jei klientui leidžiama generuoti (FR-08, PLN-2). */
    private Long start(Long clientId) {
        GenerationStatusDto status = status(clientId);
        if (!status.canGenerate()) {
            throw new ApiException(HttpStatus.CONFLICT, status.reason());
        }
        try {
            return tx.execute(s -> {
                Client client = users.findById(clientId).map(Client.class::cast)
                        .orElseThrow(() -> ApiException.notFound("Klientas nerastas."));
                return generations.saveAndFlush(new PlanGeneration(client, agent.modelName(), clock.instant()))
                        .getId();
            });
        } catch (DataIntegrityViolationException e) {
            // Tuo pačiu metu pradėtas kitas generavimas (unikalus indeksas pagal IN_PROGRESS)
            throw new ApiException(HttpStatus.CONFLICT, IN_PROGRESS);
        }
    }

    /**
     * Vykdomas generavimas. Nutrūkęs generavimas (pvz., perkrovus serverį) po laiko ribos laikomas nesėkmingu,
     * kad neblokuotų naujų generavimų.
     */
    private Optional<PlanGeneration> runningGeneration(Long clientId) {
        Optional<PlanGeneration> running = generations.findFirstByClientIdAndStatus(clientId,
                GenerationStatus.IN_PROGRESS);
        Instant staleBefore = clock.instant().minus(generationTimeout.plusSeconds(30));
        if (running.isPresent() && running.get().getStartedAt().isBefore(staleBefore)) {
            running.get().fail("Generavimas nutrūko", clock.instant());
            return Optional.empty();
        }
        return running;
    }

    /**
     * Vieno valgymo tipo patiekalų generavimas (41 pav.): DI kvietimas, produktų ID patikra, maistinė vertė,
     * porcijų koregavimas; kiekvieno bandymo rezultatas išsaugomas DB. Kartojama, kol bus atlikti 3 bandymai
     * arba praeis viso generavimo laiko riba (FR-14).
     */
    MealTypeOutcome generateMealType(Long generationId, MealType type, int target, List<AllowedProduct> productList,
                                     Map<Long, Product> allowed, Instant deadline) {
        AiRequestResult last = null;
        int attempt = 0;
        while (attempt < maxAttempts) {
            Duration remaining = Duration.between(clock.instant(), deadline);
            if (remaining.toMillis() < 1000) {
                break;
            }
            attempt++;
            Duration timeout = remaining.compareTo(callTimeout) < 0 ? remaining : callTimeout;
            AiMealResponse response = agent.generateMeals(new AiMealRequest(type, target, DAYS, productList),
                    timeout);
            DishCheck check = response.valid() ? checkDishes(response.dishes(), target, allowed)
                    : new DishCheck(response.result(), null);
            last = check.result();
            saveAiRequest(generationId, type, attempt, target, response, check.result());
            if (check.dishes() != null) {
                return new MealTypeOutcome(check.dishes(), last, attempt);
            }
        }
        return new MealTypeOutcome(null, last == null ? AiRequestResult.TIMEOUT : last, attempt);
    }

    /**
     * Patikrina, ar visi produktų ID yra leistinų produktų sąraše (FR-12, PRD-3), apskaičiuoja patiekalų
     * kaloringumą pagal produktų DB (PRD-4) ir proporcingai pakoreguoja porcijas iki valgymo tikslo
     * (koeficientas 0,7–1,3, FR-13, KAL-4).
     */
    DishCheck checkDishes(List<AiDish> aiDishes, int target, Map<Long, Product> allowed) {
        List<ScaledDish> result = new ArrayList<>();
        for (AiDish aiDish : aiDishes) {
            NutritionValue.Accumulator sum = NutritionValue.Accumulator.empty();
            for (AiIngredient ingredient : aiDish.ingredients()) {
                Product product = allowed.get(ingredient.productId());
                if (product == null) {
                    return new DishCheck(AiRequestResult.INVALID_PRODUCT, null);
                }
                sum = sum.plus(product.getNutritionPer100g().forGrams(ingredient.grams()));
            }
            int kcal = sum.toNutritionValue().getKcal();
            if (kcal <= 0) {
                return new DishCheck(AiRequestResult.PORTION_OUT_OF_RANGE, null);
            }
            double factor = calculator.portionFactor(target, kcal);
            if (!calculator.portionFactorAllowed(factor)) {
                return new DishCheck(AiRequestResult.PORTION_OUT_OF_RANGE, null);
            }
            List<ScaledIngredient> ingredients = aiDish.ingredients().stream()
                    .map(i -> new ScaledIngredient(allowed.get(i.productId()),
                            Math.max(1, (int) Math.round(i.grams() * factor))))
                    .toList();
            result.add(new ScaledDish(aiDish.name(), ingredients, aiDish.steps()));
        }
        return new DishCheck(AiRequestResult.VALID, result);
    }

    /** Iš trijų valgymo tipų patiekalų sudaromas 7 dienų × 3 valgymų planas (FR-11). */
    static MealPlan buildPlan(int dailyCalories, MealTargets targets, Map<MealType, List<ScaledDish>> dishes) {
        MealPlan plan = new MealPlan(null, null, dailyCalories, true);
        for (int day = 1; day <= DAYS; day++) {
            PlanDay planDay = plan.addDay(day);
            for (MealType type : MealType.values()) {
                ScaledDish source = dishes.get(type).get(day - 1);
                Dish dish = planDay.addMeal(type, target(targets, type)).setDish(source.name());
                source.ingredients().forEach(i -> dish.getRecipe().addIngredient(i.product(), i.grams()));
                source.steps().forEach(step -> dish.getRecipe().addStep(step));
            }
        }
        return plan;
    }

    /**
     * Plano patikra (FR-13, PLN-4): kiekvienos dienos nuokrypis ne didesnis kaip ±10 %, nėra kliento alergenų
     * ir nemėgstamų produktų, kiekvienas patiekalas turi receptą. Grąžina problemos aprašą arba null.
     */
    String checkPlan(MealPlan plan, GenerationInput input) {
        for (PlanDay day : plan.getDays()) {
            if (!calculator.deviationAllowed(day.getDeviationPercent())) {
                return "Dienos " + day.getDayNumber() + " nuokrypis " + day.getDeviationPercent() + " %";
            }
            for (Meal meal : day.getMeals()) {
                Dish dish = meal.getDish();
                if (dish.getRecipe().getIngredients().isEmpty() || dish.getRecipe().getSteps().isEmpty()) {
                    return "Patiekalas „" + dish.getName() + "“ neturi recepto";
                }
                for (Ingredient ingredient : dish.getRecipe().getIngredients()) {
                    if (input.forbiddenProductIds().contains(ingredient.getProduct().getId())) {
                        return "Patiekale „" + dish.getName() + "“ yra draudžiamas produktas";
                    }
                }
            }
        }
        return null;
    }

    /** Tinkamas planas išsaugomas vienoje transakcijoje; ankstesnis planas lieka DB neaktualus (FR-15, PLN-5). */
    private MealPlanDto save(Long clientId, Long generationId, MealPlan plan) {
        return tx.execute(s -> {
            Client client = users.findById(clientId).map(Client.class::cast)
                    .orElseThrow(() -> ApiException.notFound("Klientas nerastas."));
            plan.assignTo(client, clock.instant());
            plans.deactivateAll(clientId);
            MealPlan saved = plans.save(plan);
            generations.findById(generationId).orElseThrow().succeed(saved, clock.instant());
            return MealPlanMapper.toDto(saved);
        });
    }

    private void finishFailed(Long generationId, String reason) {
        tx.executeWithoutResult(s -> generations.findById(generationId)
                .ifPresent(gen -> gen.fail(reason, clock.instant())));
    }

    private void saveAiRequest(Long generationId, MealType type, int attempt, int target, AiMealResponse response,
                               AiRequestResult result) {
        tx.executeWithoutResult(s -> aiRequests.save(new AiGenerationRequest(
                generations.getReferenceById(generationId), type, attempt, target, response.requestJson(),
                response.responseJson(), result, response.durationMs(), clock.instant())));
    }

    private static int target(MealTargets targets, MealType type) {
        return switch (type) {
            case BREAKFAST -> targets.breakfast();
            case LUNCH -> targets.lunch();
            case DINNER -> targets.dinner();
        };
    }

    record ScaledIngredient(Product product, int grams) {
    }

    record ScaledDish(String name, List<ScaledIngredient> ingredients, List<String> steps) {
    }

    record DishCheck(AiRequestResult result, List<ScaledDish> dishes) {
    }

    /** Valgymo tipo rezultatas: pakoreguoti patiekalai arba null, jei visi bandymai nepavyko. */
    record MealTypeOutcome(List<ScaledDish> dishes, AiRequestResult lastResult, int attempts) {
    }

    private static class GenerationFailure extends RuntimeException {

        GenerationFailure(String message) {
            super(message);
        }
    }
}
