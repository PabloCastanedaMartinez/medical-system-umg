package com.cecamed.backend.application.identity.model;

import java.util.List;

public record PagedResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public PagedResult {
        content = List.copyOf(content);
    }
}
