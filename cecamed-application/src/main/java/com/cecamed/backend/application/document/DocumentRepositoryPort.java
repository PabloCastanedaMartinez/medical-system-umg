package com.cecamed.backend.application.document;

import com.cecamed.backend.domain.document.PatientDocument;
import java.util.List;
import java.util.Optional;

public interface DocumentRepositoryPort {
    List<PatientDocument> list(long patientId);
    Optional<PatientDocument> find(long id);
    PatientDocument save(PatientDocument document);
    void deactivate(long id);
}
