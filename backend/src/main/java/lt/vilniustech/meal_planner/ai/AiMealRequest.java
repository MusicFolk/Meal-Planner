package lt.vilniustech.meal_planner.ai;

import java.util.List;
import lt.vilniustech.meal_planner.enums.MealType;

/**
 * Vieno valgymo tipo užklausa DI. Joje tik valgymo tipas, vieno patiekalo kalorijų tikslas ir
 * leistinų produktų sąrašas – kliento asmens ir anketos duomenų nėra (FR-10, NFR-55).
 */
public record AiMealRequest(MealType mealType, int targetCalories, int dishCount, List<AllowedProduct> products) {

    /** Leistinas produktas: ID, pavadinimas ir kaloringumas 100 g. */
    public record AllowedProduct(long id, String name, int kcalPer100g) {
    }
}
