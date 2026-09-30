import java.util.List;

/**
 * Generates JUnit 5 tests for issues found by the static analyzer.
 *
 * The agent is only called when there is a finding that can reasonably
 * be covered by a test. This avoids generating tests for every Java class.
 */
public class TestAgent implements SpecialistAgent {

    @Override
    public String name() {
        return "Test Generation Agent";
    }

    @Override
    public boolean shouldRun(String sourceCode, List<Finding> findings) {
        boolean hasPublicMethod = sourceCode.contains("public ");

        boolean hasTestableFinding = findings.stream().anyMatch(f ->
                switch (f.getCategory()) {
                    case "Security",
                         "Null Safety",
                         "Error Handling",
                         "Resource Leak",
                         "JEE Anti-pattern" -> true;
                    default -> false;
                });

        return hasPublicMethod && hasTestableFinding;
    }

    @Override
    public String execute(
            String className,
            String sourceCode,
            List<Finding> findings,
            ClaudeClient client) throws Exception {

        return execute(className, sourceCode, findings, client, null);
    }

    /**
     * Generates a new test version using feedback from the Critic.
     */
    public String execute(
            String className,
            String sourceCode,
            List<Finding> findings,
            ClaudeClient client,
            String criticFeedback) throws Exception {

        String system =
                "You are a Java testing expert. Generate a complete JUnit 5 test class. "
                        + "The tests must focus on the issues listed below. "
                        + "Use meaningful assertions such as assertEquals, assertTrue, "
                        + "assertFalse, assertThrows, assertNotNull or Mockito verify when appropriate. "
                        + "Output ONLY valid Java code. Do not use markdown fences.";

        StringBuilder user = new StringBuilder();

        user.append("Generate JUnit 5 tests for this class:\n")
                .append(className)
                .append("\n\n");

        user.append("Source code:\n")
                .append(sourceCode)
                .append("\n\n");

        user.append("Issues the tests should specifically cover:\n");

        for (Finding finding : findings) {
            user.append("- ")
                    .append(finding)
                    .append("\n");
        }

        if (criticFeedback != null && !criticFeedback.isBlank()) {
            user.append("\nThe previous test version was reviewed by another agent.\n");
            user.append("Reviewer feedback:\n")
                    .append(criticFeedback)
                    .append("\n\n");

            user.append("Create a revised version that addresses this feedback.\n");
        }

        return client.ask(system, user.toString());
    }
}