package com.cecamed.backend.domain.scheduling;

import java.util.Set;

public enum AppointmentStatus {
  PENDIENTE,
  CONFIRMADA,
  EN_SALA,
  ATENDIDA,
  CANCELADA,
  NO_ASISTIO;

  public boolean canChangeTo(AppointmentStatus next) {
    return switch (this) {
      case PENDIENTE -> Set.of(CONFIRMADA, CANCELADA, NO_ASISTIO).contains(next);
      case CONFIRMADA -> Set.of(EN_SALA, CANCELADA, NO_ASISTIO).contains(next);
      case EN_SALA -> Set.of(ATENDIDA, CANCELADA).contains(next);
      case ATENDIDA, CANCELADA, NO_ASISTIO -> false;
    };
  }

  public boolean canReschedule() {
    return this == PENDIENTE || this == CONFIRMADA;
  }
}
