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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/** Planų istorijos integraciniai testai (FR-22, PLN-7). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PlanHistoryIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void historyListsAllClientPlansNewestFirst() throws Exception {
        mvc.perform(get("/api/meal-plans").session(login("klientas@mealplanner.lt")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].dailyCalories").value(2345))
                .andExpect(jsonPath("$[0].firstDayDishes.length()").value(3))
                .andExpect(jsonPath("$[1].active").value(false))
                .andExpect(jsonPath("$[1].dailyCalories").value(2400));
    }

    @Test
    void previousPlanCanBeMadeActiveAndCurrentPlanIsKept() throws Exception {
        MockHttpSession session = login("klientas@mealplanner.lt");
        Long current = planId("klientas@mealplanner.lt", true);
        Long previous = planId("klientas@mealplanner.lt", false);
        try {
            mvc.perform(put("/api/meal-plans/" + previous + "/active").session(session).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(previous))
                    .andExpect(jsonPath("$.active").value(true));
            mvc.perform(get("/api/meal-plans/active").session(session))
                    .andExpect(jsonPath("$.id").value(previous));
            // Ankstesnis aktualus planas neištrinamas ir lieka istorijoje
            mvc.perform(get("/api/meal-plans/" + current).session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false))
                    .andExpect(jsonPath("$.days.length()").value(7));
            mvc.perform(get("/api/meal-plans/" + current + "/shopping-list").session(session))
                    .andExpect(status().isOk());
        } finally {
            mvc.perform(put("/api/meal-plans/" + current + "/active").session(session).with(csrf()))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void clientCannotOpenOrActivateOtherPlans() throws Exception {
        MockHttpSession session = login("klientas2@mealplanner.lt");
        Long foreign = planId("klientas@mealplanner.lt", false);
        Long template = jdbc.queryForObject("SELECT min(id) FROM meal_plan WHERE client_id IS NULL", Long.class);

        mvc.perform(get("/api/meal-plans/" + foreign).session(session)).andExpect(status().isNotFound());
        mvc.perform(put("/api/meal-plans/" + foreign + "/active").session(session).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/meal-plans/" + template + "/active").session(session).with(csrf()))
                .andExpect(status().isNotFound());
    }

    private Long planId(String email, boolean active) {
        return jdbc.queryForObject("""
                SELECT p.id FROM meal_plan p JOIN app_user u ON u.id = p.client_id
                WHERE u.email = ? AND p.active = ? ORDER BY p.created_at DESC LIMIT 1""", Long.class, email, active);
    }

    private MockHttpSession login(String email) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/login").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Demo1234\"}"))
                .andExpect(status().isOk());
        return session;
    }
}
