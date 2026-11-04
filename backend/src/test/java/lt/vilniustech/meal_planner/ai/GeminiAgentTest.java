package lt.vilniustech.meal_planner.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import lt.vilniustech.meal_planner.ai.AiMealRequest.AllowedProduct;
import lt.vilniustech.meal_planner.enums.AiRequestResult;
import lt.vilniustech.meal_planner.enums.MealType;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** DI adapterio vienetų testai su imituotu Gemini atsakymu (DIA-1–DIA-3). */
class GeminiAgentTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final AiMealRequest REQUEST = new AiMealRequest(MealType.BREAKFAST, 500, 2,
            List.of(new AllowedProduct(1, "Avižiniai dribsniai", 379), new AllowedProduct(2, "Pienas", 50)));

    private static final String VALID = """
            {"dishes": [
              {"name": "Avižinė košė", "ingredients": [{"productId": 1, "grams": 80}, {"productId": 2, "grams": 200}],
               "steps": ["Užvirinkite pieną.", "Suberkite dribsnius."]},
              {"name": "Dribsniai su pienu", "ingredients": [{"productId": 1, "grams": 90}],
               "steps": ["Užpilkite pienu."]}
            ]}""";

    @Test
    void requestContainsOnlyMealTypeTargetAndProducts() {
        String json = agent(prompt -> VALID).requestJson(REQUEST);

        assertThat(json).isEqualTo("{\"mealType\":\"pusryčiai\",\"targetCaloriesPerDish\":500,\"dishCount\":2,"
                + "\"products\":[{\"id\":1,\"name\":\"Avižiniai dribsniai\",\"kcalPer100g\":379},"
                + "{\"id\":2,\"name\":\"Pienas\",\"kcalPer100g\":50}]}");
    }

    @Test
    void validResponseIsParsed() {
        AiMealResponse response = agent(prompt -> VALID).generateMeals(REQUEST, Duration.ofSeconds(5));

        assertThat(response.result()).isEqualTo(AiRequestResult.VALID);
        assertThat(response.dishes()).hasSize(2);
        assertThat(response.dishes().getFirst().ingredients()).extracting(AiMealResponse.AiIngredient::grams)
                .containsExactly(80, 200);
        assertThat(response.responseJson()).contains("Avižinė košė");
    }

    @Test
    void codeFenceIsIgnored() {
        AiMealResponse response = agent(prompt -> "```json\n" + VALID + "\n```")
                .generateMeals(REQUEST, Duration.ofSeconds(5));

        assertThat(response.result()).isEqualTo(AiRequestResult.VALID);
    }

    @Test
    void wrongDishCountIsInvalidSchema() {
        String oneDish = """
                {"dishes": [{"name": "Košė", "ingredients": [{"productId": 1, "grams": 80}], "steps": ["Virkite."]}]}""";

        AiMealResponse response = agent(prompt -> oneDish).generateMeals(REQUEST, Duration.ofSeconds(5));

        assertThat(response.result()).isEqualTo(AiRequestResult.INVALID_SCHEMA);
    }

    @Test
    void missingStepsAndNonJsonAreInvalidSchema() {
        String noSteps = VALID.replace("\"steps\": [\"Užpilkite pienu.\"]", "\"steps\": []");

        assertThat(agent(prompt -> noSteps).generateMeals(REQUEST, Duration.ofSeconds(5)).result())
                .isEqualTo(AiRequestResult.INVALID_SCHEMA);
        AiMealResponse notJson = agent(prompt -> "Atsiprašau, negaliu").generateMeals(REQUEST, Duration.ofSeconds(5));
        assertThat(notJson.result()).isEqualTo(AiRequestResult.INVALID_SCHEMA);
        // Netinkamas tekstas saugomas kaip galiojantis JSON (JSONB stulpelis)
        assertThat(notJson.responseJson()).isEqualTo("{\"raw\":\"Atsiprašau, negaliu\"}");
    }

    @Test
    void duplicateDishNamesAreInvalidSchema() {
        String duplicate = VALID.replace("Dribsniai su pienu", "avižinė košė");

        assertThat(agent(prompt -> duplicate).generateMeals(REQUEST, Duration.ofSeconds(5)).result())
                .isEqualTo(AiRequestResult.INVALID_SCHEMA);
    }

    @Test
    void slowResponseIsTimeout() {
        AiMealResponse response = agent(prompt -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return VALID;
        }).generateMeals(REQUEST, Duration.ofMillis(100));

        assertThat(response.result()).isEqualTo(AiRequestResult.TIMEOUT);
        assertThat(response.responseJson()).isNull();
    }

    @Test
    void apiExceptionAndMissingKeyAreApiError() {
        AiMealResponse failed = agent(prompt -> {
            throw new IllegalStateException("429 RESOURCE_EXHAUSTED");
        }).generateMeals(REQUEST, Duration.ofSeconds(5));
        assertThat(failed.result()).isEqualTo(AiRequestResult.API_ERROR);
        assertThat(failed.responseJson()).contains("RESOURCE_EXHAUSTED");

        AiMealResponse noKey = new GeminiAgent(null, "gemini-test", ChatOptions.builder().build(), "", MAPPER)
                .generateMeals(REQUEST, Duration.ofSeconds(5));
        assertThat(noKey.result()).isEqualTo(AiRequestResult.API_ERROR);
        assertThat(noKey.error()).isEqualTo(GeminiAgent.NO_API_KEY);
    }

    private static GeminiAgent agent(Function<Prompt, String> answer) {
        ChatModel model = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of(new Generation(new AssistantMessage(answer.apply(prompt)))));
            }
        };
        return new GeminiAgent(model, "gemini-test", ChatOptions.builder().build(), "Sistemos šablonas", MAPPER);
    }
}
