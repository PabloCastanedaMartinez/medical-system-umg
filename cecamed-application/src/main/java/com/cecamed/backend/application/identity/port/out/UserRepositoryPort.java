package com.cecamed.backend.application.identity.port.out;

import com.cecamed.backend.application.identity.model.PagedResult;
import com.cecamed.backend.domain.identity.Role;
import com.cecamed.backend.domain.identity.UserAccount;
import java.util.Optional;

public interface UserRepositoryPort {
    Optional<UserAccount> findByUsername(String username);
    Optional<UserAccount> findById(Long id);
    boolean existsByUsername(String username);
    UserAccount save(UserAccount userAccount);
    PagedResult<UserAccount> findAll(int page, int size, String search, Role role, Boolean active);
}
