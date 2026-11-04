package lt.vilniustech.meal_planner.mapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.DishSummaryDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.IngredientDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.MealDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.MealPlanDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.NutritionDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.PlanDayDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.RecipeDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.ShoppingCategoryDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.ShoppingItemDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.ShoppingListDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.StepDto;
import lt.vilniustech.meal_planner.entity.Dish;
import lt.vilniustech.meal_planner.entity.Meal;
import lt.vilniustech.meal_planner.entity.MealPlan;
import lt.vilniustech.meal_planner.entity.PlanDay;
import lt.vilniustech.meal_planner.entity.Recipe;
import lt.vilniustech.meal_planner.entity.ShoppingList;
import lt.vilniustech.meal_planner.entity.ShoppingListItem;
import lt.vilniustech.meal_planner.enums.ProductCategory;

public final class MealPlanMapper {

    private MealPlanMapper() {
    }

    public static MealPlanDto toDto(MealPlan plan) {
        List<PlanDayDto> days = plan.getDays().stream()
                .map(day -> new PlanDayDto(day.getDayNumber(), day.getTotalCalories(), day.getDeviationPercent(),
                        day.getMealsInOrder().stream().map(MealPlanMapper::toDto).toList()))
                .toList();
        return new MealPlanDto(plan.getId(), plan.getCreatedAt(), plan.isActive(), plan.isAiGenerated(),
                plan.getDailyCalories(), days);
    }

    private static MealDto toDto(Meal meal) {
        Dish dish = meal.getDish();
        return new MealDto(meal.getType(), meal.getTargetCalories(),
                new DishSummaryDto(dish.getId(), dish.getName(), dish.getPortionGrams(),
                        NutritionDto.of(dish.getNutrition())));
    }

    public static RecipeDto toRecipe(Dish dish) {
        Meal meal = dish.getMeal();
        PlanDay day = meal.getPlanDay();
        Recipe recipe = dish.getRecipe();
        return new RecipeDto(day.getMealPlan().getId(), dish.getId(), dish.getName(), meal.getType(),
                day.getDayNumber(), dish.getPortionGrams(), NutritionDto.of(dish.getNutrition()),
                recipe.getIngredients().stream()
                        .map(i -> new IngredientDto(i.getProduct().getId(), i.getProduct().getName(), i.getGrams()))
                        .toList(),
                recipe.getSteps().stream().map(s -> new StepDto(s.getStepNumber(), s.getDescription())).toList());
    }

    /** Produktų sąrašas, sugrupuotas pagal produktų kategorijas (FR-19, UI-03). */
    public static ShoppingListDto toDto(ShoppingList list) {
        Map<ProductCategory, List<ShoppingItemDto>> byCategory = new TreeMap<>();
        int purchased = 0;
        for (ShoppingListItem item : list.getItems()) {
            byCategory.computeIfAbsent(item.getProduct().getCategory(), c -> new ArrayList<>())
                    .add(new ShoppingItemDto(item.getId(), item.getProduct().getId(), item.getProduct().getName(),
                            item.getTotalGrams(), item.isPurchased()));
            if (item.isPurchased()) {
                purchased++;
            }
        }
        List<ShoppingCategoryDto> categories = byCategory.entrySet().stream()
                .map(e -> new ShoppingCategoryDto(e.getKey(),
                        e.getValue().stream().sorted(Comparator.comparing(ShoppingItemDto::name)).toList()))
                .toList();
        return new ShoppingListDto(list.getMealPlan().getId(), list.getItems().size(), purchased, categories);
    }
}
