package com.cecamed.backend.api.scheduling;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cecamed.backend.application.identity.model.CreateUserCommand;
import com.cecamed.backend.application.identity.port.in.UserManagementUseCase;
import com.cecamed.backend.domain.identity.Role;
import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Import(SchedulingFlowContract.FixedClock.class)
abstract class SchedulingFlowContract {
  @TestConfiguration
  static class FixedClock {
    @Bean
    @Primary
    Clock schedulingClock() {
      return Clock.fixed(Instant.parse("2027-01-04T12:00:00Z"), ZoneOffset.UTC);
    }
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UserManagementUseCase users;
  @Autowired javax.sql.DataSource dataSource;
  long doctorId, patientId;
  String doctorUsername;
  static final String PASSWORD = "Schedule-Testing-482!";
  static final String START = "2027-01-04T14:00:00Z";
  static final String END = "2027-01-04T14:30:00Z";

  @BeforeEach
  void prepare() throws Exception {
    doctorUsername = "doctor" + UUID.randomUUID().toString().replace("-", "");
    users.createUser(
        new CreateUserCommand(
            doctorUsername, PASSWORD, "Doctora Prueba", Role.MEDICO, "test", null, null));
    doctorId = call(post("/api/v1/doctors"), doctorBody(doctorUsername), 201).get("id").asLong();
    patientId =
        call(
                post("/api/v1/patients"),
                """
                {"firstName":"Ana","lastName":"Paciente","birthDate":"2000-01-02","gender":"FEMENINO",
                 "bloodType":"DESCONOCIDO","phone":"55551234","allergies":"Penicilina"}
                """,
                201)
            .get("id")
            .asLong();
  }

  String doctorBody(String username) throws Exception {
    return json.writeValueAsString(
        Map.of(
            "fullName",
            "Doctora Prueba",
            "specialty",
            "Medicina general",
            "username",
            username,
            "schedules",
            List.of(
                Map.of(
                    "dayOfWeek",
                    "MONDAY",
                    "startTime",
                    "08:00",
                    "endTime",
                    "12:00",
                    "slotDurationMinutes",
                    30))));
  }

  JsonNode call(MockHttpServletRequestBuilder request, String body, int expected) throws Exception {
    request.with(user("reception-test").roles("RECEPCION"));
    if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
    var response =
        mvc.perform(request)
            .andExpect(status().is(expected))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return response.isBlank() ? null : json.readTree(response);
  }

  String booking(long doctor, String start, String end) throws Exception {
    return json.writeValueAsString(
        Map.of(
            "doctorId",
            doctor,
            "patientId",
            patientId,
            "startTime",
            start,
            "endTime",
            end,
            "reasonForVisit",
            "Control"));
  }

  JsonNode create(String start, String end) throws Exception {
    return call(post("/api/v1/appointments"), booking(doctorId, start, end), 201);
  }

  MockHttpServletRequestBuilder slots() {
    return get("/api/v1/appointments/available-slots")
        .param("doctorId", Long.toString(doctorId))
        .param("date", "2027-01-04");
  }

  @Test
  void availabilityPendingConfirmationCancellationAndHistory() throws Exception {
    assertThat(call(slots(), null, 200).size()).isEqualTo(8);
    var a = create(START, END);
    long id = a.get("id").asLong();
    assertThat(a.get("status").asText()).isEqualTo("PENDIENTE");
    assertThat(a.get("patientFullName").asText()).isEqualTo("Ana Paciente");
    assertThat(call(slots(), null, 200).size()).isEqualTo(7);
    assertThat(
            call(post("/api/v1/appointments"), booking(doctorId, START, END), 409)
                .get("code")
                .asText())
        .isEqualTo("APPOINTMENT_SLOT_OCCUPIED");
    call(
        patch("/api/v1/appointments/" + id + "/status"),
        "{\"status\":\"CONFIRMADA\",\"version\":0}",
        200);
    call(
        post("/api/v1/appointments/" + id + "/cancel"),
        "{\"reason\":\"No puede asistir\",\"version\":1}",
        200);
    assertThat(call(slots(), null, 200).size()).isEqualTo(8);
    create(START, END);
    call(
        patch("/api/v1/appointments/" + id + "/status"),
        "{\"status\":\"CONFIRMADA\",\"version\":2}",
        409);
    var history = call(get("/api/v1/patients/" + patientId + "/appointments"), null, 200);
    assertThat(history.size()).isEqualTo(2);
    assertThat(history.get(0).get("doctorName").asText()).isEqualTo("Doctora Prueba");
    mvc.perform(get("/api/v1/doctors/me/agenda").with(user(doctorUsername).roles("MEDICO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].reasonForVisit").value("Control"));
  }

  @Test
  void rescheduleChecksAvailabilityVersionAndKeepsOriginalOnConflict() throws Exception {
    var first = create(START, END);
    var other = create("2027-01-04T14:30:00Z", "2027-01-04T15:00:00Z");
    long id = other.get("id").asLong();
    String collision =
        json.writeValueAsString(
            Map.of("newStartTime", START, "newEndTime", END, "reason", "Cambio", "version", 0));
    call(put("/api/v1/appointments/" + id + "/reschedule"), collision, 409);
    var history = call(get("/api/v1/patients/" + patientId + "/appointments"), null, 200);
    assertThat(history.get(1).get("startTime").asText()).isEqualTo("2027-01-04T14:30:00Z");
    String body =
        json.writeValueAsString(
            Map.of(
                "newStartTime",
                "2027-01-04T15:00:00Z",
                "newEndTime",
                "2027-01-04T15:30:00Z",
                "reason",
                "Cambio",
                "version",
                0));
    assertThat(
            call(put("/api/v1/appointments/" + id + "/reschedule"), body, 200)
                .get("status")
                .asText())
        .isEqualTo("PENDIENTE");
    call(put("/api/v1/appointments/" + id + "/reschedule"), body, 409);
    call(
        post("/api/v1/appointments"),
        booking(doctorId, "2027-01-04T14:30:00Z", "2027-01-04T15:00:00Z"),
        201);
    call(
        post("/api/v1/appointments"),
        booking(doctorId, "2027-01-04T14:15:00Z", "2027-01-04T14:45:00Z"),
        400);
    call(
        post("/api/v1/appointments"),
        booking(doctorId, "2027-01-04T12:00:00Z", "2027-01-04T12:30:00Z"),
        400);
    call(post("/api/v1/appointments"), booking(doctorId, END, START), 400);
  }

  @Test
  void simultaneousReservationsHaveOneWinnerAndDoctorsAreIndependent() throws Exception {
    var gate = new CountDownLatch(1);
    String body = booking(doctorId, START, END);
    try (var pool = Executors.newFixedThreadPool(2)) {
      Callable<Integer> reserve =
          () -> {
            gate.await();
            return mvc.perform(
                    post("/api/v1/appointments")
                        .with(user("reception-test").roles("RECEPCION"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getStatus();
          };
      var first = pool.submit(reserve);
      var second = pool.submit(reserve);
      gate.countDown();
      assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(201, 409);
    }
    String other = "other" + UUID.randomUUID().toString().replace("-", "");
    users.createUser(
        new CreateUserCommand(other, PASSWORD, "Otro médico", Role.MEDICO, "test", null, null));
    long otherId = call(post("/api/v1/doctors"), doctorBody(other), 201).get("id").asLong();
    call(post("/api/v1/appointments"), booking(otherId, START, END), 201);
    mvc.perform(
            get("/api/v1/appointments")
                .param("start", "2027-01-04T00:00:00Z")
                .param("end", "2027-01-05T00:00:00Z")
                .param("doctorId", Long.toString(otherId))
                .with(user(doctorUsername).roles("MEDICO")))
        .andExpect(status().isForbidden());
  }

  @Test
  void blocksAreDoctorSpecificAndCannotCoverReservations() throws Exception {
    create(START, END);
    String body =
        json.writeValueAsString(
            Map.of(
                "doctorId",
                doctorId,
                "title",
                "Descanso",
                "startDateTime",
                START,
                "endDateTime",
                END,
                "blockType",
                "PERSONAL"));
    call(post("/api/v1/schedule-blocks"), body, 409);
    body =
        json.writeValueAsString(
            Map.of(
                "doctorId",
                doctorId,
                "title",
                "Descanso",
                "startDateTime",
                "2027-01-04T15:00:00Z",
                "endDateTime",
                "2027-01-04T15:30:00Z",
                "blockType",
                "PERSONAL"));
    var block = call(post("/api/v1/schedule-blocks"), body, 201);
    assertThat(call(slots(), null, 200).size()).isEqualTo(6);
    call(
        post("/api/v1/appointments"),
        booking(doctorId, "2027-01-04T15:00:00Z", "2027-01-04T15:30:00Z"),
        409);
    call(delete("/api/v1/schedule-blocks/" + block.get("id").asLong()), null, 204);
    assertThat(call(slots(), null, 200).size()).isEqualTo(7);
  }

  @Test
  void patientPortalUsesAuthenticatedPatientAndDoesNotExposeOthers() throws Exception {
    String username = "patient" + UUID.randomUUID().toString().replace("-", "");
    call(
        post("/api/v1/patients/" + patientId + "/portal-access"),
        json.writeValueAsString(Map.of("username", username, "password", PASSWORD)),
        201);
    var login =
        call(
            post("/api/v1/auth/login"),
            json.writeValueAsString(Map.of("username", username, "password", PASSWORD)),
            200);
    String bearer = "Bearer " + login.get("accessToken").asText();
    mvc.perform(
            post("/api/v1/portal/appointments")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(booking(doctorId, START, END)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.patientId").value(patientId))
        .andExpect(jsonPath("$.status").value("PENDIENTE"));
    mvc.perform(get("/api/v1/portal/appointments").header("Authorization", bearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    mvc.perform(
            get("/api/v1/patients/" + patientId + "/appointments").header("Authorization", bearer))
        .andExpect(status().isForbidden());
    mvc.perform(
            patch("/api/v1/appointments/1/status")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CONFIRMADA\",\"version\":0}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/v1/appointments/available-slots")
                .param("date", "2027-01-04")
                .param("doctorId", Long.toString(doctorId))
                .param("excludingId", "1")
                .header("Authorization", bearer))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/appointments")
                .with(user(doctorUsername).roles("MEDICO"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(booking(doctorId, START, END)))
        .andExpect(status().isForbidden());
  }

  @Test
  void patientCreationRequiresContactAndAllergyDeclarationAndPersistsBoth() throws Exception {
    String invalid =
        "{\"firstName\":\"Ana\",\"lastName\":\"Test\",\"birthDate\":\"2000-01-02\",\"gender\":\"FEMENINO\",\"bloodType\":\"DESCONOCIDO\"}";
    call(post("/api/v1/patients"), invalid, 400);
    call(
        post("/api/v1/patients"),
        json.writeValueAsString(
            ((com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(invalid))
                .put("phone", "5555")),
        400);
    try (var connection = dataSource.getConnection();
        var q =
            connection.prepareStatement(
                "SELECT allergies FROM cecamed.medical_records WHERE patient_id=?")) {
      q.setLong(1, patientId);
      try (var r = q.executeQuery()) {
        assertThat(r.next()).isTrue();
        assertThat(r.getString(1)).isEqualTo("Penicilina");
      }
    }
  }
}
