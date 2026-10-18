package com.cecamed.backend.domain.scheduling;

import java.time.Instant;
import java.util.Objects;

/** Intervalo semiabierto: citas adyacentes pueden compartir su límite. */
public record TimeInterval(Instant start, Instant end) {
  public TimeInterval {
    Objects.requireNonNull(start, "La hora de inicio es obligatoria.");
    Objects.requireNonNull(end, "La hora de fin es obligatoria.");
    if (!start.isBefore(end))
      throw new IllegalArgumentException("El inicio debe ser anterior al fin.");
  }

  public boolean overlaps(TimeInterval other) {
    return start.isBefore(other.end) && end.isAfter(other.start);
  }
}
