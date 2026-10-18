package com.cecamed.backend.infrastructure.persistence.document;

import com.cecamed.backend.application.document.DocumentRepositoryPort;
import com.cecamed.backend.application.shared.ResourceNotFoundException;
import com.cecamed.backend.domain.document.PatientDocument;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

@Repository
public class DocumentRepositoryAdapter implements DocumentRepositoryPort {
    private final DocumentJpaRepository repository;
    private final Clock clock;
    public DocumentRepositoryAdapter(DocumentJpaRepository repository, Clock clock) { this.repository=repository; this.clock=clock; }
    public List<PatientDocument> list(long patientId) {
        return repository.findByPatientIdAndActiveTrueOrderByCreatedAtDescIdDesc(patientId).stream().map(DocumentJpaEntity::toDomain).toList();
    }
    public Optional<PatientDocument> find(long id) { return repository.findById(id).map(DocumentJpaEntity::toDomain); }
    @Transactional
    public PatientDocument save(PatientDocument document) { return repository.saveAndFlush(DocumentJpaEntity.from(document)).toDomain(); }
    @Transactional
    public void deactivate(long id) {
        var e = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado."));
        if (e.active) { e.active=false; e.updatedAt=clock.instant(); repository.saveAndFlush(e); }
    }
}
