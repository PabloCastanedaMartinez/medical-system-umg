package com.cecamed.backend.application.document;

import com.cecamed.backend.application.patient.port.out.PatientRepositoryPort;
import com.cecamed.backend.application.shared.ResourceNotFoundException;
import com.cecamed.backend.domain.document.*;
import com.cecamed.backend.domain.patient.PatientInactiveException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;

public class DocumentService implements DocumentUseCase {
    public static final int MAX_BYTES = 15 * 1024 * 1024;
    private final DocumentRepositoryPort repository;
    private final DocumentStoragePort storage;
    private final PatientRepositoryPort patients;
    private final Clock clock;

    public DocumentService(DocumentRepositoryPort repository, DocumentStoragePort storage,
            PatientRepositoryPort patients, Clock clock) {
        this.repository = repository; this.storage = storage; this.patients = patients; this.clock = clock;
    }
    public List<PatientDocument> list(long patientId) {
        requirePatient(patientId);
        return repository.list(patientId);
    }
    public PatientDocument upload(long patientId, String filename, DocumentType type,
            String description, byte[] content) {
        if (!requirePatient(patientId).isActive()) throw new PatientInactiveException("El paciente está inactivo.");
        if (type == null) throw new IllegalArgumentException("Se requiere el tipo de documento.");
        if (content == null || content.length == 0 || content.length > MAX_BYTES)
            throw new IllegalArgumentException("El archivo debe contener entre 1 byte y 15 MB.");
        if (filename == null || filename.isBlank()) throw new IllegalArgumentException("Se requiere el nombre del archivo.");
        String name = filename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        if (name.isBlank() || name.length() > 255 || name.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Nombre de archivo inválido.");
        if (description != null && description.length() > 2000)
            throw new IllegalArgumentException("La descripción admite hasta 2000 caracteres.");
        String mime = detectType(content);
        String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT);
        if (!(mime.equals("application/pdf") && extension.equals("pdf")
                || mime.equals("image/png") && extension.equals("png")
                || mime.equals("image/jpeg") && List.of("jpg", "jpeg").contains(extension)))
            throw new IllegalArgumentException("La extensión no corresponde al contenido del archivo.");
        String checksum;
        try { checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
        String key = storage.store(content);
        var now = clock.instant();
        try {
            return repository.save(new PatientDocument(null, patientId, key, name, mime, type,
                    content.length, checksum, description == null || description.isBlank() ? null : description.trim(), now, now, true));
        } catch (RuntimeException failure) {
            try { storage.delete(key); } catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
    public Download download(long id, Long patientId) {
        var document = find(id, patientId);
        if (!document.active()) throw missing();
        return new Download(document, storage.read(document.storageKey()));
    }
    public void delete(long id, Long patientId) {
        var document = find(id, patientId);
        // Commit before deleting bytes: failed disk deletion can be retried using the same ID.
        repository.deactivate(id);
        storage.delete(document.storageKey());
    }
    private PatientDocument find(long id, Long patientId) {
        var document = repository.find(id).orElseThrow(DocumentService::missing);
        if (patientId != null && document.patientId() != patientId) throw missing();
        return document;
    }
    private static ResourceNotFoundException missing() { return new ResourceNotFoundException("Documento no encontrado."); }
    private com.cecamed.backend.domain.patient.Patient requirePatient(long id) {
        return patients.findById(id).orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado."));
    }
    private static String detectType(byte[] bytes) {
        if (bytes.length >= 12 && new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")
                && new String(bytes, Math.max(0, bytes.length - 1024), Math.min(1024, bytes.length), StandardCharsets.ISO_8859_1).contains("%%EOF"))
            return "application/pdf";
        byte[] png = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
        if (bytes.length >= 24 && java.util.Arrays.equals(png, java.util.Arrays.copyOf(bytes, 8))) return "image/png";
        if (bytes.length >= 4 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216
                && (bytes[2] & 255) == 255 && (bytes[bytes.length - 2] & 255) == 255
                && (bytes[bytes.length - 1] & 255) == 217) return "image/jpeg";
        throw new IllegalArgumentException("Solo se admiten archivos PDF, PNG y JPEG con firma válida.");
    }
}
