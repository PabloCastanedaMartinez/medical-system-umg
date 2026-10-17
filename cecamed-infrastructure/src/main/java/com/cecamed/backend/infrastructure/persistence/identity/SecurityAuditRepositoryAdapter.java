package com.cecamed.backend.infrastructure.persistence.identity;

import com.cecamed.backend.application.identity.port.out.SecurityAuditRepositoryPort;
import com.cecamed.backend.domain.identity.SecurityAuditEvent;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SecurityAuditRepositoryAdapter implements SecurityAuditRepositoryPort {

    private final SecurityAuditSpringDataRepository repository;

    public SecurityAuditRepositoryAdapter(SecurityAuditSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(SecurityAuditEvent event) {
        if (event == null) return;
        SecurityAuditJpaEntity entity = SecurityAuditJpaEntity.fromDomain(event);
        repository.save(entity);
    }
}
