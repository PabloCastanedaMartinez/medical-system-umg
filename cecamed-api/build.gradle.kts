plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":cecamed-application"))
    implementation(project(":cecamed-infrastructure"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.flywaydb:flyway-core")
    testRuntimeOnly("com.h2database:h2")
}

springBoot {
    buildInfo {
        properties {
            name.set("cecamed-backend")
        }
    }
}

tasks.bootJar {
    archiveFileName.set("cecamed-backend.jar")
}

tasks.bootRun {
    workingDir = rootProject.projectDir
}

tasks.test {
    useJUnitPlatform { excludeTags("postgres") }
}

// Verificación explícita contra una base desechable; build no necesita PostgreSQL.
tasks.register<Test>("postgresTest") {
    description = "Verifica Flyway, PostgreSQL y UTC contra una base de pruebas aislada."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("postgres") }
    shouldRunAfter(tasks.test)
    // El estado de una base externa puede cambiar entre ejecuciones.
    outputs.upToDateWhen { false }
    doFirst {
        listOf("TEST_DB_URL", "TEST_DB_USERNAME", "TEST_DB_PASSWORD").forEach {
            require(!System.getenv(it).isNullOrBlank()) { "Define $it para una base PostgreSQL desechable." }
        }
    }
}
