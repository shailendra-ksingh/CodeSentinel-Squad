package com.soprasteria.aibydesign.codesentinel;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Lightweight offline analyzer for non-Java assets. It intentionally stays
 * heuristic: its job is to produce review evidence and routing signals, not
 * replace language compilers or mature SAST tools.
 */
public class PolyglotAnalyzer {
    private static final Pattern SECRET = Pattern.compile(
            "(?i)(password|passwd|secret|api[_-]?key|token)\\s*[:=]\\s*['\"]([^'\"]{4,})['\"]");
    private static final Pattern SQL_CONCAT = Pattern.compile(
            "(?i)(SELECT|INSERT\\s+INTO|UPDATE|DELETE\\s+FROM)[^;\\n]*(\\+|\\$\\{|f['\"])" );
    private static final Pattern PY_BARE_EXCEPT = Pattern.compile("(?m)^\\s*except\\s*:");
    private static final Pattern PY_EVAL = Pattern.compile("\\b(eval|exec)\\s*\\(");
    private static final Pattern JS_ANY = Pattern.compile("\\bany\\b");
    private static final Pattern JS_EVAL = Pattern.compile("\\beval\\s*\\(");
    private static final Pattern JS_INNER_HTML = Pattern.compile("\\.innerHTML\\s*=");
    private static final Pattern SQL_SELECT_STAR = Pattern.compile("(?i)\\bselect\\s+\\*\\s+from\\b");
    private static final Pattern YAML_PRIVILEGED = Pattern.compile("(?m)^\\s*privileged\\s*:\\s*true\\s*$");
    private static final Pattern YAML_SECRET = Pattern.compile("(?i)^\\s*(password|secret|token|apiKey)\\s*:");
    private static final Pattern JSON_SECRET = Pattern.compile("(?i)\"(password|secret|token|apiKey)\"\\s*:\\s*\"[^\"]+\"");

    public List<Finding> analyze(String source, Language language) {
        List<Finding> findings = new ArrayList<>();
        String[] lines = source.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int n = i + 1;
            if (SECRET.matcher(line).find() || (language == Language.YAML && YAML_SECRET.matcher(line).find())) {
                findings.add(new Finding(Finding.Severity.CRITICAL, n, "Security",
                        "Possible hardcoded credential or secret detected in " + language.displayName() + " source."));
            }
            if (language == Language.PYTHON) {
                if (PY_BARE_EXCEPT.matcher(line).find()) findings.add(new Finding(Finding.Severity.MAJOR, n, "Error Handling",
                        "Bare `except:` catches every exception and can hide unexpected failures."));
                if (PY_EVAL.matcher(line).find()) findings.add(new Finding(Finding.Severity.CRITICAL, n, "Security",
                        "Dynamic eval/exec can execute attacker-controlled input when data reaches this path."));
            }
            if (language == Language.JAVASCRIPT || language == Language.TYPESCRIPT) {
                if (JS_EVAL.matcher(line).find()) findings.add(new Finding(Finding.Severity.CRITICAL, n, "Security",
                        "Dynamic eval() can execute attacker-controlled JavaScript."));
                if (JS_INNER_HTML.matcher(line).find()) findings.add(new Finding(Finding.Severity.MAJOR, n, "Security",
                        "Direct innerHTML assignment can create an XSS sink when the value is not trusted."));
                if (language == Language.TYPESCRIPT && JS_ANY.matcher(line).find()) findings.add(new Finding(Finding.Severity.MINOR, n, "Type Safety",
                        "TypeScript `any` weakens compile-time guarantees at this boundary."));
            }
            if (language == Language.SQL) {
                if (SQL_CONCAT.matcher(line).find()) findings.add(new Finding(Finding.Severity.CRITICAL, n, "Security",
                        "SQL appears to be assembled dynamically; prefer parameterized queries."));
                if (SQL_SELECT_STAR.matcher(line).find()) findings.add(new Finding(Finding.Severity.MINOR, n, "Maintainability",
                        "SELECT * couples the query to schema changes and may fetch unnecessary columns."));
            }
            if (language == Language.YAML && YAML_PRIVILEGED.matcher(line).find()) findings.add(new Finding(Finding.Severity.CRITICAL, n, "Security",
                    "Container is configured as privileged; verify that host-level access is genuinely required."));
            if (language == Language.JSON && JSON_SECRET.matcher(line).find()) findings.add(new Finding(Finding.Severity.CRITICAL, n, "Security",
                    "Possible hardcoded secret detected in JSON configuration."));
        }
        if (language != Language.JSON && language != Language.YAML && lines.length > 250) {
            findings.add(new Finding(Finding.Severity.MINOR, 1, "Architecture",
                    language.displayName() + " file is large enough to merit a responsibility review."));
        }
        return findings;
    }
}
