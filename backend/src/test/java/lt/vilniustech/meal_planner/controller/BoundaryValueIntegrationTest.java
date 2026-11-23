package lt.vilniustech.meal_planner.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

/** Ribinių reikšmių testavimo atvejai pagal formuluojamų užduočių ribas (17 lentelė, FR-01, FR-28, FR-36, FR-41). */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BoundaryValueIntegrationTest extends ApiTestSupport {

    private MockHttpSession client;
    private MockHttpSession admin;

    @BeforeAll
    void sessions() throws Exception {
        client = login(registerClient());
        admin = login("admin@mealplanner.lt");
    }

    @AfterAll
    void cleanAiConfiguration() {
        jdbc.update("DELETE FROM ai_configuration");
    }

    @ParameterizedTest(name = "{0} = {1} → {2}")
    @CsvSource({
            "heightCm, 99, 400", "heightCm, 100, 200", "heightCm, 250, 200", "heightCm, 251, 400",
            "weightKg, 29.9, 400", "weightKg, 30, 200", "weightKg, 300, 200", "weightKg, 300.1, 400",
            "weightKg, 80.25, 400",
            "age, 17, 400", "age, 18, 200", "age, 100, 200", "age, 101, 400"})
    void questionnaireLimits(String field, String value, int expected) throws Exception {
        String body = """
                {"heightCm":180,"weightKg":80,"age":30,"gender":"FEMALE","activityLevel":"LIGHT",
                 "goal":"MAINTAIN","chronicDisease":false}""".replaceFirst("\"" + field + "\":[^,]+",
                "\"" + field + "\":" + value);
        mvc.perform(put("/api/questionnaire").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().is(expected));
    }

    @ParameterizedTest(name = "{0} nemėgstamų produktų → {1}")
    @CsvSource({"20, 200", "21, 400"})
    void dislikedProductsLimit(int count, int expected) throws Exception {
        String ids = String.join(",", jdbc.queryForList("SELECT id::text FROM product ORDER BY id LIMIT ?",
                String.class, count));
        mvc.perform(put("/api/questionnaire").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"heightCm":180,"weightKg":80,"age":30,"gender":"FEMALE","activityLevel":"LIGHT",
                         "goal":"MAINTAIN","chronicDisease":false,"dislikedProductIds":[%s]}""".formatted(ids)))
                .andExpect(status().is(expected));
    }

    @ParameterizedTest(name = "slaptažodis „{0}“ → {1}")
    @CsvSource({"Abcdef1, 400", "Abcdefg1, 201", "abcdefgh, 400", "12345678, 400"})
    void passwordRules(String password, int expected) throws Exception {
        String email = "riba-" + Math.abs(password.hashCode()) + "-" + System.nanoTime() + "@mealplanner.lt";
        postJson(new MockHttpSession(), "/api/auth/register", """
                {"firstName":"Riba","email":"%s","password":"%s","passwordRepeat":"%s","healthDataConsent":true}
                """.formatted(email, password, password)).andExpect(status().is(expected));
    }

    @ParameterizedTest(name = "slaptažodžio ilgis {0} → {1}")
    @CsvSource({"64, 201", "65, 400"})
    void passwordLength(int length, int expected) throws Exception {
        String password = "A1" + String.join("", Collections.nCopies(length - 2, "b"));
        postJson(new MockHttpSession(), "/api/auth/register", """
                {"firstName":"Riba","email":"ilgis-%d-%d@mealplanner.lt","password":"%s","passwordRepeat":"%s",
                 "healthDataConsent":true}""".formatted(length, System.nanoTime(), password, password))
                .andExpect(status().is(expected));
    }

    @ParameterizedTest(name = "limitas {0} → {1}")
    @CsvSource({"0, 400", "1, 200", "20, 200", "21, 400"})
    void dailyGenerationLimit(int limit, int expected) throws Exception {
        mvc.perform(put("/api/admin/ai-configuration").session(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"model\":\"gemini-3.5-flash-lite\",\"dailyGenerationLimit\":" + limit + "}"))
                .andExpect(status().is(expected));
    }

    @ParameterizedTest(name = "peržiūros klausimas {0} simb. → {1}")
    @CsvSource({"501, 400"})
    void reviewQuestionLength(int length, int expected) throws Exception {
        String question = String.join("", Collections.nCopies(length, "a"));
        postJson(login("klientas3@mealplanner.lt"), "/api/reviews/order",
                "{\"method\":\"CARD\",\"question\":\"" + question + "\"}").andExpect(status().is(expected));
    }
}
