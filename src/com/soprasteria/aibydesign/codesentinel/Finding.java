package com.soprasteria.aibydesign.codesentinel;

/**
 * A single issue discovered by the {@link StaticAnalyzer}.
 *
 * Findings are the shared evidence that drives routing in the
 * {@link CoordinatorAgent}, and the unit stored by the {@link MemoryStore}.
 */
public class Finding {

    public enum Severity { CRITICAL, MAJOR, MINOR }

    private final Severity severity;
    private final int lineNumber;
    private final String category;      // e.g. "Security", "Null Safety", "Resource Leak"
    private final String description;

    public Finding(Severity severity, int lineNumber, String category, String description) {
        this.severity = severity;
        this.lineNumber = lineNumber;
        this.category = category;
        this.description = description;
    }

    public Severity getSeverity() { return severity; }
    public int getLineNumber() { return lineNumber; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }

    /**
     * Identity of the issue independent of where it sits in the file.
     * Used to detect recurring findings across reviews.
     */
    public String signature() {
        return category + "::" + description.replace("|", "/");
    }

    @Override
    public String toString() {
        return String.format("Line %d [%s/%s]: %s", lineNumber, severity, category, description);
    }
}
