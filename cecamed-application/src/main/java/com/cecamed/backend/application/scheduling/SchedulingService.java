package com.cecamed.backend.application.scheduling;

import static com.cecamed.backend.application.scheduling.SchedulingDtos.*;

import com.cecamed.backend.application.patient.port.out.PatientRepositoryPort;
import com.cecamed.backend.application.shared.ResourceNotFoundException;
import com.cecamed.backend.domain.scheduling.*;
import com.cecamed.backend.domain.shared.BusinessRuleException;
import java.time.*;
import java.util.*;

/** Casos de uso independientes de Spring; el puerto delimita las transacciones. */
public class SchedulingService implements SchedulingUseCase {
  public static final ZoneId CLINIC_ZONE = ZoneId.of("America/Guatemala");
  private final SchedulingRepositoryPort repository;
  private final PatientRepositoryPort patients;
  private final Clock clock;

  public SchedulingService(
      SchedulingRepositoryPort repository, PatientRepositoryPort patients, Clock clock) {
    this.repository = repository;
    this.patients = patients;
    this.clock = clock;
  }

  public List<DoctorResponse> doctors() {
    return repository.doctors();
  }

  public List<ScheduleResponse> schedules(Long doctorId) {
    return repository.schedules(doctorId);
  }

  public List<BlockResponse> blocks(Long doctorId) {
    return repository.blocks(doctorId);
  }

  public long ownDoctorId(String username) {
    return repository
        .doctorIdForUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("La cuenta no tiene un médico asociado."));
  }

  public long ownPatientId(String username) {
    return repository
        .patientIdForUsername(username)
        .orElseThrow(
            () -> new ResourceNotFoundException("La cuenta no tiene un paciente asociado."));
  }

  public DoctorResponse saveDoctor(Long id, DoctorRequest request) {
    validateSchedules(request.schedules());
    if (id == null) return repository.transaction(() -> repository.saveDoctor(null, request));
    return repository.withDoctorLock(
        id,
        () -> {
          var existing = requireDoctor(id);
          requireVersion(request.version(), existing.version());
          // No modificar la jornada de reservas futuras sin resolverlas primero.
          var future =
              repository.appointments(clock.instant(), null, id, null, null).stream()
                  .filter(a -> a.status() != AppointmentStatus.CANCELADA)
                  .toList();
          for (var a : future) {
            if (!fits(request.schedules(), a.startTime(), a.endTime()))
              throw new BusinessRuleException(
                  "SCHEDULE_HAS_APPOINTMENTS",
                  "El horario deja citas futuras fuera de jornada. Reagéndelas primero.");
          }
          return repository.saveDoctor(id, request);
        });
  }

  private void validateSchedules(List<HoursRequest> hours) {
    for (int i = 0; i < hours.size(); i++) {
      var a = hours.get(i);
      if (!a.startTime().isBefore(a.endTime())
          || a.slotDurationMinutes() < 5
          || a.slotDurationMinutes() > 240
          || a.startTime().getSecond() != 0
          || a.startTime().getNano() != 0
          || a.endTime().getSecond() != 0
          || a.endTime().getNano() != 0
          || Duration.between(a.startTime(), a.endTime()).toMinutes() % a.slotDurationMinutes()
              != 0)
        throw new IllegalArgumentException(
            "La jornada debe contener bloques completos de 5 a 240 minutos.");
      for (int j = 0; j < i; j++) {
        var b = hours.get(j);
        if (a.dayOfWeek() == b.dayOfWeek()
            && a.startTime().isBefore(b.endTime())
            && a.endTime().isAfter(b.startTime()))
          throw new IllegalArgumentException("Los bloques de atención no pueden solaparse.");
      }
    }
  }

  private boolean fits(List<HoursRequest> schedules, Instant start, Instant end) {
    var s = start.atZone(CLINIC_ZONE);
    var e = end.atZone(CLINIC_ZONE);
    return s.toLocalDate().equals(e.toLocalDate())
        && schedules.stream()
            .anyMatch(
                h ->
                    h.dayOfWeek() == s.getDayOfWeek()
                        && !s.toLocalTime().isBefore(h.startTime())
                        && !e.toLocalTime().isAfter(h.endTime())
                        && Duration.between(start, end)
                            .equals(Duration.ofMinutes(h.slotDurationMinutes()))
                        && s.getSecond() == 0
                        && s.getNano() == 0
                        && Duration.between(h.startTime(), s.toLocalTime()).toMinutes()
                                % h.slotDurationMinutes()
                            == 0);
  }

  public List<AvailableSlot> availableSlots(long doctorId, LocalDate date, Long excludingId) {
    requireDoctor(doctorId);
    if (date.isBefore(LocalDate.now(clock.withZone(CLINIC_ZONE)))) return List.of();
    if (excludingId != null && requireAppointment(excludingId).doctorId() != doctorId)
      throw new IllegalArgumentException("La cita pertenece a otro médico.");
    var dayStart = date.atStartOfDay(CLINIC_ZONE).toInstant();
    var dayEnd = date.plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant();
    var booked =
        repository.appointments(dayStart, dayEnd, doctorId, null, null).stream()
            .filter(
                a ->
                    a.status() != AppointmentStatus.CANCELADA
                        && !Objects.equals(a.id(), excludingId))
            .map(a -> new TimeInterval(a.startTime(), a.endTime()))
            .toList();
    var blocked =
        repository.blocks(doctorId).stream()
            .map(b -> new TimeInterval(b.startDateTime(), b.endDateTime()))
            .toList();
    var slots = new ArrayList<AvailableSlot>();
    for (var h : repository.schedules(doctorId)) {
      if (!h.isActive() || h.dayOfWeek() != date.getDayOfWeek()) continue;
      var end = date.atTime(h.endTime()).atZone(CLINIC_ZONE).toInstant();
      for (var start = date.atTime(h.startTime()).atZone(CLINIC_ZONE).toInstant();
          !start.plusSeconds(h.slotDurationMinutes() * 60L).isAfter(end);
          start = start.plusSeconds(h.slotDurationMinutes() * 60L)) {
        var interval = new TimeInterval(start, start.plusSeconds(h.slotDurationMinutes() * 60L));
        if (start.isAfter(clock.instant())
            && booked.stream().noneMatch(interval::overlaps)
            && blocked.stream().noneMatch(interval::overlaps))
          slots.add(new AvailableSlot(doctorId, interval.start(), interval.end(), true));
      }
    }
    slots.sort(Comparator.comparing(AvailableSlot::startTime));
    return List.copyOf(slots);
  }

  public AppointmentResponse create(AppointmentRequest request, String actor) {
    return repository.withDoctorLock(
        request.doctorId(),
        () -> {
          var patient =
              patients
                  .findById(request.patientId())
                  .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado."));
          if (!patient.isActive())
            throw new BusinessRuleException("PATIENT_INACTIVE", "El paciente está inactivo.");
          validateBooking(request.doctorId(), request.startTime(), request.endTime(), null);
          var result = repository.createAppointment(request, clock.instant());
          repository.audit(result.id(), "CREATED", actor, "Solicitud pendiente", clock.instant());
          return result;
        });
  }

  public AppointmentResponse reschedule(long id, RescheduleRequest request, String actor) {
    long doctorId = requireAppointment(id).doctorId();
    return repository.withDoctorLock(
        doctorId,
        () -> {
          var existing = requireAppointment(id);
          requireVersion(request.version(), existing.version());
          if (!existing.status().canReschedule())
            throw new BusinessRuleException(
                "APPOINTMENT_CLOSED", "La cita no admite reprogramación.");
          validateBooking(doctorId, request.newStartTime(), request.newEndTime(), id);
          var result = repository.reschedule(id, request, clock.instant());
          repository.audit(
              id,
              "RESCHEDULED",
              actor,
              existing.startTime() + " → " + request.newStartTime() + ": " + request.reason(),
              clock.instant());
          return result;
        });
  }

  public AppointmentResponse status(
      long id, AppointmentStatus next, Long version, String reason, String actor) {
    long doctorId = requireAppointment(id).doctorId();
    return repository.withDoctorLock(
        doctorId,
        () -> {
          var current = requireAppointment(id);
          requireVersion(version, current.version());
          if (!current.status().canChangeTo(next))
            throw new BusinessRuleException(
                "INVALID_APPOINTMENT_TRANSITION", "Transición de estado no permitida.");
          if (next == AppointmentStatus.CANCELADA && (reason == null || reason.isBlank()))
            throw new IllegalArgumentException("Indique el motivo de cancelación.");
          var result = repository.changeStatus(id, next, reason, clock.instant());
          repository.audit(id, next.name(), actor, reason, clock.instant());
          return result;
        });
  }

  private void validateBooking(long doctorId, Instant start, Instant end, Long excludingId) {
    new TimeInterval(start, end);
    requireDoctor(doctorId);
    if (!start.isAfter(clock.instant()))
      throw new IllegalArgumentException("Seleccione un horario futuro.");
    var hours =
        repository.schedules(doctorId).stream()
            .filter(ScheduleResponse::isActive)
            .map(
                h ->
                    new HoursRequest(
                        h.dayOfWeek(), h.startTime(), h.endTime(), h.slotDurationMinutes()))
            .toList();
    if (!fits(hours, start, end))
      throw new IllegalArgumentException(
          "El horario está fuera de la jornada o no corresponde a un bloque completo.");
    var interval = new TimeInterval(start, end);
    if (repository.blocks(doctorId).stream()
        .anyMatch(b -> interval.overlaps(new TimeInterval(b.startDateTime(), b.endDateTime()))))
      throw new BusinessRuleException("SCHEDULE_BLOCKED", "El médico no atiende en ese horario.");
    if (repository.overlaps(doctorId, start, end, excludingId))
      throw new BusinessRuleException(
          "APPOINTMENT_SLOT_OCCUPIED",
          "El médico ya tiene una cita en ese horario. Seleccione otro.");
  }

  public BlockResponse createBlock(BlockRequest request) {
    return repository.withDoctorLock(
        request.doctorId(),
        () -> {
          requireDoctor(request.doctorId());
          new TimeInterval(request.startDateTime(), request.endDateTime());
          if (repository.overlaps(
              request.doctorId(), request.startDateTime(), request.endDateTime(), null))
            throw new BusinessRuleException(
                "BLOCK_HAS_APPOINTMENTS",
                "Reagende o cancele las citas existentes antes de bloquear el horario.");
          return repository.createBlock(request, clock.instant());
        });
  }

  public void deleteBlock(long id) {
    var b =
        repository
            .block(id)
            .orElseThrow(() -> new ResourceNotFoundException("Bloqueo no encontrado."));
    repository.withDoctorLock(
        b.doctorId(),
        () -> {
          repository.deleteBlock(id);
          return null;
        });
  }

  public List<AppointmentResponse> list(
      Instant start, Instant end, Long doctorId, Long patientId, AppointmentStatus status) {
    if (start != null && end != null && !start.isBefore(end))
      throw new IllegalArgumentException("Rango de fechas inválido.");
    if (patientId != null && patients.findById(patientId).isEmpty())
      throw new ResourceNotFoundException("Paciente no encontrado.");
    return repository.appointments(start, end, doctorId, patientId, status);
  }

  public List<AppointmentResponse> today(long doctorId) {
    var day = LocalDate.now(clock.withZone(CLINIC_ZONE));
    return list(
        day.atStartOfDay(CLINIC_ZONE).toInstant(),
        day.plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant(),
        doctorId,
        null,
        null);
  }

  public DashboardSummary dashboard(Long doctorId) {
    var day = LocalDate.now(clock.withZone(CLINIC_ZONE));
    var all =
        list(
            day.atStartOfDay(CLINIC_ZONE).toInstant(),
            day.plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant(),
            doctorId,
            null,
            null);
    return new DashboardSummary(
        all.size(),
        all.stream().filter(a -> a.status() == AppointmentStatus.EN_SALA).count(),
        all.stream().filter(a -> a.status() == AppointmentStatus.ATENDIDA).count(),
        all.stream().filter(a -> a.status() == AppointmentStatus.PENDIENTE).count(),
        repository.activePatientCount(),
        all);
  }

  public AppointmentResponse requireAppointment(long id) {
    return repository
        .appointment(id)
        .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada."));
  }

  private DoctorResponse requireDoctor(long id) {
    return repository
        .doctor(id)
        .filter(DoctorResponse::active)
        .orElseThrow(() -> new ResourceNotFoundException("Médico no encontrado o inactivo."));
  }

  private void requireVersion(Long supplied, long current) {
    if (supplied == null || supplied != current)
      throw new BusinessRuleException(
          "STALE_VERSION", "La cita o el médico cambió. Actualice antes de guardar.");
  }
}
