package com.cecamed.backend.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    OpenAPI cecamedOpenApi(BuildProperties buildProperties) {
        return new OpenAPI().info(new Info()
                .title("CECAMed Backend API")
                .version(buildProperties.getVersion())
                .description("API REST para la migración web de CECAMed. "
                        + "Etapa inicial: infraestructura y contrato técnico."));
    }
}
