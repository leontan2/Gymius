package com.gymius;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.security.oauth2.client.registration.google.client-id=test-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-secret",
        "app.meal-vision.provider=mock"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "POSTGRES_TEST_URL", matches = ".+")
class DatabaseMigrationTest {

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("POSTGRES_TEST_URL"));
        registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("POSTGRES_TEST_USERNAME", "gymius"));
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("POSTGRES_TEST_PASSWORD", "gymius"));
    }

    @Autowired
    private Flyway flyway;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void appliesLatestMigrationAndValidatesEntityMappings() {
        assertThat(flyway.info().current()).isNotNull();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
    }

    @Test
    void unauthenticatedApiRequestsReturnUnauthorizedInsteadOfRedirecting() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void logoutRejectsRequestsWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void logoutAcceptsRequestsWithCsrfToken() throws Exception {
        mockMvc.perform(post("/api/logout").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void issuedCsrfTokenWorksWithTheSameSession() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        String token = objectMapper.readTree(csrfResult.getResponse().getContentAsString())
                .path("token")
                .asText();

        mockMvc.perform(post("/api/logout")
                        .session(session)
                        .header("X-CSRF-TOKEN", token))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void unknownApiRoutesPreserveNotFoundStatus() throws Exception {
        mockMvc.perform(get("/api/not-a-route"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void unsupportedMethodsPreserveMethodNotAllowedStatus() throws Exception {
        mockMvc.perform(patch("/api/csrf").with(csrf()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockUser
    void missingMealImageReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/nutrition/analyze-image").with(csrf()))
                .andExpect(status().isBadRequest());
    }
}
