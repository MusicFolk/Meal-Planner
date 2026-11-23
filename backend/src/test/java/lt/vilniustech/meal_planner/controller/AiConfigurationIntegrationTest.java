package lt.vilniustech.meal_planner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import lt.vilniustech.meal_planner.TestcontainersConfiguration;
import lt.vilniustech.meal_planner.service.IAiConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/** DI parametrų administravimo integraciniai testai (NI-20, FR-41, NAU-3, DIA-4). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AiConfigurationIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private IAiConfiguration configuration;

    @Value("${app.ai.model}")
    private String defaultModel;

    @Value("${app.plans.daily-generation-limit}")
    private int defaultLimit;

    @BeforeEach
    @AfterEach
    void clean() {
        jdbc.update("DELETE FROM ai_configuration");
        jdbc.update("DELETE FROM plan_generation WHERE client_id = (SELECT id FROM app_user WHERE email = ?)",
                "klientas2@mealplanner.lt");
    }

    @Test
    void defaultsAreUsedUntilAdminChangesParameters() throws Exception {
        mvc.perform(get("/api/admin/ai-configuration").session(login("admin@mealplanner.lt")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value(defaultModel))
                .andExpect(jsonPath("$.dailyGenerationLimit").value(defaultLimit))
                .andExpect(jsonPath("$.changedAt").doesNotExist());
        assertThat(configuration.model()).isEqualTo(defaultModel);
    }

    @Test
    void adminChangeIsSavedAsNewRecordAndAppliedToNextGeneration() throws Exception {
        MockHttpSession admin = login("admin@mealplanner.lt");
        update(admin, "gemini-3.5-flash", 3);
        mvc.perform(put("/api/admin/ai-configuration").session(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"model\":\"gemini-3.6-flash\",\"dailyGenerationLimit\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("gemini-3.6-flash"))
                .andExpect(jsonPath("$.dailyGenerationLimit").value(7))
                .andExpect(jsonPath("$.changedBy").value("Administratorius Sistemos"))
                .andExpect(jsonPath("$.changedAt").exists());

        // Kiekvienas pakeitimas – naujas įrašas, galioja naujausias
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ai_configuration", Integer.class)).isEqualTo(2);
        assertThat(configuration.model()).isEqualTo("gemini-3.6-flash");
        mvc.perform(get("/api/meal-plans/generation-status").session(login("klientas2@mealplanner.lt")))
                .andExpect(jsonPath("$.dailyLimit").value(7))
                .andExpect(jsonPath("$.remainingToday").value(7));
    }

    @Test
    void invalidParametersAreRejected() throws Exception {
        mvc.perform(put("/api/admin/ai-configuration").session(login("admin@mealplanner.lt")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"model\":\"gemini 3!\",\"dailyGenerationLimit\":21}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.model")
                        .value("Modelio pavadinime gali būti tik raidės, skaitmenys, taškas ir brūkšnelis."))
                .andExpect(jsonPath("$.fieldErrors.dailyGenerationLimit").value("Limitas turi būti nuo 1 iki 20."));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ai_configuration", Integer.class)).isZero();
    }

    @Test
    void clientCannotChangeParameters() throws Exception {
        mvc.perform(put("/api/admin/ai-configuration").session(login("klientas2@mealplanner.lt")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"model\":\"gemini-3.6-flash\",\"dailyGenerationLimit\":7}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void reachedLimitTellsWhenClientCanGenerateAgain() throws Exception {
        update(login("admin@mealplanner.lt"), "gemini-3.5-flash-lite", 1);
        jdbc.update("""
                INSERT INTO plan_generation (client_id, started_at, finished_at, status, model)
                SELECT id, now(), now(), 'SUCCESS', 'gemini-3.5-flash-lite' FROM app_user WHERE email = ?""",
                "klientas2@mealplanner.lt");
        LocalDate tomorrow = LocalDate.now(ZoneId.of("Europe/Vilnius")).plusDays(1);

        mvc.perform(get("/api/meal-plans/generation-status").session(login("klientas2@mealplanner.lt")))
                .andExpect(jsonPath("$.canGenerate").value(false))
                .andExpect(jsonPath("$.remainingToday").value(0))
                .andExpect(jsonPath("$.reason").value("Šiandien planą galite generuoti dar 0 kartų. Vėl galėsite "
                        + "generuoti " + tomorrow + " nuo 00:00."));
    }

    private void update(MockHttpSession admin, String model, int limit) throws Exception {
        mvc.perform(put("/api/admin/ai-configuration").session(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"model\":\"" + model + "\",\"dailyGenerationLimit\":" + limit + "}"))
                .andExpect(status().isOk());
    }

    private MockHttpSession login(String email) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/login").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Demo1234\"}"))
                .andExpect(status().isOk());
        return session;
    }
}
