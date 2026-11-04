package lt.vilniustech.meal_planner.ai;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lt.vilniustech.meal_planner.enums.AiRequestResult;
import lt.vilniustech.meal_planner.enums.MealType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * DI adapteris (DIA) «DI agentas»: kreipiasi į Google Gemini API per Spring AI (DIA-2). Užklausa siunčiama
 * JSON formatu kartu su atsakymo JSON schema (NFR-07); šablonas ir schema laikomi atskiruose failuose
 * (NFR-45). Modulis nepriklauso nuo kitų sistemos komponentų (NFR-48).
 */
public class GeminiAgent implements IAIAgent {

    private static final Logger log = LoggerFactory.getLogger(GeminiAgent.class);

    /** Su API raktu nesukonfigūruotas klientas – kitos sistemos funkcijos vis tiek veikia (NFR-29). */
    static final String NO_API_KEY = "Nenurodytas GEMINI_API_KEY";

    private final ChatModel chatModel;
    private final String model;
    private final ChatOptions options;
    private final String systemPrompt;
    private final ObjectMapper mapper;
    private final AiResponseParser parser;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * @param chatModel Spring AI pokalbių modelis arba null, jei API raktas nenurodytas
     * @param options   modelio parinktys: modelis, atsakymo JSON schema, mąstymo lygis
     */
    public GeminiAgent(ChatModel chatModel, String model, ChatOptions options, String systemPrompt,
                       ObjectMapper mapper) {
        this.chatModel = chatModel;
        this.model = model;
        this.options = options;
        this.systemPrompt = systemPrompt;
        this.mapper = mapper;
        this.parser = new AiResponseParser(mapper);
    }

    @Override
    public String modelName() {
        return model;
    }

    @Override
    public AiMealResponse generateMeals(AiMealRequest request, Duration timeout) {
        String requestJson = requestJson(request);
        if (chatModel == null) {
            return failure(AiRequestResult.API_ERROR, requestJson, NO_API_KEY, 0);
        }
        Prompt prompt = new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(requestJson)), options);
        long started = System.nanoTime();
        Future<String> call = executor.submit(() -> chatModel.call(prompt).getResult().getOutput().getText());
        String text;
        try {
            text = call.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            call.cancel(true);
            return new AiMealResponse(AiRequestResult.TIMEOUT, requestJson, null, List.of(), elapsedMs(started),
                    "DI neatsakė per " + timeout.toSeconds() + " s");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            log.warn("Gemini API klaida ({}): {}", request.mealType(), cause.toString());
            return failure(AiRequestResult.API_ERROR, requestJson, cause.getClass().getSimpleName() + ": "
                    + cause.getMessage(), elapsedMs(started));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            call.cancel(true);
            return failure(AiRequestResult.API_ERROR, requestJson, "Kvietimas nutrauktas", elapsedMs(started));
        }
        int duration = elapsedMs(started);
        if (text == null || text.isBlank()) {
            return new AiMealResponse(AiRequestResult.INVALID_SCHEMA, requestJson, null, List.of(), duration,
                    "Tuščias atsakymas");
        }
        AiResponseParser.Parsed parsed = parser.parse(text, request.dishCount());
        AiRequestResult result = parsed.ok() ? AiRequestResult.VALID : AiRequestResult.INVALID_SCHEMA;
        return new AiMealResponse(result, requestJson, responseJson(text), parsed.dishes(), duration, parsed.error());
    }

    /** Užklausa DI: tik valgymo tipas, patiekalo kalorijų tikslas ir leistini produktai (NFR-55). */
    String requestJson(AiMealRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mealType", mealTypeName(request.mealType()));
        body.put("targetCaloriesPerDish", request.targetCalories());
        body.put("dishCount", request.dishCount());
        body.put("products", request.products());
        return mapper.writeValueAsString(body);
    }

    /** JSONB stulpelyje saugomas tik galiojantis JSON, todėl netinkamas tekstas įvyniojamas. */
    private String responseJson(String text) {
        String stripped = AiResponseParser.stripCodeFence(text);
        try {
            mapper.readTree(stripped);
            return stripped;
        } catch (JacksonException e) {
            return mapper.writeValueAsString(Map.of("raw", text));
        }
    }

    private AiMealResponse failure(AiRequestResult result, String requestJson, String error, int durationMs) {
        return new AiMealResponse(result, requestJson, mapper.writeValueAsString(Map.of("error", error)), List.of(),
                durationMs, error);
    }

    private static String mealTypeName(MealType type) {
        return switch (type) {
            case BREAKFAST -> "pusryčiai";
            case LUNCH -> "pietūs";
            case DINNER -> "vakarienė";
        };
    }

    private static int elapsedMs(long startedNanos) {
        return (int) ((System.nanoTime() - startedNanos) / 1_000_000);
    }
}
