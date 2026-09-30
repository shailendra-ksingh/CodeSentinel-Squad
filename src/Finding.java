/**
 * Represents a single issue discovered by the StaticAnalyzer.
 * This is the unit of information the AgentOrchestrator reasons over
 * when deciding which follow-up actions (LLM calls) to trigger.
 */
public class Finding {

    public enum Severity { CRITICAL, MAJOR, MINOR }

    private final Severity severity;
    private final int lineNumber;
    private final String category;      // e.g. "Null Safety", "Resource Leak", "Security"
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

    @Override
    public String toString() {
        return String.format("Line %d [%s/%s]: %s", lineNumber, severity, category, description);
    }
}
