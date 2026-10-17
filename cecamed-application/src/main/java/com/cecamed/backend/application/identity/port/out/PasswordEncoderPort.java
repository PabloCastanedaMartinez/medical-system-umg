package com.cecamed.backend.application.identity.port.out;

public interface PasswordEncoderPort {
    String encode(CharSequence rawPassword);
    boolean matches(CharSequence rawPassword, String encodedPassword);
}
