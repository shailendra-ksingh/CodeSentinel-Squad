package com.soprasteria.aibydesign.codesentinel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performs lightweight deterministic checks across project files.
 *
 * The goal is not to build a full parser. It detects simple, high-value
 * contract mismatches before AI reasoning is used.
 */
public class ProjectContractAnalyzer {

    private static final Pattern TYPESCRIPT_INTERFACE =
            Pattern.compile(
                    "interface\\s+\\w+\\s*\\{(.*?)\\}",
                    Pattern.DOTALL);

    private static final Pattern TYPESCRIPT_FIELD =
            Pattern.compile(
                    "\\b([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\??\\s*:");

    private static final Pattern JAVA_RECORD =
            Pattern.compile(
                    "record\\s+\\w+\\s*\\((.*?)\\)",
                    Pattern.DOTALL);

    private static final Pattern JAVA_FIELD =
            Pattern.compile(
                    "\\b[A-Za-z_$][A-Za-z0-9_$<>., ?\\[\\]]*\\s+"
                            + "([A-Za-z_$][A-Za-z0-9_$]*)\\s*(?:,|$)");

    /**
     * Finds simple TypeScript-to-Java response contract mismatches.
     *
     * @param sources project source files
     * @return deterministic cross-file findings
     */
    public List<String> analyze(
            Map<String, String> sources) {

        List<String> findings = new ArrayList<>();

        if (sources == null || sources.size() < 2) {
            return findings;
        }

        String typescriptSource =
                findSource(sources, ".ts", ".tsx");

        String javaSource =
                findSource(sources, ".java");

        if (typescriptSource == null
                || javaSource == null) {
            return findings;
        }

        List<String> typescriptFields =
                extractTypeScriptFields(typescriptSource);

        List<String> javaFields =
                extractJavaFields(javaSource);

        if (typescriptFields.isEmpty()
                || javaFields.isEmpty()) {
            return findings;
        }

        List<String> missingFromJava =
                typescriptFields.stream()
                        .filter(field ->
                                !javaFields.contains(field))
                        .toList();

        List<String> missingFromTypeScript =
                javaFields.stream()
                        .filter(field ->
                                !typescriptFields.contains(field))
                        .toList();

        if (missingFromJava.isEmpty()
                && missingFromTypeScript.isEmpty()) {
            return findings;
        }

        findings.add(
                """
                ### Cross-File Contract Mismatch

                TypeScript client fields:
                %s

                Java API fields:
                %s

                TypeScript fields not found in Java:
                %s

                Java fields not found in TypeScript:
                %s

                Risk:
                The client and API appear to use different response contracts.

                Recommended action:
                Align the API contract or introduce an explicit mapping.
                """
                        .formatted(
                                typescriptFields,
                                javaFields,
                                missingFromJava,
                                missingFromTypeScript));

        return findings;
    }

    private String findSource(
            Map<String, String> sources,
            String... extensions) {

        for (Map.Entry<String, String> entry :
                sources.entrySet()) {

            String fileName =
                    entry.getKey().toLowerCase();

            for (String extension : extensions) {

                if (fileName.endsWith(extension)) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    private List<String> extractTypeScriptFields(
            String source) {

        List<String> fields = new ArrayList<>();

        Matcher interfaceMatcher =
                TYPESCRIPT_INTERFACE.matcher(source);

        while (interfaceMatcher.find()) {

            String body =
                    interfaceMatcher.group(1);

            Matcher fieldMatcher =
                    TYPESCRIPT_FIELD.matcher(body);

            while (fieldMatcher.find()) {

                fields.add(
                        fieldMatcher.group(1));
            }
        }

        return fields.stream()
                .distinct()
                .toList();
    }

    private List<String> extractJavaFields(
            String source) {

        List<String> fields = new ArrayList<>();

        Matcher recordMatcher =
                JAVA_RECORD.matcher(source);

        while (recordMatcher.find()) {

            String body =
                    recordMatcher.group(1);

            Matcher fieldMatcher =
                    JAVA_FIELD.matcher(body);

            while (fieldMatcher.find()) {

                fields.add(
                        fieldMatcher.group(1));
            }
        }

        return fields.stream()
                .distinct()
                .toList();
    }
}
