package com.cecamed.backend.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class TimeConfigurationTest {

    @Test
    void injectableClockUsesUtcEvenWhenJvmUsesAnotherTimeZone() {
        var original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Guatemala"));
            try (var context = new AnnotationConfigApplicationContext(TimeConfiguration.class)) {
                var before = Instant.now();
                var clock = context.getBean(Clock.class);
                var instant = clock.instant();
                assertEquals(ZoneOffset.UTC, clock.getZone());
                assertFalse(instant.isBefore(before));
                assertFalse(instant.isAfter(Instant.now()));
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
