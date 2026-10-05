package com.soprasteria.aibydesign.codesentinel;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fast, deterministic and fully offline first pass over Java source code.
 *
 * Deliberately lightweight and pattern based: it produces candidate issues
 * (the "evidence") that drive agent routing. It does not replace SonarQube,
 * SpotBugs or PMD.
 */
public class StaticAnalyzer {

    private static final Pattern BROAD_CATCH =
            Pattern.compile("catch\\s*\\(\\s*(?:final\\s+)?(?:Exception|Throwable)\\s+\\w+\\s*\\)");

    private static final Pattern RESOURCE_CREATION =
            Pattern.compile("new\\s+(FileInputStream|FileOutputStream|FileReader|FileWriter|Socket)\\b"
                    + "|\\.getConnection\\s*\\(");

    private static final Pattern HARD_CODED_SECRET =
            Pattern.compile("(?i)\\b\\w*(password|passwd|secret|api_?key|token)\\w*\\s*=\\s*\"[^\"]+\"");

    /** A string literal that starts like SQL and is then concatenated with "+". */
    private static final Pattern SQL_CONCAT =
            Pattern.compile("(?i)\"\\s*(select\\s|insert\\s+into\\s|update\\s|delete\\s+from\\s)[^\"]*\"\\s*\\+");

    /** Non-final, non-private field declaration; an initializer is allowed. */
    private static final Pattern PUBLIC_FIELD =
            Pattern.compile("^\\s*public\\s+(?!static\\s+final\\b|final\\b)(?:static\\s+)?[\\w<>\\[\\],\\s]+?\\s+\\w+\\s*(=[^;]*)?;");

    private static final Pattern TRANSACTIONAL_METHOD =
            Pattern.compile("public\\s+(?:\\w+\\s+)?void\\s+\\w*(save|update|delete|persist)\\w*\\s*\\(", Pattern.CASE_INSENSITIVE);

    private static final Pattern CHAINED_GET =
            Pattern.compile("(\\w+)\\.get\\w+\\(\\)\\.\\w+\\(");

    /** Public method declaration (modifiers such as static/final/synchronized allowed). */
    private static final Pattern PUBLIC_METHOD =
            Pattern.compile("\\bpublic\\s+(?:(?:static|final|synchronized|abstract|default)\\s+)*"
                    + "(?:<[^>]+>\\s*)?[\\w<>\\[\\],.?]+\\s+(\\w+)\\s*\\(");

    static final int GOD_CLASS_METHODS = 6;
    static final int GOD_CLASS_LINES = 120;

    /** True when the source declares at least one public method. */
    public static boolean hasPublicMethod(String sourceCode) {
        return sourceCode != null && PUBLIC_METHOD.matcher(sourceCode).find();
    }

    public List<Finding> analyze(String sourceCode) {

        List<Finding> findings = new ArrayList<>();
        String[] lines = sourceCode.split("\\R", -1);

        for (int i = 0; i < lines.length; i++) {

            int lineNo = i + 1;
            String line = lines[i];

            if (isCommentLine(line)) {
                continue;
            }

            if (BROAD_CATCH.matcher(line).find()) {
                findings.add(new Finding(Finding.Severity.MAJOR, lineNo, "Error Handling",
                        "Broad `catch (Exception e)` can hide specific failures. "
                                + "Prefer handling the expected exception types."));
            }

            if (RESOURCE_CREATION.matcher(line).find() && !hasNearbyTryWithResources(lines, i)) {
                findings.add(new Finding(Finding.Severity.MAJOR, lineNo, "Resource Leak",
                        "Resource opened without nearby try-with-resources; it may not "
                                + "be closed when an exception occurs."));
            }

            if (HARD_CODED_SECRET.matcher(line).find()) {
                findings.add(new Finding(Finding.Severity.CRITICAL, lineNo, "Security",
                        "Possible hardcoded credential or secret detected in source code."));
            }

            if (SQL_CONCAT.matcher(line).find()) {
                findings.add(new Finding(Finding.Severity.CRITICAL, lineNo, "Security",
                        "SQL statement appears to be built using string concatenation. "
                                + "Prefer PreparedStatement or parameterized queries."));
            }

            if (PUBLIC_FIELD.matcher(line).find() && !line.contains("(")) {
                findings.add(new Finding(Finding.Severity.MINOR, lineNo, "Encapsulation",
                        "Public mutable field exposes internal state directly. "
                                + "Consider a private field with an accessor."));
            }

            if (TRANSACTIONAL_METHOD.matcher(line).find() && !hasTransactionalAnnotationAbove(lines, i)) {
                findings.add(new Finding(Finding.Severity.MAJOR, lineNo, "JEE Anti-pattern",
                        "Method name suggests a data-mutating operation but no "
                                + "@Transactional annotation was found nearby."));
            }

            Matcher chained = CHAINED_GET.matcher(line);
            if (chained.find()) {
                findings.add(new Finding(Finding.Severity.CRITICAL, lineNo, "Null Safety",
                        "Chained call on `" + chained.group(1) + ".get...()` result without a "
                                + "visible null check. This may cause NullPointerException."));
            }
        }

        addDocumentationFindings(lines, findings);
        addArchitectureFinding(sourceCode, lines, findings);

        return findings;
    }

    private static boolean isCommentLine(String line) {
        String t = line.stripLeading();
        return t.startsWith("//") || t.startsWith("*") || t.startsWith("/*");
    }

    /** Looks a few lines either side of the resource creation for "try (". */
    private boolean hasNearbyTryWithResources(String[] lines, int lineIndex) {
        int start = Math.max(0, lineIndex - 3);
        int end = Math.min(lines.length, lineIndex + 4);
        for (int i = start; i < end; i++) {
            if (lines[i].matches(".*\\btry\\s*\\(.*")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasTransactionalAnnotationAbove(String[] lines, int lineIndex) {
        for (int i = Math.max(0, lineIndex - 3); i < lineIndex; i++) {
            if (lines[i].contains("@Transactional")) {
                return true;
            }
        }
        return false;
    }

    /**
     * A public method is documented when the nearest non-annotation, non-blank
     * line above it ends a Javadoc block. (The previous version only looked
     * 120 characters back, which wrongly flagged methods with long Javadoc.)
     */
    private void addDocumentationFindings(String[] lines, List<Finding> findings) {

        for (int i = 0; i < lines.length; i++) {

            if (isCommentLine(lines[i])) {
                continue;
            }
            Matcher m = PUBLIC_METHOD.matcher(lines[i]);
            if (!m.find()) {
                continue;
            }

            if (!hasJavadocAbove(lines, i)) {
                findings.add(new Finding(Finding.Severity.MINOR, i + 1, "Documentation",
                        "Public method `" + m.group(1) + "` has no Javadoc comment."));
            }
        }
    }

    private boolean hasJavadocAbove(String[] lines, int index) {
        for (int i = index - 1; i >= 0; i--) {
            String t = lines[i].strip();
            if (t.isEmpty() || t.startsWith("@")) {
                continue;
            }
            return t.endsWith("*/");
        }
        return false;
    }

    private void addArchitectureFinding(String sourceCode, String[] lines, List<Finding> findings) {

        int publicMethodCount = 0;
        Matcher m = PUBLIC_METHOD.matcher(sourceCode);
        while (m.find()) {
            publicMethodCount++;
        }

        // A routing signal for the ArchitectureAgent, not a precise God Class metric.
        if (publicMethodCount > GOD_CLASS_METHODS || lines.length > GOD_CLASS_LINES) {
            findings.add(new Finding(Finding.Severity.MAJOR, 1, "Architecture",
                    "Class has " + publicMethodCount + " public method(s) across " + lines.length
                            + " lines. This may indicate too many responsibilities in one class."));
        }
    }
}
