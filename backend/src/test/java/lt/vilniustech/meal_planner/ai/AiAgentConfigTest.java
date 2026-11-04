package lt.vilniustech.meal_planner.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import lt.vilniustech.meal_planner.config.AiAgentConfig;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

/** DI adapteris sukuriamas su konfigūracijoje nurodytu modeliu, šablonu ir schema (DIA-4, NFR-45). */
class AiAgentConfigTest {

    private final AiAgentConfig config = new AiAgentConfig();

    @Test
    void agentIsCreatedForGemini3AndGemini2Models() throws Exception {
        assertThat(agent("gemini-3.5-flash-lite").modelName()).isEqualTo("gemini-3.5-flash-lite");
        assertThat(agent("gemini-2.5-flash").modelName()).isEqualTo("gemini-2.5-flash");
    }

    private IAIAgent agent(String model) throws Exception {
        return config.aiAgent("test-raktas", model, "MINIMAL", Duration.ofSeconds(25),
                new ClassPathResource("prompts/meal-system-prompt.txt"),
                new ClassPathResource("prompts/meal-response-schema.json"), JsonMapper.builder().build());
    }
}
