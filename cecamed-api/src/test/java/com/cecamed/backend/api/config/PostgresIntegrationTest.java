package com.cecamed.backend.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Tag("postgres")
@SpringBootTest
@ActiveProfiles({"prod", "postgres-test"})
class PostgresIntegrationTest {

    @Autowired DataSource dataSource;
    @Autowired Flyway flyway;
    @Autowired Clock clock;

    @Test
    void flywayPreparesItsSchemaAndCanMigrateAgainWithoutChanges() throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            try (var result = statement.executeQuery("SELECT to_regclass('cecamed.flyway_schema_history')")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isNotNull();
            }
        }
        flyway.validate();
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(java.util.Arrays.stream(flyway.info().applied())
                .filter(migration -> migration.getVersion() != null)
                .map(migration -> migration.getVersion().toString()))
                .containsExactly("1", "2", "3", "4", "5");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(flyway.getConfiguration().isCleanDisabled()).isTrue();
        assertThat(flyway.getConfiguration().isBaselineOnMigrate()).isFalse();
    }

    @Test
    void clinicalTablesAndPatientForeignKeysExist() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            for (String table : new String[]{"patients", "medical_records", "consultations"}) {
                try (var result = connection.getMetaData().getTables(null, "cecamed", table, new String[]{"TABLE"})) {
                    assertThat(result.next()).as("Tabla %s", table).isTrue();
                }
            }
            for (String table : new String[]{"medical_records", "consultations"}) {
                try (var keys = connection.getMetaData().getImportedKeys(null, "cecamed", table)) {
                    assertThat(keys.next()).as("FK de %s a patients", table).isTrue();
                    assertThat(keys.getString("PKTABLE_NAME")).isEqualTo("patients");
                    assertThat(keys.getString("FKCOLUMN_NAME")).isEqualTo("patient_id");
                }
            }
        }
    }

    @Test
    void connectionsAndClockUseUtcAndPreserveInstants() throws SQLException {
        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("SHOW TIME ZONE")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("UTC");
            }
            statement.execute("CREATE TEMP TABLE utc_probe (occurred_at TIMESTAMPTZ NOT NULL)");
            try {
                var localTime = OffsetDateTime.parse("2026-10-16T09:30:00-06:00");
                try (var insert = connection.prepareStatement("INSERT INTO utc_probe VALUES (?)")) {
                    insert.setObject(1, localTime);
                    insert.executeUpdate();
                }
                try (var result = statement.executeQuery("SELECT occurred_at FROM utc_probe")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getObject(1, OffsetDateTime.class).toInstant())
                            .isEqualTo(localTime.toInstant());
                }
            } finally {
                statement.execute("DROP TABLE utc_probe");
            }
        }
    }
}
