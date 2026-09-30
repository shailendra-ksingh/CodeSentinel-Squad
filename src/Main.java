import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Command-line entry point for CodeSentinel Squad.
 *
 * Usage:
 *     java -jar codesentinel.jar <path-to-java-file>
 */
public class Main {

    public static void main(String[] args) throws Exception {

        if (args.length != 1) {
            System.out.println(
                    "Usage: java -jar codesentinel.jar <path-to-java-file>");
            System.exit(1);
        }

        Path sourcePath = Path.of(args[0]);

        if (!Files.exists(sourcePath)) {
            System.err.println(
                    "File not found: " + sourcePath);
            System.exit(1);
        }

        if (!sourcePath.toString().endsWith(".java")) {
            System.err.println(
                    "Please provide a Java source file.");
            System.exit(1);
        }

        String className =
                sourcePath.getFileName()
                        .toString()
                        .replace(".java", "");

        String sourceCode =
                Files.readString(sourcePath);

        System.out.println();
        System.out.println("======================================");
        System.out.println("       CodeSentinel Squad");
        System.out.println("======================================");
        System.out.println(
                "Reviewing: " + className + ".java");
        System.out.println();

        /*
         * Step 1:
         * Run deterministic analysis first.
         *
         * This gives all agents the same evidence and also allows
         * CodeSentinel to work without an AI API key.
         */
        System.out.println("[1/5] Running static analysis...");

        StaticAnalyzer analyzer =
                new StaticAnalyzer();

        List<Finding> findings =
                analyzer.analyze(sourceCode);

        System.out.println(
                "      Findings: " + findings.size());

        /*
         * Step 2:
         * Check the local memory from the previous review.
         */
        System.out.println(
                "[2/5] Checking previous review history...");

        MemoryStore memory =
                new MemoryStore(Path.of("memory"));

        List<Finding> previousFindings =
                memory.loadPrevious(className);

        List<Finding> recurring =
                memory.recurring(
                        findings,
                        previousFindings);

        if (recurring.isEmpty()) {

            System.out.println(
                    "      Recurring issues: none");

        } else {

            System.out.println(
                    "      Recurring issues: "
                            + recurring.size());
        }

        /*
         * Step 3:
         * Create the specialist squad.
         */
        System.out.println(
                "[3/5] Coordinator routing work...");

        String apiKey =
                System.getenv("ANTHROPIC_API_KEY");

        ClaudeClient claudeClient =
                new ClaudeClient(apiKey);

        List<SpecialistAgent> specialists =
                List.of(
                        new SecurityAgent(),
                        new TestAgent(),
                        new DocsAgent(),
                        new ArchitectureAgent());

        CoordinatorAgent coordinator =
                new CoordinatorAgent(
                        specialists,
                        new CriticAgent(),
                        claudeClient);

        SquadResult result =
                coordinator.run(
                        className,
                        sourceCode,
                        findings,
                        recurring);

        /*
         * Step 4:
         * Display what happened.
         */
        System.out.println(
                "[4/5] Review execution...");

        if (result.safeMode) {

            System.out.println(
                    "      SAFE MODE: AI API key not configured.");
            System.out.println(
                    "      Static analysis completed successfully.");

        } else {

            System.out.println(
                    "      Dispatched: "
                            + formatList(
                            result.dispatchedAgents));

            System.out.println(
                    "      Skipped: "
                            + formatList(
                            result.skippedAgents));

            if (!result.criticTranscript.isEmpty()) {

                System.out.println(
                        "      Critic loop: "
                                + (result.testsApproved
                                ? "APPROVED"
                                : "REVIEW REQUIRED"));
            }
        }

        if (!result.warnings.isEmpty()) {

            System.out.println(
                    "      Warnings: "
                            + result.warnings.size());
        }

        /*
         * Step 5:
         * Save memory and create the final report.
         */
        System.out.println(
                "[5/5] Creating report...");

        memory.save(
                className,
                findings);

        Path outputDir =
                Path.of("output");

        new ReportGenerator()
                .writeAll(
                        className,
                        result,
                        outputDir);

        System.out.println();
        System.out.println("Review complete.");
        System.out.println(
                "Report: "
                        + outputDir
                        .resolve("review.md")
                        .toAbsolutePath());
        System.out.println();
    }

    private static String formatList(
            List<String> values) {

        if (values == null || values.isEmpty()) {
            return "none";
        }

        return String.join(", ", values);
    }
}