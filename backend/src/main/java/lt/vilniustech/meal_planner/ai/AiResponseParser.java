package lt.vilniustech.meal_planner.ai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lt.vilniustech.meal_planner.ai.AiMealResponse.AiDish;
import lt.vilniustech.meal_planner.ai.AiMealResponse.AiIngredient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Patikrina, ar DI atsakymas atitinka JSON schemą (DIA-3, FR-11): nurodytas patiekalų skaičius, kiekvienas
 * patiekalas turi pavadinimą, bent vieną ingredientą (produkto ID ir kiekis gramais) ir bent vieną
 * gaminimo žingsnį. Ribos sutampa su DB stulpelių ilgiais.
 *
 * <p>DI laukai {@code kcal} ir {@code totalKcal} skirti tik tam, kad modelis pats pasitikrintų porcijas;
 * jie nenuskaitomi – maistinė vertė skaičiuojama tik pagal produktų DB (FR-12, NFR-22).
 */
public class AiResponseParser {

    static final int MAX_NAME_LENGTH = 150;
    static final int MAX_STEP_LENGTH = 1000;
    static final int MAX_INGREDIENTS = 20;
    static final int MAX_STEPS = 20;
    static final int MAX_GRAMS = 2000;

    private final ObjectMapper mapper;

    public AiResponseParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /** Patikros rezultatas: patiekalai arba klaidos priežastis. */
    public record Parsed(List<AiDish> dishes, String error) {

        static Parsed invalid(String error) {
            return new Parsed(List.of(), error);
        }

        public boolean ok() {
            return error == null;
        }
    }

    public Parsed parse(String json, int dishCount) {
        JsonNode root;
        try {
            root = mapper.readTree(stripCodeFence(json));
        } catch (JacksonException e) {
            return Parsed.invalid("Atsakymas nėra JSON");
        }
        JsonNode dishesNode = root.get("dishes");
        if (dishesNode == null || !dishesNode.isArray()) {
            return Parsed.invalid("Nėra masyvo „dishes“");
        }
        if (dishesNode.size() != dishCount) {
            return Parsed.invalid("Patiekalų skaičius " + dishesNode.size() + ", reikia " + dishCount);
        }
        List<AiDish> dishes = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (JsonNode dishNode : dishesNode) {
            String name = text(dishNode.get("name"));
            if (name == null || name.length() > MAX_NAME_LENGTH) {
                return Parsed.invalid("Netinkamas patiekalo pavadinimas");
            }
            if (!names.add(name.toLowerCase(Locale.ROOT))) {
                return Parsed.invalid("Patiekalai kartojasi: " + name);
            }
            List<AiIngredient> ingredients = ingredients(dishNode.get("ingredients"));
            if (ingredients == null) {
                return Parsed.invalid("Netinkami patiekalo „" + name + "“ ingredientai");
            }
            List<String> steps = steps(dishNode.get("steps"));
            if (steps == null) {
                return Parsed.invalid("Netinkami patiekalo „" + name + "“ gaminimo žingsniai");
            }
            dishes.add(new AiDish(name, ingredients, steps));
        }
        return new Parsed(dishes, null);
    }

    private static List<AiIngredient> ingredients(JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty() || node.size() > MAX_INGREDIENTS) {
            return null;
        }
        List<AiIngredient> result = new ArrayList<>();
        for (JsonNode item : node) {
            JsonNode productId = item.get("productId");
            JsonNode grams = item.get("grams");
            if (productId == null || !productId.isIntegralNumber() || productId.asLong() <= 0
                    || grams == null || !grams.isIntegralNumber() || grams.asInt() < 1 || grams.asInt() > MAX_GRAMS) {
                return null;
            }
            result.add(new AiIngredient(productId.asLong(), grams.asInt()));
        }
        return result;
    }

    private static List<String> steps(JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty() || node.size() > MAX_STEPS) {
            return null;
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            String step = text(item);
            if (step == null || step.length() > MAX_STEP_LENGTH) {
                return null;
            }
            result.add(step);
        }
        return result;
    }

    private static String text(JsonNode node) {
        if (node == null || !node.isString()) {
            return null;
        }
        String value = node.asString().strip();
        return value.isEmpty() ? null : value;
    }

    /** Kai kurie modeliai JSON vis tiek apgaubia ```json ... ``` žymomis. */
    static String stripCodeFence(String text) {
        String trimmed = text.strip();
        if (trimmed.startsWith("```")) {
            int firstLine = trimmed.indexOf('\n');
            int end = trimmed.lastIndexOf("```");
            if (firstLine > 0 && end > firstLine) {
                return trimmed.substring(firstLine + 1, end).strip();
            }
        }
        return trimmed;
    }
}
