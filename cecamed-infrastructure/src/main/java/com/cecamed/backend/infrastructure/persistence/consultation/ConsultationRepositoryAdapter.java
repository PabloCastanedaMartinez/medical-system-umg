package com.cecamed.backend.infrastructure.persistence.consultation;

import com.cecamed.backend.application.consultation.ConsultationRepositoryPort;
import com.cecamed.backend.domain.consultation.Consultation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador del repositorio de consultas médicas.
 */
@Component
@Transactional
public class ConsultationRepositoryAdapter implements ConsultationRepositoryPort {

    private final ConsultationSpringDataRepository repository;

    public ConsultationRepositoryAdapter(ConsultationSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Consultation> findById(Long consultaId) {
        return repository.findById(consultaId)
                .map(ConsultationJpaEntity::toDomain);
    }

    @Override
    public Consultation save(Consultation consulta) {
        ConsultationJpaEntity entity = ConsultationJpaEntity.fromDomain(consulta);
        return repository.save(entity).toDomain();
    }

    @Override
    public List<Consultation> findByPatientId(Long patientId) {
        return repository.findByPatientId(patientId)
                .stream()
                .map(ConsultationJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long consultaId) {
        repository.deleteById(consultaId);
    }
}