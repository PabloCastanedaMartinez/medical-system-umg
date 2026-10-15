package com.cecamed.backend.application.shared;

/** El recurso solicitado por un caso de uso no existe. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
