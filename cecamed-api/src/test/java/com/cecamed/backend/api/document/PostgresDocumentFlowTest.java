package com.cecamed.backend.api.document;
@org.springframework.boot.test.context.SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles({"prod","postgres-test"})
@org.junit.jupiter.api.Tag("postgres")
class PostgresDocumentFlowTest extends DocumentFlowContract {}
