import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fast, deterministic and fully offline first pass over Java source code.
 *
 * This is intentionally lightweight and pattern based.
 * It finds candidate issues that can then be reviewed by specialist agents.
 *
 * It is not intended to replace tools such as SonarQube or SpotBugs.
 */
public class StaticAnalyzer {

    private static final Pattern BROAD_CATCH =
            Pattern.compile(
                    "catch\\s*\\(\\s*Exception\\s+\\w+\\s*\\)");

    private static final Pattern RESOURCE_CREATION =
            Pattern.compile(
                    "new\\s+(FileInputStream|FileOutputStream|"
                            + "FileReader|FileWriter|Socket|Connection)\\b");

    private static final Pattern HARD_CODED_SECRET =
            Pattern.compile(
                    "(?i)(password|secret|apikey|api_key)"
                            + "\\s*=\\s*\"[^\"]+\"");

    private static final Pattern SQL_CONCAT =
            Pattern.compile(
                    "(?i)(select|insert|update|delete)"
                            + "\\s.+\\+\\s*\\w+");

    private static final Pattern PUBLIC_FIELD =
            Pattern.compile(
                    "public\\s+(?!static final)"
                            + "\\w[\\w<>\\[\\]]*\\s+\\w+\\s*;");

    private static final Pattern TRANSACTIONAL_METHOD =
            Pattern.compile(
                    "public\\s+void\\s+\\w*"
                            + "(save|update|delete|persist)"
                            + "\\w*\\s*\\(");

    private static final Pattern CHAINED_GET =
            Pattern.compile(
                    "(\\w+)\\.get\\w+\\(\\)\\.\\w+\\(");

    private static final Pattern PUBLIC_METHOD =
            Pattern.compile(
                    "public\\s+[\\w<>\\[\\]]+"
                            + "\\s+(\\w+)\\s*\\(");

    public List<Finding> analyze(String sourceCode) {

        List<Finding> findings =
                new ArrayList<>();

        String[] lines =
                sourceCode.split("\\R", -1);

        for (int i = 0; i < lines.length; i++) {

            int lineNo = i + 1;
            String line = lines[i];

            // Broad exception handling
            if (BROAD_CATCH.matcher(line).find()) {

                findings.add(
                        new Finding(
                                Finding.Severity.MAJOR,
                                lineNo,
                                "Error Handling",
                                "Broad `catch (Exception e)` can hide "
                                        + "specific failures. Prefer handling "
                                        + "the expected exception types."));
            }

            // Resource leak
            if (RESOURCE_CREATION.matcher(line).find()
                    && !hasNearbyTryWithResources(lines, i)) {

                findings.add(
                        new Finding(
                                Finding.Severity.MAJOR,
                                lineNo,
                                "Resource Leak",
                                "Resource opened without nearby "
                                        + "try-with-resources; it may not "
                                        + "be closed when an exception occurs."));
            }

            // Hardcoded credentials
            if (HARD_CODED_SECRET.matcher(line).find()) {

                findings.add(
                        new Finding(
                                Finding.Severity.CRITICAL,
                                lineNo,
                                "Security",
                                "Possible hardcoded credential or "
                                        + "secret detected in source code."));
            }

            // SQL string concatenation
            if (SQL_CONCAT.matcher(line).find()) {

                findings.add(
                        new Finding(
                                Finding.Severity.CRITICAL,
                                lineNo,
                                "Security",
                                "SQL statement appears to be built "
                                        + "using string concatenation. "
                                        + "Prefer PreparedStatement or "
                                        + "parameterized queries."));
            }

            // Public mutable field
            if (PUBLIC_FIELD.matcher(line).find()) {

                findings.add(
                        new Finding(
                                Finding.Severity.MINOR,
                                lineNo,
                                "Encapsulation",
                                "Public mutable field exposes internal "
                                        + "state directly. Consider a "
                                        + "private field with an accessor."));
            }

            // Possible missing transaction boundary
            if (TRANSACTIONAL_METHOD.matcher(line).find()
                    && !hasTransactionalAnnotationAbove(lines, i)) {

                findings.add(
                        new Finding(
                                Finding.Severity.MAJOR,
                                lineNo,
                                "JEE Anti-pattern",
                                "Method name suggests a data-mutating "
                                        + "operation but no @Transactional "
                                        + "annotation was found nearby."));
            }

            // Possible null dereference
            Matcher nullMatcher =
                    CHAINED_GET.matcher(line);

            if (nullMatcher.find()) {

                findings.add(
                        new Finding(
                                Finding.Severity.CRITICAL,
                                lineNo,
                                "Null Safety",
                                "Chained call on `"
                                        + nullMatcher.group(1)
                                        + ".get...()` result without a "
                                        + "visible null check. This may "
                                        + "cause NullPointerException."));
            }
        }

        addDocumentationFindings(
                sourceCode,
                findings);

        addArchitectureFinding(
                sourceCode,
                lines,
                findings);

        return findings;
    }

    /**
     * Checks a small area around the resource creation.
     *
     * The old implementation checked the complete source file for
     * "try (". That could incorrectly hide a resource leak just because
     * another unrelated method used try-with-resources.
     */
    private boolean hasNearbyTryWithResources(
            String[] lines,
            int lineIndex) {

        int start =
                Math.max(0, lineIndex - 3);

        int end =
                Math.min(lines.length, lineIndex + 4);

        for (int i = start; i < end; i++) {

            if (lines[i].contains("try (")) {
                return true;
            }
        }

        return false;
    }

    private boolean hasTransactionalAnnotationAbove(
            String[] lines,
            int lineIndex) {

        int start =
                Math.max(0, lineIndex - 3);

        for (int i = start; i < lineIndex; i++) {

            if (lines[i].contains("@Transactional")) {
                return true;
            }
        }

        return false;
    }

    private void addDocumentationFindings(
            String sourceCode,
            List<Finding> findings) {

        Matcher matcher =
                PUBLIC_METHOD.matcher(sourceCode);

        while (matcher.find()) {

            int sourceIndex =
                    matcher.start();

            String before =
                    sourceCode.substring(
                            Math.max(0, sourceIndex - 120),
                            sourceIndex);

            if (!before.contains("/**")) {

                int lineNo =
                        countLines(
                                sourceCode,
                                sourceIndex);

                findings.add(
                        new Finding(
                                Finding.Severity.MINOR,
                                lineNo,
                                "Documentation",
                                "Public method `"
                                        + matcher.group(1)
                                        + "` has no Javadoc comment."));
            }
        }
    }

    private void addArchitectureFinding(
            String sourceCode,
            String[] lines,
            List<Finding> findings) {

        int publicMethodCount = 0;

        Matcher matcher =
                Pattern.compile(
                                "public\\s+[\\w<>\\[\\]]+"
                                        + "\\s+\\w+\\s*\\(")
                        .matcher(sourceCode);

        while (matcher.find()) {
            publicMethodCount++;
        }

        /*
         * This is only a routing signal for ArchitectureAgent.
         * It is not intended to be a precise God Class measurement.
         */
        if (publicMethodCount > 6
                || lines.length > 120) {

            findings.add(
                    new Finding(
                            Finding.Severity.MAJOR,
                            1,
                            "Architecture",
                            "Class has "
                                    + publicMethodCount
                                    + " public method(s) across "
                                    + lines.length
                                    + " lines. This may indicate "
                                    + "too many responsibilities in one class."));
        }
    }

    private int countLines(
            String text,
            int upToIndex) {

        int count = 1;

        for (int i = 0;
             i < upToIndex && i < text.length();
             i++) {

            if (text.charAt(i) == '\n') {
                count++;
            }
        }

        return count;
    }
}