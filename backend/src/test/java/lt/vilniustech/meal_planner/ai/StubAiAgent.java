package lt.vilniustech.meal_planner.ai;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import lt.vilniustech.meal_planner.ai.AiMealRequest.AllowedProduct;
import lt.vilniustech.meal_planner.ai.AiMealResponse.AiDish;
import lt.vilniustech.meal_planner.ai.AiMealResponse.AiIngredient;
import lt.vilniustech.meal_planner.enums.AiRequestResult;

/**
 * Netikras DI agentas testams: iš leistinų produktų sudaro patiekalus, kurių kaloringumas lygus tikslui.
 * Elgseną galima pakeisti, pvz., grąžinti nežinomą produktą arba laiko ribos viršijimą.
 */
public class StubAiAgent implements IAIAgent {

    private volatile BiFunction<AiMealRequest, Integer, AiMealResponse> behaviour = (request, call) -> valid(request);
    private final AtomicInteger calls = new AtomicInteger();

    public void reset() {
        behaviour = (request, call) -> valid(request);
        calls.set(0);
    }

    /** @param behaviour funkcija (užklausa, kvietimo numeris nuo 1) → atsakymas */
    public void answer(BiFunction<AiMealRequest, Integer, AiMealResponse> behaviour) {
        this.behaviour = behaviour;
    }

    public int calls() {
        return calls.get();
    }

    @Override
    public String modelName() {
        return "stub-model";
    }

    @Override
    public AiMealResponse generateMeals(AiMealRequest request, Duration timeout) {
        return behaviour.apply(request, calls.incrementAndGet());
    }

    /** Tinkamas atsakymas: kiekvienas patiekalas – du skirtingi produktai po pusę kalorijų tikslo. */
    public static AiMealResponse valid(AiMealRequest request) {
        List<AllowedProduct> caloric = request.products().stream().filter(p -> p.kcalPer100g() >= 50).toList();
        List<AiDish> dishes = new ArrayList<>();
        for (int i = 0; i < request.dishCount(); i++) {
            AllowedProduct a = caloric.get((2 * i) % caloric.size());
            AllowedProduct b = caloric.get((2 * i + 1) % caloric.size());
            dishes.add(new AiDish(request.mealType() + " patiekalas " + (i + 1), List.of(
                    new AiIngredient(a.id(), grams(request.targetCalories() / 2, a)),
                    new AiIngredient(b.id(), grams(request.targetCalories() / 2, b))),
                    List.of("Paruoškite produktus.", "Patiekite.")));
        }
        return new AiMealResponse(AiRequestResult.VALID, "{\"stub\":true}", "{\"dishes\":[]}", dishes, 5, null);
    }

    public static AiMealResponse withUnknownProduct(AiMealRequest request) {
        AiMealResponse ok = valid(request);
        List<AiDish> dishes = new ArrayList<>(ok.dishes());
        dishes.set(0, new AiDish("Nežinomas", List.of(new AiIngredient(999_999, 100)), List.of("Virkite.")));
        return new AiMealResponse(AiRequestResult.VALID, ok.requestJson(), ok.responseJson(), dishes, 5, null);
    }

    public static AiMealResponse timeout() {
        return new AiMealResponse(AiRequestResult.TIMEOUT, "{\"stub\":true}", null, List.of(), 25_000, "laikas");
    }

    private static int grams(int kcal, AllowedProduct product) {
        return Math.max(1, Math.round(kcal * 100f / product.kcalPer100g()));
    }
}
