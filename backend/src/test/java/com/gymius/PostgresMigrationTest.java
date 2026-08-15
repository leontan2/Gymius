package com.gymius;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.security.oauth2.client.registration.google.client-id=test-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-secret",
        "app.meal-vision.provider=mock"
})
@EnabledIfEnvironmentVariable(named = "POSTGRES_TEST_URL", matches = ".+")
class PostgresMigrationTest {

    @Autowired
    private Flyway flyway;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("POSTGRES_TEST_URL"));
        registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("POSTGRES_TEST_USERNAME", "gymius"));
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("POSTGRES_TEST_PASSWORD", "gymius"));
    }

    @Test
    void appliesAndValidatesSchemaAgainstPostgres() {
        assertThat(flyway.info().current()).isNotNull();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
    }
}
