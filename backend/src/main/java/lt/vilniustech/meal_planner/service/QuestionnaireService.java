package lt.vilniustech.meal_planner.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import lt.vilniustech.meal_planner.calories.CalorieResult;
import lt.vilniustech.meal_planner.calories.ICalorieCalculator;
import lt.vilniustech.meal_planner.dto.CatalogDtos.AllergenDto;
import lt.vilniustech.meal_planner.dto.CatalogDtos.ProductDto;
import lt.vilniustech.meal_planner.dto.QuestionnaireDtos.CalorieExplanation;
import lt.vilniustech.meal_planner.dto.QuestionnaireDtos.Metrics;
import lt.vilniustech.meal_planner.dto.QuestionnaireDtos.QuestionnaireRequest;
import lt.vilniustech.meal_planner.dto.QuestionnaireDtos.QuestionnaireResponse;
import lt.vilniustech.meal_planner.dto.QuestionnaireDtos.SaveResponse;
import lt.vilniustech.meal_planner.entity.Client;
import lt.vilniustech.meal_planner.entity.Questionnaire;
import lt.vilniustech.meal_planner.exception.ApiException;
import lt.vilniustech.meal_planner.repository.QuestionnaireRepository;
import lt.vilniustech.meal_planner.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class QuestionnaireService implements IQuestionnaire {

    private final QuestionnaireRepository questionnaires;
    private final UserRepository users;
    private final IProductCatalog catalog;
    private final ICalorieCalculator calculator;
    private final Clock clock;

    public QuestionnaireService(QuestionnaireRepository questionnaires, UserRepository users,
                                IProductCatalog catalog, ICalorieCalculator calculator, Clock clock) {
        this.questionnaires = questionnaires;
        this.users = users;
        this.catalog = catalog;
        this.calculator = calculator;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<QuestionnaireResponse> get(Long clientId) {
        return questionnaires.findByClientId(clientId).map(this::toResponse);
    }

    /**
     * Patikrina ir išsaugo anketą, apskaičiuoja KMI ir paros poreikį (FR-01–FR-07).
     * Laukų ribos patikrinamos valdiklyje (@Valid), čia – ryšiai su katalogu.
     */
    public SaveResponse save(Long clientId, QuestionnaireRequest request) {
        Client client = users.findById(clientId).map(Client.class::cast)
                .orElseThrow(() -> ApiException.notFound("Klientas nerastas."));
        List<Long> allergenIds = request.allergenIds() == null ? List.of() : request.allergenIds();
        List<Long> productIds = request.dislikedProductIds() == null ? List.of() : request.dislikedProductIds();
        var allergens = catalog.allergensByIds(allergenIds);
        var products = catalog.productsByIds(productIds);
        if (allergens.size() != new HashSet<>(allergenIds).size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Pasirinktas nežinomas alergenas.",
                    "allergenIds");
        }
        if (products.size() != new HashSet<>(productIds).size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Pasirinktas nežinomas produktas.",
                    "dislikedProductIds");
        }

        Optional<Questionnaire> existing = questionnaires.findByClientId(clientId);
        Questionnaire q = existing.orElseGet(() -> new Questionnaire(client));
        q.update(request.heightCm(), request.weightKg(), request.age(), request.gender(), request.activityLevel(),
                request.goal(), request.chronicDisease());
        q.setRestrictions(new HashSet<>(allergens), new HashSet<>(products));

        BigDecimal bmi = calculator.bmi(q.getHeightCm(), q.getWeightKg());
        CalorieResult calories = calculate(q);
        q.setCalculated(bmi, calories.dailyCalories(), clock.instant());
        questionnaires.save(q);

        return new SaveResponse(toResponse(q), existing.isEmpty(), q.isChronicDisease(), calories.safeLimitApplied());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Integer> dailyCalories(Long clientId) {
        return questionnaires.findByClientId(clientId).map(Questionnaire::getDailyCalories);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuestionnaireResponse> anonymized(Long clientId) {
        // Atsakyme nėra kliento vardo ir el. pašto
        return get(clientId);
    }

    private CalorieResult calculate(Questionnaire q) {
        return calculator.dailyCalories(q.getHeightCm(), q.getWeightKg(), q.getAge(), q.getGender(),
                q.getActivityLevel(), q.getGoal());
    }

    private QuestionnaireResponse toResponse(Questionnaire q) {
        CalorieResult c = calculate(q);
        CalorieExplanation explanation = new CalorieExplanation(c.bmr(), c.activityFactor(),
                (int) Math.round(c.afterActivity()), c.goalFactor(), c.calculatedCalories(), c.safeMinimum(),
                c.safeLimitApplied());
        Metrics metrics = new Metrics(q.getBmi(), calculator.bmiCategory(q.getBmi()), q.getDailyCalories(),
                explanation);
        return new QuestionnaireResponse(q.getHeightCm(), q.getWeightKg(), q.getAge(), q.getGender(),
                q.getActivityLevel(), q.getGoal(), q.isChronicDisease(),
                q.getAllergies().stream().map(AllergenDto::of).sorted(Comparator.comparing(AllergenDto::id)).toList(),
                q.getDislikedProducts().stream().map(ProductDto::of).sorted(Comparator.comparing(ProductDto::name))
                        .toList(),
                q.getUpdatedAt(), metrics);
    }
}
