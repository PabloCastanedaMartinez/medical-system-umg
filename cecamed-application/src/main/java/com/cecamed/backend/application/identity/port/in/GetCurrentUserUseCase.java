package com.cecamed.backend.application.identity.port.in;

import com.cecamed.backend.application.identity.model.CurrentUserResult;

public interface GetCurrentUserUseCase {
    CurrentUserResult getCurrentUser(String username);
}
