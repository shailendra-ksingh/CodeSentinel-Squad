package com.soprasteria.aibydesign.codesentinel;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Runs the complete review flow for one source file.
 *
 * The pipeline analyzes the file, checks previous findings, routes the
 * review to relevant specialists, writes the report and saves the findings
 * for future reviews.
 *
 * The original source is used for local analysis. A redacted copy is used
 * whenever source code is passed to AI services.
 */
public class ReviewPipeline {

    private final LlmClient llm;
    private final MemoryStore memory;
    private final Path outputRoot;
    private final PrintStream console;

    public ReviewPipeline(
            LlmClient llm,
            MemoryStore memory,
            Path outputRoot,
            PrintStream console) {

        this.llm = llm;
        this.memory = memory;
        this.outputRoot = outputRoot;
        this.console = console;
    }

    public SquadResult review(Path sourcePath) throws Exception {

        String fileName = sourcePath.getFileName().toString();
        Language language = LanguageDetector.detect(sourcePath);
        String artifactName = stripExtension(fileName);

        // Keep the original source for local deterministic analysis.
        String sourceCode = Files.readString(sourcePath);

        // Never send the original source directly to an AI service.
        String aiSafeSource = SecretRedactor.redact(sourceCode);

        console.println();
        console.println("======================================");
        console.println(
                "  CodeSentinel Squad - "
                        + fileName
                        + " ["
                        + language.displayName()
                        + "]");
        console.println("======================================");

        // 1. Run deterministic analysis.
        console.println("[1/5] Running static analysis...");

        List<Finding> findings = language == Language.JAVA
                ? new StaticAnalyzer().analyze(sourceCode)
                : new PolyglotAnalyzer().analyze(sourceCode, language);

        console.println("      Findings: " + findings.size());

        // 2. Compare findings with the previous review.
        console.println("[2/5] Checking previous review history...");

        List<Finding> previousFindings =
                memory.loadPrevious(artifactName);

        List<Finding> recurring =
                memory.recurring(findings, previousFindings);

        console.println(
                "      Recurring issues: "
                        + (recurring.isEmpty()
                        ? "none"
                        : recurring.size()));

        // 3. Select the specialist agents.
        console.println("[3/5] Coordinator routing work...");

        List<SpecialistAgent> specialists = List.of(
                new SecurityAgent(),
                new TestAgent(),
                new DocsAgent(),
                new ArchitectureAgent());

        /*
         * The Coordinator receives the redacted source.
         * Original findings are retained locally, while the Coordinator
         * creates an AI-safe copy before passing findings to specialists.
         */
        SquadResult result =
                new CoordinatorAgent(
                        specialists,
                        new CriticAgent(),
                        llm)
                        .run(
                                artifactName,
                                aiSafeSource,
                                findings,
                                recurring);

        // 4. Show the review status.
        console.println("[4/5] Review execution...");

        if (result.safeMode) {
            console.println(
                    "      SAFE MODE: ANTHROPIC_API_KEY not configured "
                            + "- static analysis only.");
        } else {
            console.println(
                    "      Dispatched: "
                            + formatList(result.dispatchedAgents));

            console.println(
                    "      Skipped:    "
                            + formatList(result.skippedAgents));

            if (!result.criticTranscript.isEmpty()) {
                console.println(
                        "      Critic:     "
                                + (result.testsApproved
                                ? "APPROVED"
                                : "REVIEW REQUIRED"));
            }
        }

        if (!result.warnings.isEmpty()) {
            console.println(
                    "      Warnings:   "
                            + result.warnings.size());
        }

        // 5. Write the report and remember the current findings.
        console.println("[5/5] Writing report...");

        Path outputDir = outputRoot.resolve(artifactName);

        new ReportGenerator().writeAll(
                artifactName,
                fileName,
                language,
                result,
                outputDir);

        memory.save(artifactName, findings);

        console.println(
                "      Report: "
                        + outputDir
                        .resolve("review.md")
                        .toAbsolutePath());

        return result;
    }

    private static String stripExtension(String fileName) {

        int dot = fileName.lastIndexOf('.');

        return dot > 0
                ? fileName.substring(0, dot)
                : fileName;
    }

    private static String formatList(List<String> values) {

        return values == null || values.isEmpty()
                ? "none"
                : String.join(", ", values);
    }
}