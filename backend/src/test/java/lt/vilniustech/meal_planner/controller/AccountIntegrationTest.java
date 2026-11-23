package lt.vilniustech.meal_planner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

/** Registracija, prisijungimas, anketa ir paskyros valdymas (1, 5 ir 8 užduotys). */
class AccountIntegrationTest extends ApiTestSupport {

    private static final String QUESTIONNAIRE = """
            {"heightCm":180,"weightKg":80,"age":30,"gender":"MALE","activityLevel":"MODERATE",
             "goal":"LOSE_WEIGHT","chronicDisease":%s,"allergenIds":[],"dislikedProductIds":[]}""";

    @Test
    void registrationRequiresMatchingPasswordAndConsent() throws Exception {
        postJson(new MockHttpSession(), "/api/auth/register", """
                {"firstName":"Jonas","email":"naujas@mealplanner.lt","password":"Slaptas123",
                 "passwordRepeat":"Kitas1234","healthDataConsent":false}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.healthDataConsent")
                        .value("Būtinas sutikimas tvarkyti sveikatos duomenis."));
    }

    @Test
    void loginIsLockedAfterFiveFailedAttemptsAndLogoutEndsSession() throws Exception {
        String email = registerClient();
        for (int i = 0; i < 4; i++) {
            postJson(new MockHttpSession(), "/api/auth/login", "{\"email\":\"" + email + "\",\"password\":\"Blogas123\"}")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Neteisingas el. paštas arba slaptažodis."));
        }
        postJson(new MockHttpSession(), "/api/auth/login", "{\"email\":\"" + email + "\",\"password\":\"Blogas123\"}")
                .andExpect(jsonPath("$.message").value("Prisijungimas užrakintas 15 minučių. Bandykite vėliau."));
        // Užrakinus net teisingas slaptažodis netinka (FR-31, NFR-52)
        postJson(new MockHttpSession(), "/api/auth/login", "{\"email\":\"" + email + "\",\"password\":\"Demo1234\"}")
                .andExpect(status().isUnauthorized());

        MockHttpSession session = login("klientas2@mealplanner.lt");
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void questionnaireCalculatesMetricsAndWarnsAboutChronicDisease() throws Exception {
        MockHttpSession client = login(registerClient());
        mvc.perform(put("/api/questionnaire").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(QUESTIONNAIRE.formatted(true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstTime").value(true))
                .andExpect(jsonPath("$.chronicDiseaseWarning").value(true))
                // 10·80 + 6,25·180 − 5·30 + 5 = 1780; × 1,55 × 0,85 = 2345 kcal (FR-04, FR-05)
                .andExpect(jsonPath("$.questionnaire.metrics.bmi").value(24.7))
                .andExpect(jsonPath("$.questionnaire.metrics.dailyCalories").value(2345));

        mvc.perform(put("/api/questionnaire").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(QUESTIONNAIRE.formatted(false)))
                .andExpect(jsonPath("$.firstTime").value(false))
                .andExpect(jsonPath("$.chronicDiseaseWarning").value(false));
    }

    @Test
    void settingsAreSavedAndDataCanBeExported() throws Exception {
        String email = registerClient();
        MockHttpSession client = login(email);
        mvc.perform(put("/api/account/settings").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"theme\":\"DARK\",\"firstDayOfWeek\":\"SUNDAY\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/account/settings").session(client))
                .andExpect(jsonPath("$.theme").value("DARK"))
                .andExpect(jsonPath("$.firstDayOfWeek").value("SUNDAY"));

        mvc.perform(get("/api/account/export").session(client))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("mealplanner_duomenys_")))
                .andExpect(jsonPath("$.profile.email").value(email))
                .andExpect(jsonPath("$.subscription.status").value("TRIAL"));
    }

    @Test
    void deletedAccountRemovesPersonalDataAndAnonymizesPayments() throws Exception {
        String email = registerClient();
        MockHttpSession client = login(email);
        String reference = json(postJson(client, "/api/subscription/pay", "{\"method\":\"CARD\"}")).get("reference")
                .asString();
        postJson(client, "/api/bank/payments/" + reference + "/confirm", "{\"approved\":true}");

        mvc.perform(delete("/api/account").session(client).with(csrf())).andExpect(status().isNoContent());

        postJson(new MockHttpSession(), "/api/auth/login", "{\"email\":\"" + email + "\",\"password\":\"Demo1234\"}")
                .andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user WHERE email = ?", Integer.class, email)).isZero();
        assertThat(jdbc.queryForObject("SELECT client_id FROM payment WHERE reference = ?::uuid", Long.class,
                reference)).isNull();
    }
}
