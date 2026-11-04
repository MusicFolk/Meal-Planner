package lt.vilniustech.meal_planner.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.ExportedPlanDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.GenerationStatusDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.MealPlanDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.RecipeDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.ShoppingListDto;
import lt.vilniustech.meal_planner.entity.Client;
import lt.vilniustech.meal_planner.entity.Dish;
import lt.vilniustech.meal_planner.entity.MealPlan;
import lt.vilniustech.meal_planner.entity.ShoppingListItem;
import lt.vilniustech.meal_planner.enums.SubscriptionStatus;
import lt.vilniustech.meal_planner.exception.ApiException;
import lt.vilniustech.meal_planner.mapper.MealPlanMapper;
import lt.vilniustech.meal_planner.repository.DishRepository;
import lt.vilniustech.meal_planner.repository.MealPlanRepository;
import lt.vilniustech.meal_planner.repository.ShoppingListItemRepository;
import lt.vilniustech.meal_planner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MealPlanService implements IMealPlans {

    static final String NO_QUESTIONNAIRE = "Norėdami generuoti planą, pirmiausia užpildykite anketą.";
    static final String SUBSCRIPTION_EXPIRED =
            "Prenumerata nebegalioja. Norėdami generuoti planą, atnaujinkite prenumeratą.";
    static final String LIMIT_REACHED = "Šiandien planą galite generuoti dar 0 kartų. Bandykite rytoj.";

    private final MealPlanRepository plans;
    private final DishRepository dishes;
    private final ShoppingListItemRepository shoppingItems;
    private final UserRepository users;
    private final IQuestionnaire questionnaire;
    private final ISubscription subscription;
    private final DemoPlanGenerator generator;
    private final Clock clock;
    private final int dailyLimit;

    public MealPlanService(MealPlanRepository plans, DishRepository dishes, ShoppingListItemRepository shoppingItems,
                           UserRepository users, IQuestionnaire questionnaire, ISubscription subscription,
                           DemoPlanGenerator generator, Clock clock,
                           @Value("${app.plans.daily-generation-limit}") int dailyLimit) {
        this.plans = plans;
        this.dishes = dishes;
        this.shoppingItems = shoppingItems;
        this.users = users;
        this.questionnaire = questionnaire;
        this.subscription = subscription;
        this.generator = generator;
        this.clock = clock;
        this.dailyLimit = dailyLimit;
    }

    @Transactional(readOnly = true)
    public Optional<MealPlanDto> activePlan(Long clientId) {
        return plans.findByClientIdAndActiveTrue(clientId).map(MealPlanMapper::toDto);
    }

    /** Patikrina anketą, prenumeratą ir dienos generavimų limitą (FR-08). */
    @Transactional(readOnly = true)
    public GenerationStatusDto generationStatus(Long clientId) {
        boolean questionnaireFilled = questionnaire.dailyCalories(clientId).isPresent();
        SubscriptionStatus status = subscription.status(clientId);
        Instant startOfDay = LocalDate.now(clock).atStartOfDay(clock.getZone()).toInstant();
        int remaining = (int) Math.max(0, dailyLimit - plans.countByClientIdAndCreatedAtGreaterThanEqual(clientId,
                startOfDay));
        String reason = null;
        if (!questionnaireFilled) {
            reason = NO_QUESTIONNAIRE;
        } else if (status == SubscriptionStatus.EXPIRED) {
            reason = SUBSCRIPTION_EXPIRED;
        } else if (remaining == 0) {
            reason = LIMIT_REACHED;
        }
        return new GenerationStatusDto(reason == null, reason, questionnaireFilled, status, remaining, dailyLimit);
    }

    /**
     * Sudaro naują savaitės planą ir pažymi jį aktualiu; ankstesnis planas lieka DB (FR-15, FR-16).
     * Visas planas išsaugomas vienoje transakcijoje (NFR-25).
     */
    public MealPlanDto generate(Long clientId) {
        GenerationStatusDto status = generationStatus(clientId);
        if (!status.canGenerate()) {
            throw new ApiException(HttpStatus.CONFLICT, status.reason());
        }
        Client client = users.findById(clientId).map(Client.class::cast)
                .orElseThrow(() -> ApiException.notFound("Klientas nerastas."));
        int dailyCalories = questionnaire.dailyCalories(clientId).orElseThrow();
        MealPlan plan = generator.generate(client, dailyCalories, clock.instant());
        plans.deactivateAll(clientId);
        return MealPlanMapper.toDto(plans.save(plan));
    }

    @Transactional(readOnly = true)
    public RecipeDto recipe(Long clientId, Long dishId) {
        Dish dish = dishes.findById(dishId).orElseThrow(() -> ApiException.notFound("Patiekalas nerastas."));
        requireOwner(dish.getMeal().getPlanDay().getMealPlan(), clientId);
        return MealPlanMapper.toRecipe(dish);
    }

    @Transactional(readOnly = true)
    public ShoppingListDto shoppingList(Long clientId, Long planId) {
        MealPlan plan = plans.findById(planId).orElseThrow(() -> ApiException.notFound("Planas nerastas."));
        requireOwner(plan, clientId);
        return MealPlanMapper.toDto(plan.getShoppingList());
    }

    /** Išsaugo produktų sąrašo varnelę serveryje (FR-20). */
    public void setPurchased(Long clientId, Long itemId, boolean purchased) {
        ShoppingListItem item = shoppingItems.findById(itemId)
                .orElseThrow(() -> ApiException.notFound("Produktas nerastas."));
        requireOwner(item.getShoppingList().getMealPlan(), clientId);
        item.setPurchased(purchased);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> activePlanId(Long clientId) {
        return plans.findByClientIdAndActiveTrue(clientId).map(MealPlan::getId);
    }

    @Override
    @Transactional(readOnly = true)
    public MealPlanDto planView(Long planId) {
        return MealPlanMapper.toDto(plans.findById(planId)
                .orElseThrow(() -> ApiException.notFound("Planas nerastas.")));
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeDto recipeView(Long planId, Long dishId) {
        Dish dish = dishes.findById(dishId).orElseThrow(() -> ApiException.notFound("Patiekalas nerastas."));
        if (!dish.getMeal().getPlanDay().getMealPlan().getId().equals(planId)) {
            throw ApiException.notFound("Patiekalas nerastas.");
        }
        return MealPlanMapper.toRecipe(dish);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean dishBelongsToPlan(Long dishId, Long planId) {
        return dishes.findById(dishId)
                .map(d -> d.getMeal().getPlanDay().getMealPlan().getId().equals(planId))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExportedPlanDto> exportPlans(Long clientId) {
        return plans.findByClientIdOrderByCreatedAtDesc(clientId).stream()
                .map(plan -> new ExportedPlanDto(MealPlanMapper.toDto(plan),
                        plan.getDays().stream().flatMap(day -> day.getMealsInOrder().stream())
                                .map(meal -> MealPlanMapper.toRecipe(meal.getDish())).toList(),
                        MealPlanMapper.toDto(plan.getShoppingList())))
                .toList();
    }

    /** Klientas mato tik savo duomenis (NFR-54). */
    private static void requireOwner(MealPlan plan, Long clientId) {
        if (plan.getClient() == null || !plan.getClient().getId().equals(clientId)) {
            throw ApiException.notFound("Planas nerastas.");
        }
    }
}
