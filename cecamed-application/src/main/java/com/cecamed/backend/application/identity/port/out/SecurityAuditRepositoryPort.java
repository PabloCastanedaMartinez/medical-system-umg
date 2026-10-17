package com.cecamed.backend.application.identity.port.out;

import com.cecamed.backend.domain.identity.SecurityAuditEvent;

public interface SecurityAuditRepositoryPort {
    void save(SecurityAuditEvent event);
}
