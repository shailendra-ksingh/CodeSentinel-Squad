package com.soprasteria.aibydesign.codesentinel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecretRedactorTest {

    @Test
    void masksCredentialValuesButKeepsStructure() {
        String src = "public String dbPassword = \"Hunter2\";\nString apiKey = \"abc123\";\nint n = 1;";
        String out = SecretRedactor.redact(src);

        assertFalse(out.contains("Hunter2"));
        assertFalse(out.contains("abc123"));
        assertTrue(out.contains("dbPassword = \"<REDACTED>\""));
        assertEquals(src.lines().count(), out.lines().count(), "line numbers must be preserved");
    }

    @Test
    void leavesOrdinaryCodeUntouched() {
        String src = "String greeting = \"hello\";";
        assertEquals(src, SecretRedactor.redact(src));
    }
}
