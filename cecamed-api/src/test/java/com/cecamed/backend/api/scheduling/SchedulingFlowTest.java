package com.cecamed.backend.api.scheduling;

@org.springframework.boot.test.context.SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:scheduling;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE"
            + " SCHEMA IF NOT EXISTS cecamed")
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
class SchedulingFlowTest extends SchedulingFlowContract {}
