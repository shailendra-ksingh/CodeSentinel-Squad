package com.soprasteria.aibydesign.codesentinel;

import java.util.regex.Pattern;

/**
 * Masks common credential-like values before source code is sent to AI.
 *
 * The original source remains available to the local analyzers.
 * Only the copy sent to AI is redacted.
 *
 * This is a lightweight protection layer and is not a replacement
 * for a dedicated secret-scanning tool.
 */
public final class SecretRedactor {

    static final String PLACEHOLDER = "<REDACTED>";

    /*
     * Matches common credential names, including compound names such as:
     *
     * password
     * dbPassword
     * database_password
     * apiKey
     * clientApiKey
     * client_secret
     * accessToken
     *
     * Supported assignment styles include Java, JSON, YAML and
     * environment-variable style configuration.
     */
    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
            "(?i)([\"']?"
                    + "(?:[a-z0-9_-]*(?:password|passwd|secret|"
                    + "api[_-]?key|access[_-]?token|token|"
                    + "client[_-]?secret))"
                    + "[\"']?\\s*[:=]\\s*)"
                    + "(\"[^\"]*\"|'[^']*'|[^\\s,;}]+)"
    );

    private SecretRedactor() {
        // Utility class.
    }

    /**
     * Replaces values assigned to credential-like names.
     *
     * Quotation marks are preserved when the original value was quoted.
     */
    public static String redact(String sourceCode) {

        if (sourceCode == null || sourceCode.isBlank()) {
            return sourceCode;
        }

        return SECRET_ASSIGNMENT
                .matcher(sourceCode)
                .replaceAll(match -> {

                    String prefix = match.group(1);
                    String value = match.group(2);

                    if (isDoubleQuoted(value)) {
                        return prefix + "\"" + PLACEHOLDER + "\"";
                    }

                    if (isSingleQuoted(value)) {
                        return prefix + "'" + PLACEHOLDER + "'";
                    }

                    return prefix + PLACEHOLDER;
                });
    }

    private static boolean isDoubleQuoted(String value) {
        return value.length() >= 2
                && value.startsWith("\"")
                && value.endsWith("\"");
    }

    private static boolean isSingleQuoted(String value) {
        return value.length() >= 2
                && value.startsWith("'")
                && value.endsWith("'");
    }
}