package com.cecamed.backend.infrastructure.persistence.scheduling;

import static com.cecamed.backend.application.scheduling.SchedulingDtos.*;

import com.cecamed.backend.application.scheduling.SchedulingRepositoryPort;
import com.cecamed.backend.application.shared.ResourceNotFoundException;
import com.cecamed.backend.domain.scheduling.AppointmentStatus;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Proyecciones con JOIN explícito: una consulta por listado, sin cargas N+1. */
@Component
public class SchedulingJdbcAdapter implements SchedulingRepositoryPort {
  private final NamedParameterJdbcTemplate jdbc;
  private final TransactionTemplate tx;

  public SchedulingJdbcAdapter(
      NamedParameterJdbcTemplate jdbc, PlatformTransactionManager manager) {
    this.jdbc = jdbc;
    this.tx = new TransactionTemplate(manager);
  }

  private static Object time(Instant instant) {
    return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
  }

  private static Instant instant(ResultSet rs, String name) throws SQLException {
    var v = rs.getObject(name, OffsetDateTime.class);
    return v == null ? null : v.toInstant();
  }

  private static final String DOCTORS =
      "SELECT d.*,u.username,u.active AS account_active,u.role AS account_role FROM cecamed.doctors"
          + " d JOIN cecamed.user_accounts u ON u.id=d.user_id";
  private static final RowMapper<DoctorResponse> DOCTOR =
      (r, n) ->
          new DoctorResponse(
              r.getLong("id"),
              r.getString("full_name"),
              r.getString("specialty"),
              r.getString("username"),
              r.getBoolean("active")
                  && r.getBoolean("account_active")
                  && "MEDICO".equals(r.getString("account_role")),
              r.getLong("version"));
  private static final String APPOINTMENTS =
      """
      SELECT a.*,d.full_name AS doctor_name,d.specialty,p.full_name AS patient_name,p.phone AS patient_phone
      FROM cecamed.appointments a JOIN cecamed.doctors d ON d.id=a.doctor_id
      JOIN cecamed.patients p ON p.id=a.patient_id
      """;
  private static final RowMapper<AppointmentResponse> APPOINTMENT =
      (r, n) ->
          new AppointmentResponse(
              r.getLong("id"),
              r.getLong("doctor_id"),
              r.getString("doctor_name"),
              r.getString("specialty"),
              r.getLong("patient_id"),
              r.getString("patient_name"),
              r.getString("patient_phone"),
              instant(r, "start_time"),
              instant(r, "end_time"),
              AppointmentStatus.valueOf(r.getString("status")),
              r.getString("reason_for_visit"),
              r.getString("cancellation_reason"),
              r.getString("notes"),
              instant(r, "checked_in_at"),
              instant(r, "created_at"),
              instant(r, "updated_at"),
              r.getLong("version"));
  private static final String BLOCKS =
      "SELECT b.*,d.full_name AS doctor_name FROM cecamed.schedule_blocks b JOIN cecamed.doctors d"
          + " ON d.id=b.doctor_id";
  private static final RowMapper<BlockResponse> BLOCK =
      (r, n) ->
          new BlockResponse(
              r.getLong("id"),
              r.getLong("doctor_id"),
              r.getString("doctor_name"),
              r.getString("title"),
              instant(r, "start_date_time"),
              instant(r, "end_date_time"),
              r.getString("block_type"),
              r.getBoolean("all_day"),
              r.getString("reason"),
              instant(r, "created_at"));

  @Override
  public <T> T transaction(Supplier<T> work) {
    return tx.execute(status -> work.get());
  }

  @Override
  public <T> T withDoctorLock(long doctorId, Supplier<T> work) {
    return transaction(
        () -> {
          var ids =
              jdbc.queryForList(
                  "SELECT id FROM cecamed.doctors WHERE id=:id FOR UPDATE",
                  Map.of("id", doctorId),
                  Long.class);
          if (ids.isEmpty()) throw new ResourceNotFoundException("Médico no encontrado.");
          return work.get();
        });
  }

  private long insert(String sql, MapSqlParameterSource params) {
    var key = new GeneratedKeyHolder();
    jdbc.update(sql, params, key, new String[] {"id"});
    return Objects.requireNonNull(key.getKey()).longValue();
  }

  @Override
  public List<DoctorResponse> doctors() {
    return jdbc.query(
        DOCTORS + " WHERE d.active=TRUE AND u.active=TRUE AND u.role='MEDICO' ORDER BY d.full_name",
        DOCTOR);
  }

  @Override
  public Optional<DoctorResponse> doctor(long id) {
    return jdbc.query(DOCTORS + " WHERE d.id=:id", Map.of("id", id), DOCTOR).stream().findFirst();
  }

  @Override
  public Optional<Long> doctorIdForUsername(String username) {
    return jdbc
        .queryForList(
            "SELECT d.id FROM cecamed.doctors d JOIN cecamed.user_accounts u ON u.id=d.user_id"
                + " WHERE u.username=:u AND u.role='MEDICO' AND u.active=TRUE AND d.active=TRUE",
            Map.of("u", username),
            Long.class)
        .stream()
        .findFirst();
  }

  @Override
  public Optional<Long> patientIdForUsername(String username) {
    return jdbc
        .queryForList(
            "SELECT p.patient_id FROM cecamed.patient_portal_accounts p JOIN cecamed.user_accounts"
                + " u ON u.id=p.user_id WHERE u.username=:u AND u.role='PACIENTE' AND"
                + " u.active=TRUE",
            Map.of("u", username),
            Long.class)
        .stream()
        .findFirst();
  }

  @Override
  public DoctorResponse saveDoctor(Long id, DoctorRequest request) {
    var users =
        jdbc.queryForList(
            "SELECT id FROM cecamed.user_accounts WHERE username=:u AND role='MEDICO' AND"
                + " active=TRUE",
            Map.of("u", request.username().trim().toLowerCase(Locale.ROOT)),
            Long.class);
    if (users.isEmpty())
      throw new IllegalArgumentException("Seleccione una cuenta activa de médico.");
    var params =
        new MapSqlParameterSource()
            .addValue("u", users.getFirst())
            .addValue("name", request.fullName().trim())
            .addValue("s", request.specialty().trim());
    if (id == null)
      id =
          insert(
              "INSERT INTO cecamed.doctors(user_id,full_name,specialty) VALUES(:u,:name,:s)",
              params);
    else {
      params.addValue("id", id);
      jdbc.update(
          "UPDATE cecamed.doctors SET user_id=:u,full_name=:name,specialty=:s,version=version+1"
              + " WHERE id=:id",
          params);
      jdbc.update("DELETE FROM cecamed.doctor_availability WHERE doctor_id=:id", Map.of("id", id));
    }
    for (var h : request.schedules())
      jdbc.update(
          "INSERT INTO"
              + " cecamed.doctor_availability(doctor_id,day_of_week,start_time,end_time,slot_duration_minutes)"
              + " VALUES(:id,:day,:start,:end,:minutes)",
          new MapSqlParameterSource()
              .addValue("id", id)
              .addValue("day", h.dayOfWeek().name())
              .addValue("start", h.startTime())
              .addValue("end", h.endTime())
              .addValue("minutes", h.slotDurationMinutes()));
    return doctor(id).orElseThrow();
  }

  @Override
  public List<ScheduleResponse> schedules(Long doctorId) {
    return jdbc.query(
        "SELECT h.*,d.full_name,d.active FROM cecamed.doctor_availability h JOIN cecamed.doctors d"
            + " ON d.id=h.doctor_id"
            + (doctorId == null ? "" : " WHERE d.id=:id")
            + " ORDER BY h.doctor_id,h.day_of_week,h.start_time",
        doctorId == null ? Map.of() : Map.of("id", doctorId),
        (r, n) ->
            new ScheduleResponse(
                r.getLong("id"),
                r.getLong("doctor_id"),
                r.getString("full_name"),
                DayOfWeek.valueOf(r.getString("day_of_week")),
                r.getObject("start_time", LocalTime.class),
                r.getObject("end_time", LocalTime.class),
                r.getInt("slot_duration_minutes"),
                r.getBoolean("active")));
  }

  @Override
  public List<BlockResponse> blocks(Long doctorId) {
    return jdbc.query(
        BLOCKS + (doctorId == null ? "" : " WHERE b.doctor_id=:id") + " ORDER BY b.start_date_time",
        doctorId == null ? Map.of() : Map.of("id", doctorId),
        BLOCK);
  }

  @Override
  public Optional<BlockResponse> block(long id) {
    return jdbc.query(BLOCKS + " WHERE b.id=:id", Map.of("id", id), BLOCK).stream().findFirst();
  }

  @Override
  public BlockResponse createBlock(BlockRequest r, Instant now) {
    long id =
        insert(
            "INSERT INTO"
                + " cecamed.schedule_blocks(doctor_id,title,start_date_time,end_date_time,block_type,all_day,reason,created_at)"
                + " VALUES(:d,:t,:s,:e,:type,:all,:r,:now)",
            new MapSqlParameterSource()
                .addValue("d", r.doctorId())
                .addValue("t", r.title().trim())
                .addValue("s", time(r.startDateTime()))
                .addValue("e", time(r.endDateTime()))
                .addValue("type", r.blockType())
                .addValue("all", r.allDay())
                .addValue("r", r.reason())
                .addValue("now", time(now)));
    return block(id).orElseThrow();
  }

  @Override
  public void deleteBlock(long id) {
    jdbc.update("DELETE FROM cecamed.schedule_blocks WHERE id=:id", Map.of("id", id));
  }

  @Override
  public Optional<AppointmentResponse> appointment(long id) {
    return jdbc.query(APPOINTMENTS + " WHERE a.id=:id", Map.of("id", id), APPOINTMENT).stream()
        .findFirst();
  }

  @Override
  public List<AppointmentResponse> appointments(
      Instant start, Instant end, Long doctorId, Long patientId, AppointmentStatus status) {
    var sql = new StringBuilder(APPOINTMENTS + " WHERE 1=1");
    var p = new MapSqlParameterSource();
    if (start != null) {
      sql.append(" AND a.end_time>:start");
      p.addValue("start", time(start));
    }
    if (end != null) {
      sql.append(" AND a.start_time<:end");
      p.addValue("end", time(end));
    }
    if (doctorId != null) {
      sql.append(" AND a.doctor_id=:d");
      p.addValue("d", doctorId);
    }
    if (patientId != null) {
      sql.append(" AND a.patient_id=:p");
      p.addValue("p", patientId);
    }
    if (status != null) {
      sql.append(" AND a.status=:status");
      p.addValue("status", status.name());
    }
    sql.append(" ORDER BY a.start_time,a.id");
    return jdbc.query(sql.toString(), p, APPOINTMENT);
  }

  @Override
  public boolean overlaps(long doctorId, Instant start, Instant end, Long excludingId) {
    var p =
        new MapSqlParameterSource()
            .addValue("d", doctorId)
            .addValue("s", time(start))
            .addValue("e", time(end));
    String sql =
        "SELECT COUNT(*) FROM cecamed.appointments WHERE doctor_id=:d AND status<>'CANCELADA' AND"
            + " start_time<:e AND end_time>:s";
    if (excludingId != null) {
      sql += " AND id<>:ignore";
      p.addValue("ignore", excludingId);
    }
    return jdbc.queryForObject(sql, p, Long.class) > 0;
  }

  @Override
  public AppointmentResponse createAppointment(AppointmentRequest r, Instant now) {
    long id =
        insert(
            "INSERT INTO"
                + " cecamed.appointments(doctor_id,patient_id,start_time,end_time,status,reason_for_visit,notes,created_at,updated_at)"
                + " VALUES(:d,:p,:s,:e,'PENDIENTE',:r,:n,:now,:now)",
            new MapSqlParameterSource()
                .addValue("d", r.doctorId())
                .addValue("p", r.patientId())
                .addValue("s", time(r.startTime()))
                .addValue("e", time(r.endTime()))
                .addValue("r", r.reasonForVisit().trim())
                .addValue("n", r.notes())
                .addValue("now", time(now)));
    return appointment(id).orElseThrow();
  }

  @Override
  public AppointmentResponse reschedule(long id, RescheduleRequest r, Instant now) {
    jdbc.update(
        "UPDATE cecamed.appointments SET"
            + " start_time=:s,end_time=:e,status='PENDIENTE',checked_in_at=NULL,updated_at=:now,version=version+1"
            + " WHERE id=:id",
        new MapSqlParameterSource()
            .addValue("s", time(r.newStartTime()))
            .addValue("e", time(r.newEndTime()))
            .addValue("now", time(now))
            .addValue("id", id));
    return appointment(id).orElseThrow();
  }

  @Override
  public AppointmentResponse changeStatus(
      long id, AppointmentStatus status, String reason, Instant now) {
    jdbc.update(
        "UPDATE cecamed.appointments SET status=:status,cancellation_reason=:r,checked_in_at=CASE"
            + " WHEN :status='EN_SALA' THEN :now ELSE checked_in_at"
            + " END,updated_at=:now,version=version+1 WHERE id=:id",
        new MapSqlParameterSource()
            .addValue("status", status.name())
            .addValue("r", status == AppointmentStatus.CANCELADA ? reason : null)
            .addValue("now", time(now))
            .addValue("id", id));
    return appointment(id).orElseThrow();
  }

  @Override
  public void audit(long appointmentId, String action, String actor, String details, Instant now) {
    jdbc.update(
        "INSERT INTO cecamed.appointment_audit(appointment_id,action,actor,details,occurred_at)"
            + " VALUES(:id,:a,:u,:d,:now)",
        new MapSqlParameterSource()
            .addValue("id", appointmentId)
            .addValue("a", action)
            .addValue("u", actor)
            .addValue("d", details)
            .addValue("now", time(now)));
  }

  @Override
  public void linkPatient(long patientId, long userId) {
    jdbc.update(
        "INSERT INTO cecamed.patient_portal_accounts(patient_id,user_id) VALUES(:p,:u)",
        Map.of("p", patientId, "u", userId));
  }

  @Override
  public long activePatientCount() {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM cecamed.patients WHERE active=TRUE", Map.of(), Long.class);
  }
}
