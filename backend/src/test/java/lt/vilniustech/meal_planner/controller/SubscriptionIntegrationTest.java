package lt.vilniustech.meal_planner.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

/** Prenumeratos būsena ir apmokėjimas (8 užduotis, FR-08, FR-42–FR-44, NFR-26). */
class SubscriptionIntegrationTest extends ApiTestSupport {

    private static final String EXPIRED_CLIENT = "klientas3@mealplanner.lt";

    @AfterEach
    void restoreExpiredSubscription() {
        jdbc.update("""
                UPDATE subscription SET paid_until = now() - INTERVAL '5 days'
                WHERE client_id = (SELECT id FROM app_user WHERE email = ?)""", EXPIRED_CLIENT);
    }

    @Test
    void newClientGetsSevenDayTrial() throws Exception {
        mvc.perform(get("/api/subscription").session(login(registerClient())))
                .andExpect(jsonPath("$.status").value("TRIAL"))
                .andExpect(jsonPath("$.canPay").value(true));
    }

    @Test
    void expiredSubscriptionBlocksGenerationUntilPaid() throws Exception {
        MockHttpSession client = login(EXPIRED_CLIENT);
        mvc.perform(get("/api/subscription").session(client)).andExpect(jsonPath("$.status").value("EXPIRED"));
        mvc.perform(get("/api/meal-plans/generation-status").session(client))
                .andExpect(jsonPath("$.canGenerate").value(false))
                .andExpect(jsonPath("$.reason")
                        .value("Prenumerata nebegalioja. Norėdami generuoti planą, atnaujinkite prenumeratą."));
        // Ankstesni planai lieka matomi (FR-42)
        mvc.perform(get("/api/meal-plans/active").session(client)).andExpect(status().isOk());

        // Atmestas mokėjimas būsenos nekeičia (FR-43, NFR-26)
        String rejected = pay(client);
        postJson(client, "/api/bank/payments/" + rejected + "/confirm", "{\"approved\":false}")
                .andExpect(jsonPath("$.status").value("FAILED"));
        mvc.perform(get("/api/subscription").session(client)).andExpect(jsonPath("$.status").value("EXPIRED"));

        String paid = pay(client);
        postJson(client, "/api/bank/payments/" + paid + "/confirm", "{\"approved\":true}")
                .andExpect(jsonPath("$.status").value("PAID"));
        mvc.perform(get("/api/subscription").session(client))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.canPay").value(false));
        // Apdoroto mokėjimo pakartotinai patvirtinti negalima
        postJson(client, "/api/bank/payments/" + paid + "/confirm", "{\"approved\":true}")
                .andExpect(status().isConflict());
    }

    @Test
    void activeSubscriptionCannotBePaidAgain() throws Exception {
        postJson(login("klientas@mealplanner.lt"), "/api/subscription/pay", "{\"method\":\"BANK_LINK\"}")
                .andExpect(status().isConflict());
    }

    private String pay(MockHttpSession client) throws Exception {
        return json(postJson(client, "/api/subscription/pay", "{\"method\":\"BANK_LINK\"}")
                .andExpect(status().isOk())).get("reference").asString();
    }
}
