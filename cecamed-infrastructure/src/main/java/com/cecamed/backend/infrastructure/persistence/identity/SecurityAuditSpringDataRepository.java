package com.cecamed.backend.infrastructure.persistence.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityAuditSpringDataRepository extends JpaRepository<SecurityAuditJpaEntity, Long> {
}
