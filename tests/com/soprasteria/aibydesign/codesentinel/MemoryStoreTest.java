package com.soprasteria.aibydesign.codesentinel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemoryStoreTest {

    private static Finding f(int line, String desc) {
        return new Finding(Finding.Severity.CRITICAL, line, "Security", desc);
    }

    @Test
    void recurringIgnoresLineNumbers(@TempDir Path dir) throws Exception {
        MemoryStore store = new MemoryStore(dir);
        store.save("Svc", List.of(f(10, "hardcoded password")));

        List<Finding> previous = store.loadPrevious("Svc");
        List<Finding> current = List.of(f(42, "hardcoded password"), f(7, "new issue"));

        List<Finding> recurring = store.recurring(current, previous);
        assertEquals(1, recurring.size());
        assertEquals(42, recurring.get(0).getLineNumber());
    }

    @Test
    void firstReviewHasNoHistory(@TempDir Path dir) throws Exception {
        assertTrue(new MemoryStore(dir).loadPrevious("Unknown").isEmpty());
    }

    @Test
    void corruptLinesAreIgnored(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("Svc.log"),
                "garbage\n\nts|NOT_A_SEVERITY|1|Cat|d\nts|MAJOR|3|Error Handling|ok\n");
        List<Finding> loaded = new MemoryStore(dir).loadPrevious("Svc");
        assertEquals(1, loaded.size());
        assertEquals("Error Handling", loaded.get(0).getCategory());
    }

    @Test
    void pipeCharacterInDescriptionRoundTrips(@TempDir Path dir) throws Exception {
        MemoryStore store = new MemoryStore(dir);
        Finding withPipe = f(1, "a | b");
        store.save("Svc", List.of(withPipe));
        assertEquals(1, store.recurring(List.of(withPipe), store.loadPrevious("Svc")).size());
    }
}
