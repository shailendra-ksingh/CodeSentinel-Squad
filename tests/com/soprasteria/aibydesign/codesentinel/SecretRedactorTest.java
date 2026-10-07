package com.soprasteria.aibydesign.codesentinel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SecretRedactorTest {

    @Test
    void shouldRedactApiKeyWithDoubleQuotes() {
        String source = "apiKey = \"real-secret-value\"";

        assertEquals(
                "apiKey = \"<REDACTED>\"",
                SecretRedactor.redact(source)
        );
    }

    @Test
    void shouldRedactApiKeyWithSingleQuotes() {
        String source = "apiKey = 'real-secret-value'";

        assertEquals(
                "apiKey = '<REDACTED>'",
                SecretRedactor.redact(source)
        );
    }

    @Test
    void shouldRedactPassword() {
        String source = "password = \"my-password\"";

        assertEquals(
                "password = \"<REDACTED>\"",
                SecretRedactor.redact(source)
        );
    }

    @Test
    void shouldRedactYamlSecret() {
        String source = "password: \"my-password\"";

        assertEquals(
                "password: \"<REDACTED>\"",
                SecretRedactor.redact(source)
        );
    }

    @Test
    void shouldRedactJsonSecret() {
        String source = "\"apiKey\": \"real-secret\"";

        assertEquals(
                "\"apiKey\": \"<REDACTED>\"",
                SecretRedactor.redact(source)
        );
    }

    @Test
    void shouldRedactAccessToken() {
        String source = "access_token = \"real-access-token\"";

        assertEquals(
                "access_token = \"<REDACTED>\"",
                SecretRedactor.redact(source)
        );
    }

    @Test
    void shouldRedactClientSecret() {
        String source = "client_secret = \"real-client-secret\"";

        assertEquals(
                "client_secret = \"<REDACTED>\"",
                SecretRedactor.redact(source)
        );
    }

    @Test
    void shouldRedactUnquotedSecret() {
        String source = "API_KEY=real-secret-value";

        assertEquals(
                "API_KEY=<REDACTED>",
                SecretRedactor.redact(source)
        );
    }

    @Test
    void shouldRedactYamlUnquotedSecret() {
        String source = "token: real-token-value";

        assertEquals(
                "token: <REDACTED>",
                SecretRedactor.redact(source)
        );
    }

    @Test
    void shouldRedactMultipleSecrets() {
        String source = """
                apiKey = "secret-one"
                password = "secret-two"
                token = "secret-three"
                """;

        String expected = """
                apiKey = "<REDACTED>"
                password = "<REDACTED>"
                token = "<REDACTED>"
                """;

        assertEquals(
                expected,
                SecretRedactor.redact(source)
        );
    }

    @Test
    void shouldNotLeaveOriginalSecretInOutput() {
        String source = "apiKey = \"super-secret-123\"";

        String result = SecretRedactor.redact(source);

        assertFalse(
                result.contains("super-secret-123")
        );

        assertEquals(
                "apiKey = \"<REDACTED>\"",
                result
        );
    }

    @Test
    void shouldKeepNormalCodeUnchanged() {
        String source = """
                public void hello() {
                    System.out.println("Hello");
                }
                """;

        assertEquals(
                source,
                SecretRedactor.redact(source)
        );
    }

    @Test
    void shouldKeepNormalVariableUnchanged() {
        String source = "String name = \"John\";";

        assertEquals(
                source,
                SecretRedactor.redact(source)
        );
    }

    @Test
    void shouldReturnNullForNullInput() {
        assertEquals(
                null,
                SecretRedactor.redact(null)
        );
    }

    @Test
    void shouldReturnBlankInputUnchanged() {
        assertEquals(
                "   ",
                SecretRedactor.redact("   ")
        );
    }

    @Test
    void shouldPreserveLineNumbers() {
        String source = """
                public class Test {
                    String apiKey = "real-secret";
                    String name = "John";
                }
                """;

        String result = SecretRedactor.redact(source);

        assertEquals(
                source.lines().count(),
                result.lines().count()
        );
    }
}