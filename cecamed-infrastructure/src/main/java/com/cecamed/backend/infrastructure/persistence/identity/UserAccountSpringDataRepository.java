package com.cecamed.backend.infrastructure.persistence.identity;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAccountSpringDataRepository
        extends JpaRepository<UserAccountJpaEntity, Long>, JpaSpecificationExecutor<UserAccountJpaEntity> {

    Optional<UserAccountJpaEntity> findByUsername(String username);

    boolean existsByUsername(String username);
}
