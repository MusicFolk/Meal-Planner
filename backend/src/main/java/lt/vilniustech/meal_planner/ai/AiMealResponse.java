package lt.vilniustech.meal_planner.ai;

import java.util.List;
import lt.vilniustech.meal_planner.enums.AiRequestResult;

/**
 * DI adapterio atsakymas: patikros rezultatas (VALID, INVALID_SCHEMA, TIMEOUT arba API_ERROR), DI siųsta
 * užklausa ir gautas atsakymas JSON formatu (NFR-05), patiekalai ir kvietimo trukmė.
 */
public record AiMealResponse(AiRequestResult result, String requestJson, String responseJson, List<AiDish> dishes,
                             int durationMs, String error) {

    public boolean valid() {
        return result == AiRequestResult.VALID;
    }

    /** DI pasiūlytas patiekalas: pavadinimas, ingredientai (produkto ID ir gramai) ir gaminimo žingsniai. */
    public record AiDish(String name, List<AiIngredient> ingredients, List<String> steps) {
    }

    public record AiIngredient(long productId, int grams) {
    }
}
