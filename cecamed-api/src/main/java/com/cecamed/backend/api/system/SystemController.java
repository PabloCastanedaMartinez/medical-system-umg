package com.cecamed.backend.api.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "Sistema", description = "Información técnica de la API")
public class SystemController {

    private final BuildProperties buildProperties;

    public SystemController(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @GetMapping("/info")
    @Operation(summary = "Consultar el nombre y la versión del backend")
    public SystemInfoResponse info() {
        return new SystemInfoResponse(buildProperties.getName(), buildProperties.getVersion(), "v1");
    }

    public record SystemInfoResponse(String name, String version, String apiVersion) {}
}
