package lt.vilniustech.meal_planner.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lt.vilniustech.meal_planner.ai.GeminiAgent;
import lt.vilniustech.meal_planner.ai.IAIAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.common.GoogleGenAiThinkingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * DI adapterio konfigūracija. DI modelis nurodomas aplinkos kintamuoju GEMINI_MODEL (DIA-4, 2 sprintas),
 * API raktas – GEMINI_API_KEY (NFR-57). Spring AI ir Gemini kliento pakartojimai išjungti: pakartojimus
 * pagal valgymo tipą valdo mitybos plano komponentas (FR-14).
 */
@Configuration
public class AiAgentConfig {

    private static final Logger log = LoggerFactory.getLogger(AiAgentConfig.class);

    @Bean
    public IAIAgent aiAgent(@Value("${app.ai.api-key:}") String apiKey,
                            @Value("${app.ai.model}") String model,
                            @Value("${app.ai.thinking-level}") String thinkingLevel,
                            @Value("${app.ai.call-timeout}") Duration callTimeout,
                            @Value("${app.ai.system-prompt}") Resource systemPrompt,
                            @Value("${app.ai.response-schema}") Resource responseSchema,
                            ObjectMapper mapper) throws IOException {
        GoogleGenAiChatOptions.Builder options = GoogleGenAiChatOptions.builder();
        options.model(model);
        options.responseMimeType("application/json");
        options.responseSchema(responseSchema.getContentAsString(StandardCharsets.UTF_8));
        // Mąstymas sulėtina atsakymą, todėl jis sumažinamas iki minimumo (25 s riba, NFR-28)
        if (model.startsWith("gemini-2")) {
            options.thinkingBudget(0);
        } else {
            options.thinkingLevel(GoogleGenAiThinkingLevel.valueOf(thinkingLevel));
        }

        GoogleGenAiChatModel chatModel = null;
        if (apiKey.isBlank()) {
            log.warn("GEMINI_API_KEY nenurodytas – plano generavimas su DI neveiks, kitos funkcijos veikia");
        } else {
            Client client = Client.builder()
                    .apiKey(apiKey)
                    .httpOptions(HttpOptions.builder()
                            .timeout((int) callTimeout.toMillis())
                            .retryOptions(HttpRetryOptions.builder().attempts(1).build())
                            .build())
                    .build();
            chatModel = GoogleGenAiChatModel.builder()
                    .genAiClient(client)
                    .options(options.build())
                    .retryTemplate(new RetryTemplate(RetryPolicy.withMaxRetries(0)))
                    .observationRegistry(ObservationRegistry.NOOP)
                    .build();
        }
        return new GeminiAgent(chatModel, model, options.build(),
                systemPrompt.getContentAsString(StandardCharsets.UTF_8), mapper);
    }
}
