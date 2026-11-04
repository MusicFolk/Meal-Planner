package lt.vilniustech.meal_planner.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lt.vilniustech.meal_planner.ai.AiMealResponse.AiDish;
import lt.vilniustech.meal_planner.ai.AiMealResponse.AiIngredient;
import lt.vilniustech.meal_planner.calories.CalorieCalculator;
import lt.vilniustech.meal_planner.entity.MealPlan;
import lt.vilniustech.meal_planner.entity.NutritionValue;
import lt.vilniustech.meal_planner.entity.Product;
import lt.vilniustech.meal_planner.enums.AiRequestResult;
import lt.vilniustech.meal_planner.enums.MealType;
import lt.vilniustech.meal_planner.service.IQuestionnaire.GenerationInput;
import lt.vilniustech.meal_planner.service.PlanGenerationService.DishCheck;
import lt.vilniustech.meal_planner.service.PlanGenerationService.ScaledDish;
import lt.vilniustech.meal_planner.service.PlanGenerationService.ScaledIngredient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

/** Plano patikros ir porcijų koregavimo vienetų testai (FR-12, FR-13, NFR-47). */
class PlanGenerationServiceTest {

    private static final Product OATS = product(1L, 400);
    private static final Product MILK = product(2L, 50);

    private final CalorieCalculator calculator = new CalorieCalculator();
    private final PlanGenerationService service = new PlanGenerationService(null, null, null, null, null, null,
            calculator, new PlanCalculator(calculator), null, null, Clock.systemUTC(), 5, Duration.ofSeconds(25),
            Duration.ofSeconds(60), 3, ZoneId.of("Europe/Vilnius"));

    private final Map<Long, Product> allowed = Map.of(1L, OATS, 2L, MILK);

    @Test
    void portionsAreScaledToMealTarget() {
        // 100 g dribsnių (400 kcal) + 200 g pieno (100 kcal) = 500 kcal; tikslas 600 → koeficientas 1,2
        DishCheck check = service.checkDishes(List.of(dish(100, 200)), 600, allowed);

        assertThat(check.result()).isEqualTo(AiRequestResult.VALID);
        assertThat(check.dishes().getFirst().ingredients()).extracting(ScaledIngredient::grams)
                .containsExactly(120, 240);
    }

    @Test
    void factorOutsideRangeOrUnknownProductRejectsMealType() {
        assertThat(service.checkDishes(List.of(dish(100, 200)), 700, allowed).result())
                .isEqualTo(AiRequestResult.PORTION_OUT_OF_RANGE);
        assertThat(service.checkDishes(List.of(dish(100, 200)), 340, allowed).result())
                .isEqualTo(AiRequestResult.PORTION_OUT_OF_RANGE);
        AiDish unknown = new AiDish("Kita", List.of(new AiIngredient(3, 100)), List.of("Virkite."));
        assertThat(service.checkDishes(List.of(unknown), 500, allowed).result())
                .isEqualTo(AiRequestResult.INVALID_PRODUCT);
    }

    @Test
    void planWithinTenPercentPassesCheck() {
        MealPlan plan = plan(2000, 500, 800, 700);

        assertThat(plan.getDays()).hasSize(7);
        assertThat(plan.getDays().getFirst().getDeviationPercent()).isEqualByComparingTo("0.0");
        assertThat(service.checkPlan(plan, new GenerationInput(2000, List.of(OATS, MILK), Set.of()))).isNull();
    }

    @Test
    void planOutsideTenPercentOrWithForbiddenProductFails() {
        MealPlan tooBig = plan(2000, 600, 900, 752);
        assertThat(service.checkPlan(tooBig, new GenerationInput(2000, List.of(OATS, MILK), Set.of())))
                .startsWith("Dienos 1 nuokrypis 12.6");

        MealPlan ok = plan(2000, 500, 800, 700);
        assertThat(service.checkPlan(ok, new GenerationInput(2000, List.of(MILK), Set.of(1L))))
                .contains("draudžiamas produktas");
    }

    /** Planas, kurio kiekvieno valgymo patiekalas – tik dribsniai su nurodytu kaloringumu (kcal dalus iš 4). */
    private MealPlan plan(int daily, int breakfast, int lunch, int dinner) {
        Map<MealType, List<ScaledDish>> dishes = new EnumMap<>(MealType.class);
        dishes.put(MealType.BREAKFAST, sevenDishes(breakfast));
        dishes.put(MealType.LUNCH, sevenDishes(lunch));
        dishes.put(MealType.DINNER, sevenDishes(dinner));
        MealPlan plan = PlanGenerationService.buildPlan(daily, calculator.mealTargets(daily), dishes);
        new PlanCalculator(calculator).calculate(plan);
        return plan;
    }

    private static List<ScaledDish> sevenDishes(int kcal) {
        return java.util.stream.IntStream.range(0, 7)
                .mapToObj(i -> new ScaledDish("Patiekalas " + i,
                        List.of(new ScaledIngredient(OATS, kcal / 4)),
                        List.of("Virkite.")))
                .toList();
    }

    private static AiDish dish(int oats, int milk) {
        return new AiDish("Košė", List.of(new AiIngredient(1, oats), new AiIngredient(2, milk)), List.of("Virkite."));
    }

    private static Product product(Long id, int kcal) {
        Product product = BeanUtils.instantiateClass(Product.class);
        ReflectionTestUtils.setField(product, "id", id);
        ReflectionTestUtils.setField(product, "nutritionPer100g",
                new NutritionValue(kcal, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));
        return product;
    }
}
