package lt.vilniustech.meal_planner.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lt.vilniustech.meal_planner.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void unauthenticatedUserGetsUnauthorized() throws Exception {
        mvc.perform(get("/api/meal-plans/active"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Prisijunkite prie sistemos."));
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Jonas","email":"klientas@mealplanner.lt","password":"Slaptas123",
                                 "passwordRepeat":"Slaptas123","healthDataConsent":true}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Šiuo el. paštu paskyra jau užregistruota."));
    }

    @Test
    void clientCannotAccessAdminFunctions() throws Exception {
        MockHttpSession session = login("klientas2@mealplanner.lt");
        mvc.perform(get("/api/admin/users").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/questionnaire").session(session)).andExpect(status().isOk());
    }

    @Test
    void questionnaireOutOfRangeIsRejected() throws Exception {
        MockHttpSession session = login("klientas2@mealplanner.lt");
        mvc.perform(put("/api/questionnaire")
                        .session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"heightCm":99,"weightKg":80,"age":17,"gender":"MALE","activityLevel":"MODERATE",
                                 "goal":"MAINTAIN","chronicDisease":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.heightCm").value("Ūgis turi būti nuo 100 iki 250 cm."))
                .andExpect(jsonPath("$.fieldErrors.age").value("Amžius turi būti nuo 18 iki 100 metų."));
    }

    private MockHttpSession login(String email) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/login").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Demo1234\"}"))
                .andExpect(status().isOk());
        return session;
    }
}
