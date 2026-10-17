package com.cecamed.backend.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TimeConfiguration {

    /** Inyectar este reloj para obtener instantes sin depender de la zona de la JVM. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
