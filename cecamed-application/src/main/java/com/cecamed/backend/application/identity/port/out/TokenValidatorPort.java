package com.cecamed.backend.application.identity.port.out;

import com.cecamed.backend.application.identity.model.TokenClaims;

public interface TokenValidatorPort {
    TokenClaims validateAccessToken(String token);
}
