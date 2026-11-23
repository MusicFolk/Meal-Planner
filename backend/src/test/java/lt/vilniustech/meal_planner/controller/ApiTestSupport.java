package lt.vilniustech.meal_planner.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lt.vilniustech.meal_planner.TestcontainersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Bendri REST API integracinių testų veiksmai: prisijungimas, registracija ir JSON užklausos. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
abstract class ApiTestSupport {

    static final String PASSWORD = "Demo1234";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper mapper;

    MockHttpSession login(String email) throws Exception {
        return login(email, PASSWORD);
    }

    MockHttpSession login(String email, String password) throws Exception {
        MockHttpSession session = new MockHttpSession();
        postJson(session, "/api/auth/login", "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
                .andExpect(status().isOk());
        return session;
    }

    /** Užregistruoja naują klientą su unikaliu el. paštu ir grąžina jo el. paštą. */
    String registerClient() throws Exception {
        String email = "test-" + UUID.randomUUID().toString().substring(0, 8) + "@mealplanner.lt";
        postJson(new MockHttpSession(), "/api/auth/register", """
                {"firstName":"Testas","email":"%s","password":"%s","passwordRepeat":"%s","healthDataConsent":true}
                """.formatted(email, PASSWORD, PASSWORD))
                .andExpect(status().isCreated());
        return email;
    }

    ResultActions postJson(MockHttpSession session, String url, String body) throws Exception {
        return mvc.perform(post(url).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    JsonNode json(ResultActions result) throws Exception {
        return mapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
