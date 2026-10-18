package com.cecamed.backend.domain.document;

import java.time.Instant;

public record PatientDocument(Long id, long patientId, String storageKey, String originalFileName,
        String fileType, DocumentType documentType, long fileSizeBytes, String checksumSha256,
        String description, Instant createdAt, Instant updatedAt, boolean active) {}
