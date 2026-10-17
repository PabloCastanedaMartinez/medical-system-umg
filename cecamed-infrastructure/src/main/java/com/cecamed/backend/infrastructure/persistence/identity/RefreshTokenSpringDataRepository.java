package com.cecamed.backend.infrastructure.persistence.identity;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenSpringDataRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {

    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshTokenJpaEntity r SET r.revoked = true, r.revokedAt = :now WHERE r.userId = :userId AND r.revoked = false")
    void revokeAllByUserId(@Param("userId") Long userId, @Param("now") Instant now);
}
