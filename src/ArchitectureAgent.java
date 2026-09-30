import java.util.List;

/**
 * Specialist agent: looks beyond line-level bugs at class-level design —
 * the kind of thing a Technical Architect would flag in a design review,
 * not just a code review (e.g. "God Class" smells, mixed responsibilities).
 *
 * Only runs when the offline StaticAnalyzer already suspects a design
 * smell (see StaticAnalyzer's "Architecture" finding), so an LLM call is
 * never spent on a small, well-structured class.
 */
public class ArchitectureAgent implements SpecialistAgent {

    @Override
    public String name() { return "Architecture Review Agent"; }

    @Override
    public boolean shouldRun(String sourceCode, List<Finding> findings) {
        return findings.stream().anyMatch(f -> f.getCategory().equals("Architecture"));
    }

    @Override
    public String execute(String className, String sourceCode, List<Finding> findings, ClaudeClient client) throws Exception {
        String system = "You are a Technical Architect performing a design-level review (not a line-by-line "
                + "code review). Comment on responsibilities, coupling and cohesion, and whether this class "
                + "should be split. Keep it to 3-5 sentences, practical and specific to Java/JEE.";
        String archFindings = findings.stream()
                .filter(f -> f.getCategory().equals("Architecture"))
                .map(Finding::toString)
                .reduce("", (a, b) -> a + "\n" + b);
        String user = "Class: " + className + "\n\n" + sourceCode
                + "\n\nStatic analysis already suspects a design smell:\n" + archFindings;

        return client.ask(system, user);
    }
}
