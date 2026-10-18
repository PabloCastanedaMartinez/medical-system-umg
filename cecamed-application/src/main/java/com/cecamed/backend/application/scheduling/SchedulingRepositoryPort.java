package com.cecamed.backend.application.scheduling;

import static com.cecamed.backend.application.scheduling.SchedulingDtos.*;

import com.cecamed.backend.domain.scheduling.AppointmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public interface SchedulingRepositoryPort {
  <T> T transaction(Supplier<T> work);

  <T> T withDoctorLock(long doctorId, Supplier<T> work);

  List<DoctorResponse> doctors();

  Optional<DoctorResponse> doctor(long id);

  Optional<Long> doctorIdForUsername(String username);

  Optional<Long> patientIdForUsername(String username);

  DoctorResponse saveDoctor(Long id, DoctorRequest request);

  List<ScheduleResponse> schedules(Long doctorId);

  List<BlockResponse> blocks(Long doctorId);

  BlockResponse createBlock(BlockRequest request, Instant now);

  void deleteBlock(long id);

  Optional<BlockResponse> block(long id);

  Optional<AppointmentResponse> appointment(long id);

  List<AppointmentResponse> appointments(
      Instant start, Instant end, Long doctorId, Long patientId, AppointmentStatus status);

  boolean overlaps(long doctorId, Instant start, Instant end, Long excludingId);

  AppointmentResponse createAppointment(AppointmentRequest request, Instant now);

  AppointmentResponse reschedule(long id, RescheduleRequest request, Instant now);

  AppointmentResponse changeStatus(long id, AppointmentStatus status, String reason, Instant now);

  void audit(long appointmentId, String action, String actor, String details, Instant now);

  void linkPatient(long patientId, long userId);

  long activePatientCount();
}
