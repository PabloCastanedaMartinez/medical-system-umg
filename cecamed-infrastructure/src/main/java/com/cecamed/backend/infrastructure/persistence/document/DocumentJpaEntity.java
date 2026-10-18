package com.cecamed.backend.infrastructure.persistence.document;

import com.cecamed.backend.domain.document.*;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "patient_documents", schema = "cecamed")
public class DocumentJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "patient_id", nullable = false) long patientId;
    @Column(name = "storage_key", nullable = false, unique = true, length = 36) String storageKey;
    @Column(name = "original_file_name", nullable = false) String originalFileName;
    @Column(name = "file_type", nullable = false, length = 100) String fileType;
    @Enumerated(EnumType.STRING) @Column(name = "document_type", nullable = false, length = 40) DocumentType documentType;
    @Column(name = "file_size_bytes", nullable = false) long fileSizeBytes;
    @Column(name = "checksum_sha256", nullable = false, length = 64) String checksumSha256;
    @Column(length = 2000) String description;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    @Column(nullable = false) boolean active;
    protected DocumentJpaEntity() {}
    static DocumentJpaEntity from(PatientDocument d) {
        var e = new DocumentJpaEntity();
        e.id=d.id(); e.patientId=d.patientId(); e.storageKey=d.storageKey(); e.originalFileName=d.originalFileName();
        e.fileType=d.fileType(); e.documentType=d.documentType(); e.fileSizeBytes=d.fileSizeBytes();
        e.checksumSha256=d.checksumSha256(); e.description=d.description(); e.createdAt=d.createdAt();
        e.updatedAt=d.updatedAt(); e.active=d.active(); return e;
    }
    PatientDocument toDomain() {
        return new PatientDocument(id, patientId, storageKey, originalFileName, fileType, documentType,
                fileSizeBytes, checksumSha256, description, createdAt, updatedAt, active);
    }
}
