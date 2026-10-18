package com.cecamed.backend.application.document;
import com.cecamed.backend.application.patient.port.out.PatientRepositoryPort;
import com.cecamed.backend.domain.patient.Patient;
import com.cecamed.backend.domain.document.*;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class DocumentServiceTest {
    final DocumentRepositoryPort repository=mock(DocumentRepositoryPort.class);
    final DocumentStoragePort storage=mock(DocumentStoragePort.class);
    final PatientRepositoryPort patients=mock(PatientRepositoryPort.class);
    final DocumentService service=new DocumentService(repository,storage,patients,Clock.systemUTC());
    @Test void failedMetadataCommitRemovesUploadedBytes() {
        var patient=mock(Patient.class);
        when(patient.isActive()).thenReturn(true);
        when(patients.findById(1L)).thenReturn(Optional.of(patient));
        when(storage.store(any())).thenReturn("stored-key");
        when(repository.save(any())).thenThrow(new IllegalStateException("database unavailable"));
        assertThatThrownBy(()->service.upload(1L,"a.pdf",DocumentType.OTRO,null,"%PDF-1.4\n%%EOF".getBytes()))
                .isInstanceOf(IllegalStateException.class);
        verify(storage).delete("stored-key");
    }
    @Test void failedTombstoneDoesNotDeleteFile() {
        var d=new PatientDocument(2L,1L,"key","a.pdf","application/pdf",DocumentType.OTRO,20,"hash",null,Instant.now(),Instant.now(),true);
        when(repository.find(2L)).thenReturn(Optional.of(d));
        doThrow(new IllegalStateException("database unavailable")).when(repository).deactivate(2L);
        assertThatThrownBy(()->service.delete(2L,null)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(storage);
    }
    @Test void deletionCanRetryAfterDiskFailure() {
        var d=new PatientDocument(2L,1L,"key","a.pdf","application/pdf",DocumentType.OTRO,20,"hash",null,Instant.now(),Instant.now(),false);
        when(repository.find(2L)).thenReturn(Optional.of(d));
        doThrow(new IllegalStateException("disk unavailable")).doNothing().when(storage).delete("key");
        assertThatThrownBy(()->service.delete(2L,null)).isInstanceOf(IllegalStateException.class);
        service.delete(2L,null);
        verify(storage,times(2)).delete("key");
    }
}
