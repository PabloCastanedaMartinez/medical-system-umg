package com.cecamed.backend.domain.identity;

public enum Role {
    MEDICO("Médico"),
    RECEPCION("Recepción"),
    ADMIN("Administrador");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
