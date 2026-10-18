package com.cecamed.backend.api.document;

import com.cecamed.backend.application.document.DocumentUseCase;
import com.cecamed.backend.application.document.DocumentService;
import com.cecamed.backend.domain.document.*;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('MEDICO')")
public class DocumentController {
    private final DocumentUseCase documents;
    public DocumentController(DocumentUseCase documents) { this.documents=documents; }
    @GetMapping("/patients/{patientId}/documents")
    public List<DocumentResponse> list(@PathVariable long patientId) {
        return documents.list(patientId).stream().map(DocumentResponse::from).toList();
    }
    @PostMapping(value="/patients/{patientId}/documents", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(@PathVariable long patientId,
            @RequestParam MultipartFile file, @RequestParam DocumentType documentType,
            @RequestParam(required=false) String description) throws IOException {
        if (file.getSize() > DocumentService.MAX_BYTES) throw new MaxUploadSizeExceededException(DocumentService.MAX_BYTES);
        byte[] bytes;
        try (var input=file.getInputStream()) { bytes=input.readNBytes(DocumentService.MAX_BYTES + 1); }
        if (bytes.length > DocumentService.MAX_BYTES) throw new MaxUploadSizeExceededException(DocumentService.MAX_BYTES);
        var d=documents.upload(patientId, file.getOriginalFilename(), documentType, description, bytes);
        return ResponseEntity.created(URI.create("/api/v1/documents/"+d.id()+"/file")).body(DocumentResponse.from(d));
    }
    @GetMapping({"/documents/{id}/file", "/patients/{patientId}/documents/{id}/content"})
    public ResponseEntity<byte[]> download(@PathVariable long id, @PathVariable(required=false) Long patientId) {
        var download=documents.download(id, patientId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(download.document().fileType()))
                .contentLength(download.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.document().originalFileName(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff").body(download.content());
    }
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        documents.delete(id, null); return ResponseEntity.noContent().build();
    }
    @PostMapping("/patients/{patientId}/documents/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable long patientId, @PathVariable long id) {
        documents.delete(id, patientId); return ResponseEntity.noContent().build();
    }
    public record DocumentResponse(long id, long patientId, Long consultationId, String fileName,
            String originalFileName, String fileType, DocumentType documentType, long fileSizeBytes,
            String checksumSha256, String description, Instant createdAt, Instant updatedAt) {
        static DocumentResponse from(PatientDocument d) {
            return new DocumentResponse(d.id(), d.patientId(), null, d.originalFileName(), d.originalFileName(),
                    d.fileType(), d.documentType(), d.fileSizeBytes(), d.checksumSha256(), d.description(), d.createdAt(), d.updatedAt());
        }
    }
}
