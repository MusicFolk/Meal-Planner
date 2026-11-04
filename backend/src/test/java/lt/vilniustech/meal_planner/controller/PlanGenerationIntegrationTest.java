package lt.vilniustech.meal_planner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lt.vilniustech.meal_planner.TestcontainersConfiguration;
import lt.vilniustech.meal_planner.ai.IAIAgent;
import lt.vilniustech.meal_planner.ai.StubAiAgent;
import lt.vilniustech.meal_planner.entity.AiGenerationRequest;
import lt.vilniustech.meal_planner.entity.MealPlan;
import lt.vilniustech.meal_planner.entity.PlanGeneration;
import lt.vilniustech.meal_planner.enums.AiRequestResult;
import lt.vilniustech.meal_planner.enums.GenerationStatus;
import lt.vilniustech.meal_planner.enums.MealType;
import lt.vilniustech.meal_planner.repository.AiGenerationRequestRepository;
import lt.vilniustech.meal_planner.repository.MealPlanRepository;
import lt.vilniustech.meal_planner.repository.PlanGenerationRepository;
import lt.vilniustech.meal_planner.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/** Plano generavimo su DI integraciniai testai su netikru DI agentu ir PostgreSQL (FR-08–FR-16). */
@Import({TestcontainersConfiguration.class, PlanGenerationIntegrationTest.StubAgentConfig.class})
@SpringBootTest
@AutoConfigureMockMvc
class PlanGenerationIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class StubAgentConfig {

        @Bean
        @Primary
        StubAiAgent stubAiAgent() {
            return new StubAiAgent();
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private IAIAgent agent;

    @Autowired
    private PlanGenerationRepository generations;

    @Autowired
    private AiGenerationRequestRepository aiRequests;

    @Autowired
    private MealPlanRepository plans;

    @Autowired
    private UserRepository users;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void resetAgent() {
        stub().reset();
    }

    @Test
    void generatedPlanIsSavedAndPreviousPlanKept() throws Exception {
        Long clientId = clientId("klientas@mealplanner.lt");
        Long previousPlanId = plans.findByClientIdAndActiveTrue(clientId).map(MealPlan::getId).orElseThrow();
        MockHttpSession session = login("klientas@mealplanner.lt");
        int remaining = remaining(session);

        mvc.perform(post("/api/meal-plans/generate").session(session).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aiGenerated").value(true))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.days.length()").value(7))
                .andExpect(jsonPath("$.days[0].meals.length()").value(3));

        MealPlan active = plans.findByClientIdAndActiveTrue(clientId).orElseThrow();
        assertThat(active.getId()).isNotEqualTo(previousPlanId);
        assertThat(plans.findById(previousPlanId)).get().extracting(MealPlan::isActive).isEqualTo(false);
        // ±10 % ir nuokrypis išsaugotas kiekvienai dienai (FR-13)
        List<Double> deviations = jdbc.queryForList(
                "SELECT deviation_percent FROM plan_day WHERE meal_plan_id = ?", Double.class, active.getId());
        assertThat(deviations).hasSize(7).allMatch(d -> Math.abs(d) <= 10);
        // Nemėgstami produktai į planą nepatenka (FR-13)
        Set<Integer> fdcIds = Set.copyOf(jdbc.queryForList("""
                SELECT p.fdc_id FROM ingredient i JOIN recipe r ON r.id = i.recipe_id JOIN dish d ON d.id = r.dish_id
                JOIN meal m ON m.id = d.meal_id JOIN plan_day pd ON pd.id = m.plan_day_id
                JOIN product p ON p.id = i.product_id WHERE pd.meal_plan_id = ?""", Integer.class, active.getId()));
        assertThat(fdcIds).doesNotContain(169251, 175119);

        PlanGeneration generation = latestGeneration(clientId);
        assertThat(generation.getStatus()).isEqualTo(GenerationStatus.SUCCESS);
        assertThat(generation.getModel()).isEqualTo("stub-model");
        assertThat(requests(generation)).hasSize(3).allMatch(r -> r.getResult() == AiRequestResult.VALID);
        assertThat(remaining(session)).isEqualTo(remaining - 1);
    }

    @Test
    void allergenProductsAreNotSentToAi() throws Exception {
        Set<Long> nutProducts = Set.copyOf(jdbc.queryForList("""
                SELECT pa.product_id FROM product_allergen pa JOIN allergen a ON a.id = pa.allergen_id
                WHERE a.code = 'RIESUTAI'""", Long.class));
        assertThat(nutProducts).isNotEmpty();
        stub().answer((request, call) -> {
            assertThat(request.products()).noneMatch(p -> nutProducts.contains(p.id()));
            return StubAiAgent.valid(request);
        });

        mvc.perform(post("/api/meal-plans/generate").session(login("klientas2@mealplanner.lt")).with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    void invalidMealTypeIsRetriedOnlyForThatType() throws Exception {
        stub().answer((request, call) -> request.mealType() == MealType.LUNCH && call <= 3
                ? StubAiAgent.withUnknownProduct(request) : StubAiAgent.valid(request));
        MockHttpSession session = login("klientas@mealplanner.lt");

        mvc.perform(post("/api/meal-plans/generate").session(session).with(csrf())).andExpect(status().isCreated());

        List<AiGenerationRequest> requests = requests(latestGeneration(clientId("klientas@mealplanner.lt")));
        var byType = requests.stream().collect(Collectors.groupingBy(AiGenerationRequest::getMealType));
        assertThat(byType.get(MealType.BREAKFAST)).hasSize(1);
        assertThat(byType.get(MealType.DINNER)).hasSize(1);
        assertThat(byType.get(MealType.LUNCH)).extracting(AiGenerationRequest::getResult)
                .first().isEqualTo(AiRequestResult.INVALID_PRODUCT);
        assertThat(byType.get(MealType.LUNCH).getLast().getResult()).isEqualTo(AiRequestResult.VALID);
    }

    @Test
    void failedGenerationKeepsActivePlanAndIsNotCounted() throws Exception {
        Long clientId = clientId("klientas@mealplanner.lt");
        Long activeBefore = plans.findByClientIdAndActiveTrue(clientId).map(MealPlan::getId).orElseThrow();
        stub().answer((request, call) -> request.mealType() == MealType.DINNER
                ? StubAiAgent.timeout() : StubAiAgent.valid(request));
        MockHttpSession session = login("klientas@mealplanner.lt");
        int remaining = remaining(session);

        mvc.perform(post("/api/meal-plans/generate").session(session).with(csrf()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Plano sugeneruoti nepavyko. Ankstesnis planas nepakeistas."));

        assertThat(plans.findByClientIdAndActiveTrue(clientId).map(MealPlan::getId)).contains(activeBefore);
        PlanGeneration generation = latestGeneration(clientId);
        assertThat(generation.getStatus()).isEqualTo(GenerationStatus.FAILED);
        assertThat(generation.getMealPlan()).isNull();
        // Kiekvienas bandymas išsaugotas ir nesėkmės atveju (FR-14): vakarienė – 3 TIMEOUT
        assertThat(requests(generation)).filteredOn(r -> r.getMealType() == MealType.DINNER)
                .hasSize(3).allMatch(r -> r.getResult() == AiRequestResult.TIMEOUT);
        assertThat(remaining(session)).isEqualTo(remaining);

        mvc.perform(get("/api/meal-plans/generations/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.meals[0].type").value("BREAKFAST"))
                .andExpect(jsonPath("$.meals[0].done").value(true))
                .andExpect(jsonPath("$.meals[2].attempts").value(3));
    }

    @Test
    void secondGenerationIsRejectedWhileFirstIsRunning() throws Exception {
        Long clientId = clientId("klientas@mealplanner.lt");
        jdbc.update("INSERT INTO plan_generation (client_id, started_at, status, model) VALUES (?, now(), 'IN_PROGRESS', 'x')",
                clientId);
        try {
            MockHttpSession session = login("klientas@mealplanner.lt");
            mvc.perform(get("/api/meal-plans/generation-status").session(session))
                    .andExpect(jsonPath("$.canGenerate").value(false))
                    .andExpect(jsonPath("$.inProgress").value(true));
            mvc.perform(post("/api/meal-plans/generate").session(session).with(csrf()))
                    .andExpect(status().isConflict());
            assertThat(stub().calls()).isZero();
        } finally {
            jdbc.update("DELETE FROM plan_generation WHERE client_id = ? AND status = 'IN_PROGRESS'", clientId);
        }
    }

    private StubAiAgent stub() {
        return (StubAiAgent) agent;
    }

    private Long clientId(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow().getId();
    }

    private PlanGeneration latestGeneration(Long clientId) {
        return generations.findFirstByClientIdOrderByStartedAtDesc(clientId).orElseThrow();
    }

    private List<AiGenerationRequest> requests(PlanGeneration generation) {
        return aiRequests.findByPlanGenerationIdOrderByIdAsc(generation.getId());
    }

    private int remaining(MockHttpSession session) throws Exception {
        String body = mvc.perform(get("/api/meal-plans/generation-status").session(session))
                .andReturn().getResponse().getContentAsString();
        return Integer.parseInt(body.replaceAll(".*\"remainingToday\":(\\d+).*", "$1"));
    }

    private MockHttpSession login(String email) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/login").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Demo1234\"}"))
                .andExpect(status().isOk());
        return session;
    }
}
