package com.cecamed.backend.application.document;

import com.cecamed.backend.domain.document.DocumentType;
import com.cecamed.backend.domain.document.PatientDocument;
import java.util.List;

public interface DocumentUseCase {
    List<PatientDocument> list(long patientId);
    PatientDocument upload(long patientId, String filename, DocumentType type, String description, byte[] content);
    Download download(long id, Long patientId);
    void delete(long id, Long patientId);
    record Download(PatientDocument document, byte[] content) {}
}
