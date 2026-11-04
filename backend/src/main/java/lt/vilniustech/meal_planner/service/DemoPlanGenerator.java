package lt.vilniustech.meal_planner.service;

import java.time.Instant;
import lt.vilniustech.meal_planner.calories.ICalorieCalculator;
import lt.vilniustech.meal_planner.calories.MealTargets;
import lt.vilniustech.meal_planner.entity.Client;
import lt.vilniustech.meal_planner.entity.Dish;
import lt.vilniustech.meal_planner.entity.Ingredient;
import lt.vilniustech.meal_planner.entity.Meal;
import lt.vilniustech.meal_planner.entity.MealPlan;
import lt.vilniustech.meal_planner.entity.PlanDay;
import lt.vilniustech.meal_planner.exception.ApiException;
import lt.vilniustech.meal_planner.repository.MealPlanRepository;
import org.springframework.stereotype.Component;

/**
 * 1 sprinto plano šaltinis: planas sudaromas iš demonstracinio šablono DB (PLN-1), o porcijos
 * proporcingai pritaikomos kliento paros poreikiui. 2 sprinte šį komponentą pakeis DI generavimas.
 */
@Component
public class DemoPlanGenerator {

    private final MealPlanRepository plans;
    private final ICalorieCalculator calculator;
    private final PlanCalculator planCalculator;

    public DemoPlanGenerator(MealPlanRepository plans, ICalorieCalculator calculator, PlanCalculator planCalculator) {
        this.plans = plans;
        this.calculator = calculator;
        this.planCalculator = planCalculator;
    }

    public MealPlan generate(Client client, int dailyCalories, Instant now) {
        MealPlan template = plans.findFirstByClientIsNullOrderByIdAsc()
                .orElseThrow(() -> ApiException.badRequest("Plano sugeneruoti nepavyko. Ankstesnis planas nepakeistas."));
        MealTargets targets = calculator.mealTargets(dailyCalories);
        MealPlan plan = new MealPlan(client, now, dailyCalories, false);
        for (PlanDay sourceDay : template.getDays()) {
            PlanDay day = plan.addDay(sourceDay.getDayNumber());
            for (Meal sourceMeal : sourceDay.getMealsInOrder()) {
                int target = switch (sourceMeal.getType()) {
                    case BREAKFAST -> targets.breakfast();
                    case LUNCH -> targets.lunch();
                    case DINNER -> targets.dinner();
                };
                Dish source = sourceMeal.getDish();
                double factor = calculator.portionFactor(target, source.getNutrition().getKcal());
                Dish dish = day.addMeal(sourceMeal.getType(), target).setDish(source.getName());
                for (Ingredient ingredient : source.getRecipe().getIngredients()) {
                    dish.getRecipe().addIngredient(ingredient.getProduct(),
                            Math.max(1, (int) Math.round(ingredient.getGrams() * factor)));
                }
                source.getRecipe().getSteps().forEach(step -> dish.getRecipe().addStep(step.getDescription()));
            }
        }
        planCalculator.calculate(plan);
        return plan;
    }
}
