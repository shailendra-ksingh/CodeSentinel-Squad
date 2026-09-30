import java.util.List;
import java.util.stream.Collectors;

/**
 * Specialist agent: deep-dives on Security-category findings only.
 * Deliberately narrow scope — it does one thing (security reasoning) and
 * does it well, rather than being folded into a generic all-purpose reviewer.
 */
public class SecurityAgent implements SpecialistAgent {

    @Override
    public String name() { return "Security Agent"; }

    @Override
    public boolean shouldRun(String sourceCode, List<Finding> findings) {
        return findings.stream().anyMatch(f -> f.getCategory().equals("Security"));
    }

    @Override
    public String execute(String className, String sourceCode, List<Finding> findings, ClaudeClient client) throws Exception {
        String securityFindings = findings.stream()
                .filter(f -> f.getCategory().equals("Security"))
                .map(Finding::toString)
                .collect(Collectors.joining("\n"));

        String system = "You are an application security specialist reviewing Java/JEE code. "
                + "For each finding, explain the realistic exploit scenario in 1-2 sentences, "
                + "rate business risk as Low/Medium/High/Critical, and give the concrete fixed code.";
        String user = "Class: " + className + "\n\nSource:\n" + sourceCode
                + "\n\nSecurity findings to address:\n" + securityFindings;

        return client.ask(system, user);
    }
}
