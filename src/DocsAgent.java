import java.util.List;

/**
 * Generates useful documentation when the analyzer identifies
 * a documentation-related issue.
 */
public class DocsAgent implements SpecialistAgent {

    @Override
    public String name() {
        return "Documentation Agent";
    }

    @Override
    public boolean shouldRun(String sourceCode, List<Finding> findings) {
        return findings.stream()
                .anyMatch(f -> "Documentation".equals(f.getCategory()));
    }

    @Override
    public String execute(
            String className,
            String sourceCode,
            List<Finding> findings,
            ClaudeClient client) throws Exception {

        String system =
                "You are a Java technical writer. "
                        + "Add accurate and concise Javadoc to the class and public methods "
                        + "where documentation is missing. "
                        + "Keep the existing code and behaviour unchanged. "
                        + "Output the full modified Java source file. "
                        + "After the source code, add one line starting with "
                        + "'PLAIN_SUMMARY:' followed by a simple explanation of what the class does.";

        StringBuilder user =
                new StringBuilder("Class: " + className + "\n\n"
                        + "Source code:\n"
                        + sourceCode
                        + "\n\n"
                        + "Documentation findings:\n");

        for (Finding finding : findings) {
            if ("Documentation".equals(finding.getCategory())) {
                user.append("- ").append(finding).append("\n");
            }
        }

        return client.ask(system, user.toString());
    }
}