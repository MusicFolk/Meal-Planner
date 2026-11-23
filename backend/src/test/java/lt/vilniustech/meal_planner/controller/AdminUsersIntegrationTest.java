package lt.vilniustech.meal_planner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import tools.jackson.databind.JsonNode;

/** Naudotojų administravimas (7 užduotis, FR-38–FR-40). */
class AdminUsersIntegrationTest extends ApiTestSupport {

    @Test
    void createdDietitianMustChangeTemporaryPassword() throws Exception {
        MockHttpSession admin = login("admin@mealplanner.lt");
        String email = "dietologas-" + UUID.randomUUID().toString().substring(0, 8) + "@mealplanner.lt";
        JsonNode created = json(postJson(admin, "/api/admin/dietitians",
                "{\"firstName\":\"Ieva\",\"lastName\":\"Testaitė\",\"email\":\"" + email + "\"}")
                .andExpect(status().isCreated()));
        String temporary = created.get("temporaryPassword").asString();
        assertThat(created.get("user").get("role").asString()).isEqualTo("DIETITIAN");

        MockHttpSession dietitian = new MockHttpSession();
        postJson(dietitian, "/api/auth/login", "{\"email\":\"" + email + "\",\"password\":\"" + temporary + "\"}")
                .andExpect(jsonPath("$.mustChangePassword").value(true));
        postJson(dietitian, "/api/auth/change-password",
                "{\"currentPassword\":\"" + temporary + "\",\"newPassword\":\"Naujas123\",\"newPasswordRepeat\":\"Naujas123\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));
        login(email, "Naujas123");

        // Antros paskyros tuo pačiu el. paštu sukurti negalima
        postJson(admin, "/api/admin/dietitians",
                "{\"firstName\":\"Ieva\",\"lastName\":\"Testaitė\",\"email\":\"" + email + "\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.email").value("Šiuo el. paštu paskyra jau užregistruota."));
    }

    @Test
    void blockedUserCannotLoginButDataIsKept() throws Exception {
        MockHttpSession admin = login("admin@mealplanner.lt");
        String email = registerClient();
        MockHttpSession client = login(email);
        long id = jdbc.queryForObject("SELECT id FROM app_user WHERE email = ?", Long.class, email);

        postJson(admin, "/api/admin/users/" + id + "/block", "{}")
                .andExpect(jsonPath("$.blocked").value(true));
        // Aktyvi sesija panaikinama, prisijungti nebegalima
        mvc.perform(get("/api/meal-plans/generation-status").session(client)).andExpect(status().isUnauthorized());
        postJson(new MockHttpSession(), "/api/auth/login", "{\"email\":\"" + email + "\",\"password\":\"Demo1234\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Paskyra užblokuota. Kreipkitės į sistemos administratorių."));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user WHERE id = ?", Integer.class, id)).isEqualTo(1);

        postJson(admin, "/api/admin/users/" + id + "/unblock", "{}").andExpect(jsonPath("$.blocked").value(false));
        login(email);
    }

    @Test
    void adminCannotBlockOwnAccountAndCanFilterUsers() throws Exception {
        MockHttpSession admin = login("admin@mealplanner.lt");
        long adminId = jdbc.queryForObject("SELECT id FROM app_user WHERE email = 'admin@mealplanner.lt'", Long.class);
        postJson(admin, "/api/admin/users/" + adminId + "/block", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Savo paskyros užblokuoti negalima."));

        JsonNode dietitians = json(mvc.perform(get("/api/admin/users?role=DIETITIAN").session(admin)));
        assertThat(dietitians).isNotEmpty().allMatch(u -> u.get("role").asString().equals("DIETITIAN"));
    }
}
