package com.cecamed.backend.infrastructure.persistence.identity;

import com.cecamed.backend.application.identity.model.PagedResult;
import com.cecamed.backend.application.identity.port.out.UserRepositoryPort;
import com.cecamed.backend.domain.identity.Role;
import com.cecamed.backend.domain.identity.UserAccount;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserAccountSpringDataRepository repository;

    public UserRepositoryAdapter(UserAccountSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findByUsername(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        return repository.findByUsername(username.trim().toLowerCase()).map(UserAccountJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findById(Long id) {
        if (id == null) return Optional.empty();
        return repository.findById(id).map(UserAccountJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        if (username == null || username.isBlank()) return false;
        return repository.existsByUsername(username.trim().toLowerCase());
    }

    @Override
    public UserAccount save(UserAccount userAccount) {
        UserAccountJpaEntity entity = UserAccountJpaEntity.fromDomain(userAccount);
        return repository.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<UserAccount> findAll(int page, int size, String search, Role role, Boolean active) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(100, size));
        var pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "username"));

        Specification<UserAccountJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate usernameMatch = cb.like(cb.lower(root.get("username")), pattern);
                Predicate fullNameMatch = cb.like(cb.lower(root.get("fullName")), pattern);
                predicates.add(cb.or(usernameMatch, fullNameMatch));
            }

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<UserAccountJpaEntity> springPage = repository.findAll(spec, pageable);
        List<UserAccount> domainList = springPage.getContent().stream()
                .map(UserAccountJpaEntity::toDomain)
                .toList();

        return new PagedResult<>(
                domainList,
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages()
        );
    }
}
