package com.cecamed.backend.infrastructure.storage;

import com.cecamed.backend.application.document.DocumentStoragePort;
import com.cecamed.backend.application.document.DocumentService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalDocumentStorage implements DocumentStoragePort {
    private final Path root;
    public LocalDocumentStorage(@Value("${cecamed.documents.directory:uploads/documents}") String directory) {
        try {
            root = Path.of(directory).toAbsolutePath().normalize();
            Files.createDirectories(root);
            if (Files.isSymbolicLink(root) || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS))
                throw new IOException("El directorio de documentos debe ser un directorio real.");
        } catch (IOException e) { throw new UncheckedIOException("No se pudo preparar el almacenamiento de documentos.", e); }
    }
    private Path resolve(String key) {
        if (key == null || !key.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
            throw new IllegalArgumentException("Clave de almacenamiento inválida.");
        return root.resolve(key);
    }
    public String store(byte[] content) {
        String key = UUID.randomUUID().toString();
        Path file = resolve(key);
        boolean created = false;
        try {
            // Create first so cleanup never removes a pre-existing file on collision.
            if (file.getFileSystem().supportedFileAttributeViews().contains("posix")) {
                Files.createFile(file, java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
                        java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")));
            } else {
                Files.createFile(file);
            }
            created = true;
            Files.write(file, content, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
            return key;
        } catch (IOException e) {
            if (created) {
                try { Files.deleteIfExists(file); } catch (IOException cleanup) { e.addSuppressed(cleanup); }
            }
            throw new UncheckedIOException("No se pudo guardar el documento.", e);
        }
    }
    public byte[] read(String key) {
        try (var input=Files.newInputStream(resolve(key), LinkOption.NOFOLLOW_LINKS)) {
            byte[] data=input.readNBytes(DocumentService.MAX_BYTES + 1);
            if (data.length > DocumentService.MAX_BYTES) throw new IOException("Archivo almacenado fuera del límite permitido.");
            return data;
        } catch (IOException e) { throw new UncheckedIOException("No se pudo leer el documento.", e); }
    }
    public void delete(String key) {
        try { Files.deleteIfExists(resolve(key)); }
        catch (IOException e) { throw new UncheckedIOException("No se pudo eliminar el documento.", e); }
    }
}
