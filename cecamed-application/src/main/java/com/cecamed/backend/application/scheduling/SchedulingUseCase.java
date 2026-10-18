package com.cecamed.backend.application.scheduling;

import static com.cecamed.backend.application.scheduling.SchedulingDtos.*;

import com.cecamed.backend.domain.scheduling.AppointmentStatus;
import java.time.*;
import java.util.List;

public interface SchedulingUseCase {
  List<DoctorResponse> doctors();

  List<ScheduleResponse> schedules(Long doctorId);

  List<BlockResponse> blocks(Long doctorId);

  long ownDoctorId(String username);

  long ownPatientId(String username);

  DoctorResponse saveDoctor(Long id, DoctorRequest request);

  List<AvailableSlot> availableSlots(long doctorId, LocalDate date, Long excludingId);

  AppointmentResponse create(AppointmentRequest request, String actor);

  AppointmentResponse reschedule(long id, RescheduleRequest request, String actor);

  AppointmentResponse status(
      long id, AppointmentStatus next, Long version, String reason, String actor);

  BlockResponse createBlock(BlockRequest request);

  void deleteBlock(long id);

  List<AppointmentResponse> list(
      Instant start, Instant end, Long doctorId, Long patientId, AppointmentStatus status);

  List<AppointmentResponse> today(long doctorId);

  DashboardSummary dashboard(Long doctorId);
}
