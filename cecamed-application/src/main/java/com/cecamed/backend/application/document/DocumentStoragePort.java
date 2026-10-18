package com.cecamed.backend.application.document;

public interface DocumentStoragePort {
    String store(byte[] content);
    byte[] read(String key);
    void delete(String key);
}
