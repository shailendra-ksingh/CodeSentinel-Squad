package com.soprasteria.aibydesign.codesentinel;

import java.util.regex.Pattern;

/**
 * Masks hard-coded credential values before source code leaves the machine.
 *
 * The StaticAnalyzer still sees the original source (so it can report the
 * issue); only the text sent to the AI service is redacted. Line numbers
 * are unchanged because only the string literal is replaced.
 */
public final class SecretRedactor {

    static final String PLACEHOLDER = "<REDACTED>";

    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
            "(?i)(\\b\\w*(?:password|passwd|secret|api_?key|token)\\w*\\s*=\\s*)\"[^\"]+\"");

    private SecretRedactor() { }

    public static String redact(String sourceCode) {
        if (sourceCode == null) {
            return "";
        }
        return SECRET_ASSIGNMENT.matcher(sourceCode)
                .replaceAll("$1\\\"" + PLACEHOLDER + "\\\"");
    }
}
