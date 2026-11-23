package lt.vilniustech.meal_planner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import tools.jackson.databind.JsonNode;

/** Dietologo peržiūros užsakymas, apmokėjimas ir peržiūra (4 ir 6 užduotys, FR-21, FR-23–FR-27, FR-34–FR-37). */
class ReviewFlowIntegrationTest extends ApiTestSupport {

    @Test
    void rejectedPaymentCreatesNoReview() throws Exception {
        MockHttpSession client = login("klientas@mealplanner.lt");
        int before = reviewCount();
        String reference = order(client, "Ar tinka sportuojant?");

        postJson(client, "/api/bank/payments/" + reference + "/confirm", "{\"approved\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));

        assertThat(reviewCount()).isEqualTo(before);
        mvc.perform(get("/api/reviews/order-info").session(client)).andExpect(jsonPath("$.canOrder").value(true));
    }

    @Test
    void paidReviewIsCommentedByDietitianAndShownToClient() throws Exception {
        MockHttpSession client = login("klientas@mealplanner.lt");
        String reference = order(client, "Ar užtenka baltymų?");
        postJson(client, "/api/bank/payments/" + reference + "/confirm", "{\"approved\":true}")
                .andExpect(jsonPath("$.status").value("PAID"));

        // Užklausa sukuriama tik apmokėjus, antros užsakyti tam pačiam planui negalima (FR-23, FR-25)
        JsonNode current = json(mvc.perform(get("/api/reviews/current").session(client)));
        assertThat(current.get("status").asString()).isEqualTo("PENDING");
        long reviewId = current.get("id").asLong();
        mvc.perform(get("/api/reviews/order-info").session(client))
                .andExpect(jsonPath("$.canOrder").value(false))
                .andExpect(jsonPath("$.reason").value("Šiam planui jau užsakyta peržiūra."));

        MockHttpSession dietitian = login("dietologas2@mealplanner.lt");
        mvc.perform(get("/api/dietitian/reviews?status=PENDING").session(dietitian))
                .andExpect(jsonPath("$[?(@.id == " + reviewId + ")]").exists());

        // Atidarius užklausą ji priskiriama dietologui, anketa pateikiama be kontaktinių duomenų (FR-35)
        JsonNode detail = json(mvc.perform(post("/api/dietitian/reviews/" + reviewId + "/open").session(dietitian)
                .with(csrf())));
        assertThat(detail.get("status").asString()).isEqualTo("IN_REVIEW");
        assertThat(detail.toString()).doesNotContain("klientas@mealplanner.lt");
        long dishId = detail.get("plan").get("days").get(0).get("meals").get(0).get("dish").get("id").asLong();

        postJson(dietitian, "/api/dietitian/reviews/" + reviewId + "/comments",
                "{\"dishId\":" + dishId + ",\"text\":\"Pusryčiams pridėkite daugiau baltymų.\"}")
                .andExpect(status().isCreated());
        postJson(dietitian, "/api/dietitian/reviews/" + reviewId + "/submit", "{\"generalComment\":\"\"}")
                .andExpect(status().isBadRequest());
        postJson(dietitian, "/api/dietitian/reviews/" + reviewId + "/submit",
                "{\"generalComment\":\"Planas tinkamas, laikykitės jo.\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Pateikus peržiūrą komentarų redaguoti nebegalima, klientas gauna pranešimą ir mato komentarus (FR-37, FR-21)
        postJson(dietitian, "/api/dietitian/reviews/" + reviewId + "/comments",
                "{\"dishId\":" + dishId + ",\"text\":\"Dar vienas.\"}")
                .andExpect(status().isConflict());
        mvc.perform(get("/api/reviews/current").session(client))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.comments.length()").value(2));
        mvc.perform(get("/api/notifications").session(client))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void otherDietitianCannotOpenAssignedReview() throws Exception {
        long pending = jdbc.queryForObject("""
                SELECT r.id FROM review_request r JOIN app_user u ON u.id = r.client_id
                WHERE u.email = 'klientas3@mealplanner.lt' AND r.status = 'PENDING'""", Long.class);
        mvc.perform(post("/api/dietitian/reviews/" + pending + "/open").session(login("dietologas@mealplanner.lt"))
                .with(csrf())).andExpect(status().isOk());

        mvc.perform(post("/api/dietitian/reviews/" + pending + "/open").session(login("dietologas2@mealplanner.lt"))
                .with(csrf())).andExpect(status().isNotFound());
    }

    private String order(MockHttpSession client, String question) throws Exception {
        JsonNode initiated = json(postJson(client, "/api/reviews/order",
                "{\"method\":\"CARD\",\"question\":\"" + question + "\"}").andExpect(status().isOk()));
        String reference = initiated.get("reference").asString();
        mvc.perform(get("/api/payments/" + reference).session(client))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.purpose").value("REVIEW"));
        return reference;
    }

    private int reviewCount() {
        return jdbc.queryForObject("SELECT count(*) FROM review_request", Integer.class);
    }
}
