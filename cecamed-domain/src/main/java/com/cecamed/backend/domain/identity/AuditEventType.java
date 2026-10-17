package com.cecamed.backend.domain.identity;

public enum AuditEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    TOKEN_REFRESH,
    REFRESH_TOKEN_REUSE_DETECTED,
    LOGOUT,
    ACCOUNT_LOCKED,
    USER_CREATED,
    USER_UPDATED,
    USER_ACTIVATED,
    USER_DEACTIVATED,
    PASSWORD_RESET
}
