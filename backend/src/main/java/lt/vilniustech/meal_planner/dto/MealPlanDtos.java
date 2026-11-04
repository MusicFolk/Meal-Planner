package lt.vilniustech.meal_planner.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lt.vilniustech.meal_planner.entity.NutritionValue;
import lt.vilniustech.meal_planner.enums.AiRequestResult;
import lt.vilniustech.meal_planner.enums.GenerationStatus;
import lt.vilniustech.meal_planner.enums.MealType;
import lt.vilniustech.meal_planner.enums.ProductCategory;
import lt.vilniustech.meal_planner.enums.SubscriptionStatus;

public final class MealPlanDtos {

    private MealPlanDtos() {
    }

    public record NutritionDto(int kcal, BigDecimal proteinG, BigDecimal fatG, BigDecimal carbsG) {

        public static NutritionDto of(NutritionValue n) {
            return new NutritionDto(n.getKcal(), n.getProteinG(), n.getFatG(), n.getCarbsG());
        }
    }

    public record DishSummaryDto(Long id, String name, int portionGrams, NutritionDto nutrition) {
    }

    public record MealDto(MealType type, Integer targetCalories, DishSummaryDto dish) {
    }

    public record PlanDayDto(int dayNumber, int totalCalories, BigDecimal deviationPercent, List<MealDto> meals) {
    }

    public record MealPlanDto(Long id, Instant createdAt, boolean active, boolean aiGenerated, Integer dailyCalories,
                              List<PlanDayDto> days) {
    }

    public record IngredientDto(Long productId, String name, int grams) {
    }

    public record StepDto(int number, String description) {
    }

    public record RecipeDto(Long planId, Long dishId, String name, MealType mealType, int dayNumber,
                            int portionGrams, NutritionDto nutrition, List<IngredientDto> ingredients,
                            List<StepDto> steps) {
    }

    public record ShoppingItemDto(Long id, Long productId, String name, int totalGrams, boolean purchased) {
    }

    public record ShoppingCategoryDto(ProductCategory category, List<ShoppingItemDto> items) {
    }

    public record ShoppingListDto(Long planId, int totalItems, int purchasedItems,
                                  List<ShoppingCategoryDto> categories) {
    }

    public record ExportedPlanDto(MealPlanDto plan, List<RecipeDto> recipes, ShoppingListDto shoppingList) {
    }

    public record PurchasedRequest(boolean purchased) {
    }

    /** Ar klientas gali gauti naują planą (FR-08) ir, jei ne, kodėl. */
    public record GenerationStatusDto(boolean canGenerate, String reason, boolean questionnaireFilled,
                                      SubscriptionStatus subscriptionStatus, int remainingToday, int dailyLimit,
                                      boolean inProgress) {
    }

    /** Generavimo eiga pagal valgymo tipus (FR-09). */
    public record GenerationProgressDto(Long id, GenerationStatus status, Instant startedAt,
                                        List<MealProgressDto> meals) {
    }

    /**
     * @param attempts   atliktų DI bandymų skaičius
     * @param lastResult paskutinio bandymo patikros rezultatas
     * @param done       ar valgymo tipo patiekalai jau gauti
     */
    public record MealProgressDto(MealType type, int attempts, AiRequestResult lastResult, boolean done) {
    }
}
