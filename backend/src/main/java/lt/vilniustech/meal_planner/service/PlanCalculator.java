package lt.vilniustech.meal_planner.service;

import java.util.LinkedHashMap;
import java.util.Map;
import lt.vilniustech.meal_planner.entity.Dish;
import lt.vilniustech.meal_planner.entity.Ingredient;
import lt.vilniustech.meal_planner.entity.Meal;
import lt.vilniustech.meal_planner.entity.MealPlan;
import lt.vilniustech.meal_planner.entity.NutritionValue;
import lt.vilniustech.meal_planner.entity.PlanDay;
import lt.vilniustech.meal_planner.entity.Product;
import lt.vilniustech.meal_planner.entity.ShoppingList;
import org.springframework.stereotype.Component;

/**
 * Apskaičiuoja patiekalų ir dienų maistinę vertę tik pagal produktų DB (NFR-22) ir sudaro
 * produktų sąrašą, sumuodamas tų pačių produktų kiekius visuose plano receptuose (FR-19, NFR-24).
 */
@Component
public class PlanCalculator {

    public void calculate(MealPlan plan) {
        Map<Product, Integer> totals = new LinkedHashMap<>();
        for (PlanDay day : plan.getDays()) {
            int dayCalories = 0;
            for (Meal meal : day.getMeals()) {
                Dish dish = meal.getDish();
                NutritionValue.Accumulator sum = NutritionValue.Accumulator.empty();
                int grams = 0;
                for (Ingredient ingredient : dish.getRecipe().getIngredients()) {
                    sum = sum.plus(ingredient.getProduct().getNutritionPer100g().forGrams(ingredient.getGrams()));
                    grams += ingredient.getGrams();
                    totals.merge(ingredient.getProduct(), ingredient.getGrams(), Integer::sum);
                }
                dish.setCalculated(grams, sum.toNutritionValue());
                dayCalories += dish.getNutrition().getKcal();
            }
            day.setTotalCalories(dayCalories);
        }
        ShoppingList list = new ShoppingList(plan);
        totals.forEach(list::addItem);
        plan.setShoppingList(list);
    }
}
