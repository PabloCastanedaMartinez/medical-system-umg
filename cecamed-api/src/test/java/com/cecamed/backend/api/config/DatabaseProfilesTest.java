package com.cecamed.backend.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;

class DatabaseProfilesTest {

    private ApplicationContextRunner profile(String profile) {
        return new ApplicationContextRunner()
                .withPropertyValues("spring.config.location=classpath:/",
                        "spring.profiles.active=" + profile)
                .withInitializer(context -> {
                    // Las credenciales del desarrollador no deben influir en estas pruebas.
                    var sources = context.getEnvironment().getPropertySources();
                    sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                    sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
                    new ConfigDataApplicationContextInitializer().initialize(context);
                });
    }

    @Test
    void localConnectionUsesTheSamePortAndDatabaseAsCompose() {
        profile("local").withPropertyValues("DB_HOST=127.0.0.1", "DB_PORT=5544",
                        "DB_NAME=isolated_test", "DB_PASSWORD=test-only")
                .run(context -> assertThat(context.getEnvironment().getProperty("spring.datasource.url"))
                        .isEqualTo("jdbc:postgresql://127.0.0.1:5544/isolated_test"));
    }

    @Test
    void explicitUrlOverridesLocalConnectionParts() {
        profile("local").withPropertyValues("DB_URL=jdbc:postgresql://database:5432/custom",
                        "DB_PORT=5544", "DB_NAME=ignored")
                .run(context -> assertThat(context.getEnvironment().getProperty("spring.datasource.url"))
                        .isEqualTo("jdbc:postgresql://database:5432/custom"));
    }

    @Test
    void productionRequiresExplicitCredentialsAndDoesNotImportLocalDotEnv() {
        profile("prod").run(context -> {
            var environment = context.getEnvironment();
            assertThat(environment.getProperty("spring.config.import")).isNull();
            for (var property : new String[] {"url", "username", "password"}) {
                assertThatThrownBy(() -> environment.getProperty("spring.datasource." + property))
                        .isInstanceOf(IllegalArgumentException.class);
            }
            assertThat(environment.getProperty("springdoc.api-docs.enabled", Boolean.class)).isFalse();
            assertThat(environment.getProperty("cecamed.cors.allowed-origins")).isEmpty();
        });
    }

    @Test
    void testProfileUsesH2WithoutLocalCredentialsOrFlyway() {
        profile("test").run(context -> {
            var environment = context.getEnvironment();
            assertThat(environment.getProperty("spring.datasource.url")).startsWith("jdbc:h2:mem:");
            assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("test-only");
            assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isFalse();
            assertThat(environment.getProperty("spring.config.import")).isNull();
        });
    }
}
