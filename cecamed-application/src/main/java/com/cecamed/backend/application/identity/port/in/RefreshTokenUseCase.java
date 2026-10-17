package com.cecamed.backend.application.identity.port.in;

import com.cecamed.backend.application.identity.model.RefreshTokenCommand;
import com.cecamed.backend.application.identity.model.RefreshTokenResult;

public interface RefreshTokenUseCase {
    RefreshTokenResult refresh(RefreshTokenCommand command);
}
