package com.cecamed.backend.api.scheduling;

@org.springframework.boot.test.context.SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles({"prod", "postgres-test"})
@org.junit.jupiter.api.Tag("postgres")
class PostgresSchedulingFlowTest extends SchedulingFlowContract {
  @org.junit.jupiter.api.Test
  void databaseRejectsOverlapsEvenWhenApplicationIsBypassed() throws Exception {
    create(START, END);
    try (var c = dataSource.getConnection();
        var p =
            c.prepareStatement(
                "INSERT INTO"
                    + " cecamed.appointments(doctor_id,patient_id,start_time,end_time,reason_for_visit,created_at,updated_at)"
                    + " VALUES(?,?,?::timestamptz,?::timestamptz,'Bypass',now(),now())")) {
      p.setLong(1, doctorId);
      p.setLong(2, patientId);
      p.setString(3, "2027-01-04T14:15:00Z");
      p.setString(4, "2027-01-04T14:45:00Z");
      org.assertj.core.api.Assertions.assertThatThrownBy(p::executeUpdate)
          .isInstanceOf(java.sql.SQLException.class)
          .satisfies(
              ex ->
                  org.assertj.core.api.Assertions.assertThat(
                          ((java.sql.SQLException) ex).getSQLState())
                      .isEqualTo("23P01"));
    }
  }
}
