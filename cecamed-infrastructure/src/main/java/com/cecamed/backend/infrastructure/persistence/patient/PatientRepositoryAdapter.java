package com.cecamed.backend.infrastructure.persistence.patient;

import com.cecamed.backend.application.patient.port.out.PatientRepositoryPort;
import com.cecamed.backend.domain.patient.Patient;
import com.cecamed.backend.application.identity.model.PagedResult;
import com.cecamed.backend.domain.patient.PatientAlreadyExistsException;
import com.cecamed.backend.domain.patient.StaleVersionException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador que implementa el puerto de salida para pacientes usando JPA.
 */
@Component
public class PatientRepositoryAdapter implements PatientRepositoryPort {

    @PersistenceContext
    private EntityManager entityManager;

    private final PatientSpringDataRepository repository;

    public PatientRepositoryAdapter(PatientSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Patient save(Patient patient) {
        try {
            PatientJpaEntity entity = PatientJpaEntity.fromDomain(patient);
            PatientJpaEntity saved = repository.save(entity);
            return saved.toDomain();
        } catch (OptimisticLockingFailureException ex) {
            throw new StaleVersionException("Conflicto de versión al actualizar el paciente. Por favor, inténtelo de nuevo.");
        } catch (DataIntegrityViolationException ex) {
            // Verificar si es por violación de unicidad en identification_number
            if (ex.getCause() instanceof ConstraintViolationException) {
                ConstraintViolationException cvEx = (ConstraintViolationException) ex.getCause();
                if ("uq_patients_identification_number".equals(cvEx.getConstraintName())) {
                    throw new PatientAlreadyExistsException(
                            "Ya existe un paciente con el número de identificación proporcionado");
                }
            }
            throw ex; // volver a lanzar otras violaciones de integridad
        }
    }

    @Override
    public boolean existsByIdentificationNumber(String identificationNumber) {
        if (identificationNumber == null) {
            return false;
        }
        return repository.existsByIdentificationNumber(identificationNumber);
    }

    @Override
    public Optional<Patient> findById(Long id) {
        return repository.findById(id)
                .map(PatientJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<Patient> search(int page, int size, String search, Boolean active) {
        // Normalizar parámetros
        if (page < 0) page = 0;
        if (size <= 0) size = 10000; // mismo límite que en el servicio
        String searchTerm = (search == null) ? null : search.trim();

        // Construir consulta Criteria
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<PatientJpaEntity> cq = cb.createQuery(PatientJpaEntity.class);
        Root<PatientJpaEntity> root = cq.from(PatientJpaEntity.class);

        // Construir predicados
        Predicate conjunction = cb.conjunction();
        if (active != null) {
            conjunction.getExpressions().add(cb.equal(root.get("active"), active));
        }
        if (searchTerm != null && !searchTerm.isEmpty()) {
            String likePattern = "%" + searchTerm.toUpperCase() + "%";
            Predicate namePredicate = cb.or(
                    cb.like(cb.upper(root.get("identificationNumber")), likePattern),
                    cb.like(cb.upper(root.get("firstName")), likePattern),
                    cb.like(cb.upper(root.get("lastName")), likePattern),
                    cb.like(cb.upper(root.get("phone")), likePattern),
                    cb.like(cb.upper(root.get("email")), likePattern)
            );
            conjunction.getExpressions().add(namePredicate);
        }
        cq.where(conjunction);
        cq.orderBy(cb.asc(root.get("id"))); // ordenar por ID para consistencia

        // Contar total de elementos
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        countQuery.select(cb.count(countQuery.from(PatientJpaEntity.class)));
        countQuery.where(cq.getRestriction());
        Long totalElements = entityManager.createQuery(countQuery).getSingleResult();

        // Ejecutar consulta de página
        TypedQuery<PatientJpaEntity> query = entityManager.createQuery(cq);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        List<PatientJpaEntity> resultList = query.getResultList();

        // Mapear a dominio
        List<Patient> patients = new ArrayList<>();
        for (PatientJpaEntity entity : resultList) {
            patients.add(entity.toDomain());
        }

        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PagedResult<>(patients, page, size, totalElements, totalPages);
    }
}