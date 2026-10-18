package com.cecamed.backend.infrastructure.storage;
import java.nio.file.*;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;

class LocalDocumentStorageTest {
    @TempDir Path directory;
    @Test void persistsAcrossInstancesAndRejectsTraversalAndSymlinks() throws Exception {
        var storage=new LocalDocumentStorage(directory.toString());
        String key=storage.store(new byte[]{1,2,3});
        assertThat(new LocalDocumentStorage(directory.toString()).read(key)).containsExactly(1,2,3);
        assertThatThrownBy(()->storage.read("../outside")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->storage.delete("../outside")).isInstanceOf(IllegalArgumentException.class);
        Path outside=Files.createTempFile(directory.getParent(),"document-test-",".txt");
        try {
            Files.delete(directory.resolve(key));
            Files.createSymbolicLink(directory.resolve(key),outside);
            assertThatThrownBy(()->storage.read(key)).isInstanceOf(UncheckedIOException.class);
            storage.delete(key);
            assertThat(outside).exists();
            storage.delete(key);
        } finally { Files.deleteIfExists(outside); }
    }
}
