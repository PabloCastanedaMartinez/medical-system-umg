package com.cecamed.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CecamedBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CecamedBackendApplication.class, args);
    }
}
