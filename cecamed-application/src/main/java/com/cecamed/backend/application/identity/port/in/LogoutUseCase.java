package com.cecamed.backend.application.identity.port.in;

import com.cecamed.backend.application.identity.model.LogoutCommand;

public interface LogoutUseCase {
    void logout(LogoutCommand command);
}
