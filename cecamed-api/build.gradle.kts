plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":cecamed-application"))
    implementation(project(":cecamed-infrastructure"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")
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
