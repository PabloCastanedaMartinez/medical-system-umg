package com.cecamed.backend.api.scheduling;

import static com.cecamed.backend.application.scheduling.SchedulingDtos.*;

import com.cecamed.backend.application.patient.model.PatientResponse;
import com.cecamed.backend.application.patient.port.in.PatientManagementUseCase;
import com.cecamed.backend.application.scheduling.*;
import com.cecamed.backend.domain.scheduling.AppointmentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.time.*;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class SchedulingController {
  private final SchedulingUseCase service;
  private final SchedulingAdministrationUseCase administration;
  private final PatientManagementUseCase patients;

  public SchedulingController(
      SchedulingUseCase service,
      SchedulingAdministrationUseCase administration,
      PatientManagementUseCase patients) {
    this.service = service;
    this.administration = administration;
    this.patients = patients;
  }

  private boolean isDoctor(Authentication auth) {
    return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MEDICO"));
  }

  private Long filterDoctor(Authentication auth, Long requested) {
    if (!isDoctor(auth)) return requested;
    long own = service.ownDoctorId(auth.getName());
    if (requested != null && requested != own)
      throw new AccessDeniedException("Solo puede consultar su agenda.");
    return own;
  }

  @GetMapping("/doctors")
  public List<DoctorResponse> doctors() {
    return service.doctors();
  }

  @PostMapping("/doctors")
  public ResponseEntity<DoctorResponse> createDoctor(
      @Valid @RequestBody DoctorRequest request, Authentication auth) {
    var doctor = administration.registerDoctor(request, auth.getName());
    return ResponseEntity.created(URI.create("/api/v1/doctors/" + doctor.id())).body(doctor);
  }

  @PutMapping("/doctors/{id}")
  public DoctorResponse updateDoctor(
      @PathVariable long id, @Valid @RequestBody DoctorRequest request) {
    return service.saveDoctor(id, request);
  }

  @GetMapping("/schedules")
  public List<ScheduleResponse> schedules(@RequestParam(required = false) Long doctorId) {
    return service.schedules(doctorId);
  }

  @GetMapping("/schedule-blocks")
  public List<BlockResponse> blocks(@RequestParam(required = false) Long doctorId) {
    return service.blocks(doctorId);
  }

  @PostMapping("/schedule-blocks")
  public ResponseEntity<BlockResponse> block(@Valid @RequestBody BlockRequest request) {
    return ResponseEntity.status(201).body(service.createBlock(request));
  }

  @DeleteMapping("/schedule-blocks/{id}")
  public ResponseEntity<Void> deleteBlock(@PathVariable long id) {
    service.deleteBlock(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/appointments/available-slots")
  public List<AvailableSlot> available(
      @RequestParam long doctorId,
      @RequestParam LocalDate date,
      @RequestParam(required = false) Long excludingId,
      Authentication auth) {
    // Excluir una reserva solo está permitido al personal que la puede reagendar.
    if (excludingId != null
        && auth.getAuthorities().stream()
            .noneMatch(a -> List.of("ROLE_ADMIN", "ROLE_RECEPCION").contains(a.getAuthority())))
      throw new AccessDeniedException("No puede excluir reservas.");
    return service.availableSlots(doctorId, date, excludingId);
  }

  @GetMapping("/appointments")
  public List<AppointmentResponse> list(
      @RequestParam Instant start,
      @RequestParam Instant end,
      @RequestParam(required = false) Long doctorId,
      @RequestParam(required = false) AppointmentStatus status,
      Authentication auth) {
    return service.list(start, end, filterDoctor(auth, doctorId), null, status);
  }

  @GetMapping("/doctors/me/agenda")
  public List<AppointmentResponse> agenda(Authentication auth) {
    return service.today(service.ownDoctorId(auth.getName()));
  }

  @GetMapping("/patients/{id}/appointments")
  public List<AppointmentResponse> history(@PathVariable long id) {
    return service.list(null, null, null, id, null);
  }

  @PostMapping("/appointments")
  public ResponseEntity<AppointmentResponse> create(
      @Valid @RequestBody AppointmentRequest request, Authentication auth) {
    var a = service.create(request, auth.getName());
    return ResponseEntity.created(URI.create("/api/v1/appointments/" + a.id())).body(a);
  }

  @PutMapping("/appointments/{id}/reschedule")
  public AppointmentResponse reschedule(
      @PathVariable long id, @Valid @RequestBody RescheduleRequest request, Authentication auth) {
    return service.reschedule(id, request, auth.getName());
  }

  @PatchMapping("/appointments/{id}/status")
  public AppointmentResponse status(
      @PathVariable long id, @Valid @RequestBody StatusRequest request, Authentication auth) {
    return service.status(id, request.status(), request.version(), null, auth.getName());
  }

  @PostMapping("/appointments/{id}/cancel")
  public AppointmentResponse cancel(
      @PathVariable long id, @Valid @RequestBody CancelRequest request, Authentication auth) {
    return service.status(
        id, AppointmentStatus.CANCELADA, request.version(), request.reason(), auth.getName());
  }

  @GetMapping("/dashboard/summary")
  public DashboardSummary dashboard(Authentication auth) {
    return service.dashboard(filterDoctor(auth, null));
  }

  @PostMapping("/patients/{id}/portal-access")
  public ResponseEntity<Void> portalAccess(
      @PathVariable long id,
      @Valid @RequestBody PatientAccessRequest request,
      Authentication auth) {
    administration.grantPatientAccess(id, request, auth.getName());
    return ResponseEntity.status(201).build();
  }

  @GetMapping("/portal/me")
  public PatientResponse profile(Authentication auth) {
    return patients.getPatient(service.ownPatientId(auth.getName()));
  }

  @GetMapping("/portal/appointments")
  public List<AppointmentResponse> ownHistory(Authentication auth) {
    return service.list(null, null, null, service.ownPatientId(auth.getName()), null);
  }

  public record PortalAppointmentRequest(
      @NotNull @Positive Long doctorId,
      @NotNull Instant startTime,
      @NotNull Instant endTime,
      @NotBlank @Size(max = 2000) String reasonForVisit) {}

  @PostMapping("/portal/appointments")
  public ResponseEntity<AppointmentResponse> request(
      @Valid @RequestBody PortalAppointmentRequest r, Authentication auth) {
    long patientId = service.ownPatientId(auth.getName());
    return ResponseEntity.status(201)
        .body(
            service.create(
                new AppointmentRequest(
                    r.doctorId(), patientId, r.startTime(), r.endTime(), r.reasonForVisit(), null),
                auth.getName()));
  }
}
