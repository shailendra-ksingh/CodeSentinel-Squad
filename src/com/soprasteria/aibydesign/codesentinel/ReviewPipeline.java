package com.soprasteria.aibydesign.codesentinel;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The end-to-end review of one Java file:
 * Analyze -> Remember (compare) -> Route/Specialize/Critique -> Report -> Remember (save).
 *
 * Separated from {@link Main} so the whole flow can be tested with a stub
 * {@link LlmClient} and no network access.
 */
public class ReviewPipeline {

    private final LlmClient llm;
    private final MemoryStore memory;
    private final Path outputRoot;
    private final PrintStream console;

    public ReviewPipeline(LlmClient llm, MemoryStore memory, Path outputRoot, PrintStream console) {
        this.llm = llm;
        this.memory = memory;
        this.outputRoot = outputRoot;
        this.console = console;
    }

    public SquadResult review(Path sourcePath) throws Exception {

        String fileName = sourcePath.getFileName().toString();
        Language language = LanguageDetector.detect(sourcePath);
        String artifactName = stripExtension(fileName);
        String sourceCode = Files.readString(sourcePath);

        console.println();
        console.println("======================================");
        console.println("  CodeSentinel Squad - " + fileName + " [" + language.displayName() + "]");
        console.println("======================================");

        console.println("[1/5] Running static analysis...");
        List<Finding> findings = language == Language.JAVA
                ? new StaticAnalyzer().analyze(sourceCode)
                : new PolyglotAnalyzer().analyze(sourceCode, language);
        console.println("      Findings: " + findings.size());

        console.println("[2/5] Checking previous review history...");
        List<Finding> recurring = memory.recurring(findings, memory.loadPrevious(artifactName));
        console.println("      Recurring issues: " + (recurring.isEmpty() ? "none" : recurring.size()));

        console.println("[3/5] Coordinator routing work...");
        List<SpecialistAgent> specialists = List.of(
                new SecurityAgent(), new TestAgent(), new DocsAgent(), new ArchitectureAgent());

        SquadResult result = new CoordinatorAgent(specialists, new CriticAgent(), llm)
                .run(artifactName, sourceCode, findings, recurring);

        console.println("[4/5] Review execution...");
        if (result.safeMode) {
            console.println("      SAFE MODE: ANTHROPIC_API_KEY not configured - static analysis only.");
        } else {
            console.println("      Dispatched: " + formatList(result.dispatchedAgents));
            console.println("      Skipped:    " + formatList(result.skippedAgents));
            if (!result.criticTranscript.isEmpty()) {
                console.println("      Critic:     " + (result.testsApproved ? "APPROVED" : "REVIEW REQUIRED"));
            }
        }
        if (!result.warnings.isEmpty()) {
            console.println("      Warnings:   " + result.warnings.size());
        }

        console.println("[5/5] Writing report...");
        Path outputDir = outputRoot.resolve(artifactName);
        new ReportGenerator().writeAll(artifactName, fileName, language, result, outputDir);
        memory.save(artifactName, findings);

        console.println("      Report: " + outputDir.resolve("review.md").toAbsolutePath());
        return result;
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static String formatList(List<String> values) {
        return (values == null || values.isEmpty()) ? "none" : String.join(", ", values);
    }
}
