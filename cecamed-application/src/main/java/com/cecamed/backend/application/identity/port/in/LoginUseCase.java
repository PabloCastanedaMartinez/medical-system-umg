package com.cecamed.backend.application.identity.port.in;

import com.cecamed.backend.application.identity.model.LoginCommand;
import com.cecamed.backend.application.identity.model.LoginResult;

public interface LoginUseCase {
    LoginResult login(LoginCommand command);
}
