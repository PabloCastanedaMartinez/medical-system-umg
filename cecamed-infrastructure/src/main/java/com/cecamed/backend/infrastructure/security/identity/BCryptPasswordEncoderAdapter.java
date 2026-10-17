package com.cecamed.backend.infrastructure.security.identity;

import com.cecamed.backend.application.identity.port.out.PasswordEncoderPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordEncoderAdapter implements PasswordEncoderPort {

    private final BCryptPasswordEncoder delegate;

    public BCryptPasswordEncoderAdapter() {
        this.delegate = new BCryptPasswordEncoder(12);
    }

    public BCryptPasswordEncoderAdapter(int strength) {
        this.delegate = new BCryptPasswordEncoder(strength);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return delegate.matches(rawPassword, encodedPassword);
    }
}
