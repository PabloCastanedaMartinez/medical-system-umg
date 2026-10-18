package com.cecamed.backend.application.scheduling;

import com.cecamed.backend.domain.scheduling.AppointmentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;

public final class SchedulingDtos {
  private SchedulingDtos() {}

  public record HoursRequest(
      @NotNull DayOfWeek dayOfWeek,
      @NotNull LocalTime startTime,
      @NotNull LocalTime endTime,
      @Min(5) @Max(240) int slotDurationMinutes) {}

  public record DoctorRequest(
      @NotBlank @Size(max = 150) String fullName,
      @NotBlank @Size(max = 150) String specialty,
      @NotBlank @Size(max = 80) String username,
      @Size(min = 12, max = 100) String password,
      @NotEmpty @Size(max = 28) List<@Valid HoursRequest> schedules,
      @PositiveOrZero Long version) {}

  public record DoctorResponse(
      long id, String fullName, String specialty, String username, boolean active, long version) {}

  public record ScheduleResponse(
      long id,
      long doctorId,
      String doctorName,
      DayOfWeek dayOfWeek,
      LocalTime startTime,
      LocalTime endTime,
      int slotDurationMinutes,
      boolean isActive) {}

  public record AppointmentRequest(
      @NotNull @Positive Long doctorId,
      @NotNull @Positive Long patientId,
      @NotNull Instant startTime,
      @NotNull Instant endTime,
      @NotBlank @Size(max = 2000) String reasonForVisit,
      @Size(max = 2000) String notes) {}

  public record RescheduleRequest(
      @NotNull Instant newStartTime,
      @NotNull Instant newEndTime,
      @NotBlank @Size(max = 500) String reason,
      @NotNull @PositiveOrZero Long version) {}

  public record StatusRequest(
      @NotNull AppointmentStatus status, @NotNull @PositiveOrZero Long version) {}

  public record CancelRequest(
      @NotBlank @Size(max = 500) String reason, @NotNull @PositiveOrZero Long version) {}

  public record AppointmentResponse(
      long id,
      long doctorId,
      String doctorName,
      String specialty,
      long patientId,
      String patientFullName,
      String patientPhone,
      Instant startTime,
      Instant endTime,
      AppointmentStatus status,
      String reasonForVisit,
      String cancellationReason,
      String notes,
      Instant checkedInAt,
      Instant createdAt,
      Instant updatedAt,
      long version) {}

  public record AvailableSlot(
      long doctorId, Instant startTime, Instant endTime, boolean available) {}

  public record BlockRequest(
      @NotNull @Positive Long doctorId,
      @NotBlank @Size(max = 150) String title,
      @NotNull Instant startDateTime,
      @NotNull Instant endDateTime,
      @NotBlank @Pattern(regexp = "VACACIONES|FERIADO|CAPACITACION|PERSONAL|MANTENIMIENTO|OTRO")
          String blockType,
      boolean allDay,
      @Size(max = 500) String reason) {}

  public record BlockResponse(
      long id,
      long doctorId,
      String doctorName,
      String title,
      Instant startDateTime,
      Instant endDateTime,
      String blockType,
      boolean allDay,
      String reason,
      Instant createdAt) {}

  public record DashboardSummary(
      long appointmentsToday,
      long waitingRoom,
      long completedToday,
      long pendingToday,
      long activePatients,
      List<AppointmentResponse> todayAppointments) {}

  public record PatientAccessRequest(
      @NotBlank @Size(min = 3, max = 80) String username,
      @NotBlank @Size(min = 12, max = 100) String password) {}
}
