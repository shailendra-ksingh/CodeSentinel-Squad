package com.soprasteria.aibydesign.codesentinel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Offline tests for request/response handling (no network). */
class ClaudeClientTest {

    @Test
    void notConfiguredWithoutKey() {
        assertFalse(new ClaudeClient(null).isConfigured());
        assertFalse(new ClaudeClient("  ").isConfigured());
        assertTrue(new ClaudeClient("placeholder").isConfigured());
    }

    @Test
    void extractsAndUnescapesTextBlocks() {
        String json = "{\"content\":[{\"type\":\"text\",\"text\":\"Line1\\nLine \\\"2\\\" \\u00e9 \\\\ end\"}]}";
        assertEquals("Line1\nLine \"2\" \u00e9 \\ end", ClaudeClient.extractText(json));
    }

    @Test
    void joinsMultipleTextBlocks() {
        String json = "{\"content\":[{\"type\":\"text\",\"text\":\"A\"},{\"type\":\"text\",\"text\":\"B\"}]}";
        assertEquals("AB", ClaudeClient.extractText(json));
    }

    @Test
    void escapesControlCharactersInRequests() {
        assertEquals("a\\\"b\\\\c\\n\\u0001", ClaudeClient.escapeJson("a\"b\\c\n\u0001"));
    }

    @Test
    void askWithoutKeyFailsClearly() {
        assertThrows(IllegalStateException.class, () -> new ClaudeClient(null).ask("s", "u"));
    }
}
