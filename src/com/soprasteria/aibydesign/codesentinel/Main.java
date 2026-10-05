package com.soprasteria.aibydesign.codesentinel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Command-line entry point.
 *
 * Usage:
 *     java -jar codesentinel.jar
 *     java -jar codesentinel.jar <file-or-directory> [more paths...]
 *
 * If no path is supplied, the default source directory is "src".
 *
 * Environment:
 *     ANTHROPIC_API_KEY        enables AI mode (absent = Safe Mode)
 *     CODESENTINEL_MODEL       optional model override
 *     CODESENTINEL_OUTPUT_DIR  report folder   (default: output)
 *     CODESENTINEL_MEMORY_DIR  history folder  (default: memory)
 */
public class Main {

    public static void main(String[] args) throws Exception {

        /*
         * No argument = review the default "src" directory.
         * This makes the CLI easier to use:
         *
         *     java -jar codesentinel.jar
         */
        String[] inputPaths = args.length == 0
                ? new String[]{"src"}
                : args;

        List<Path> files = new ArrayList<>();

        for (String inputPath : inputPaths) {
            Path path = Path.of(inputPath);

            if (!Files.exists(path)) {
                System.err.println("Not found: " + path);
                System.exit(2);
            }

            collectSourceFiles(path, files);
        }

        if (files.isEmpty()) {
            System.err.println("No supported source/configuration files found.");
            System.exit(2);
        }

        LlmClient llm = new ClaudeClient(System.getenv("ANTHROPIC_API_KEY"));
        MemoryStore memory =
                new MemoryStore(Path.of(envOr("CODESENTINEL_MEMORY_DIR", "memory")));
        Path outputRoot =
                Path.of(envOr("CODESENTINEL_OUTPUT_DIR", "output"));

        ReviewPipeline pipeline =
                new ReviewPipeline(llm, memory, outputRoot, System.out);

        int failed = 0;
        Map<String, SquadResult> results = new LinkedHashMap<>();

        for (Path file : files) {
            try {
                results.put(file.toString(), pipeline.review(file));
            } catch (Exception e) {
                failed++;
                System.err.println(
                        "Review of " + file + " failed: " + e.getMessage());
            }
        }

        /*
         * Run project-level integration when more than one file
         * was successfully reviewed.
         */
        if (results.size() > 1) {
            try {
                ProjectIntegrationAgent integration =
                        new ProjectIntegrationAgent();

                Map<String, String> sources = new LinkedHashMap<>();

                for (Path file : files) {
                    if (results.containsKey(file.toString())) {
                        sources.put(
                                file.toString(),
                                SecretRedactor.redact(Files.readString(file))
                        );
                    }
                }

                String projectAnalysis = integration.shouldRun(files)
                        ? (llm.isConfigured()
                        ? integration.execute(sources, results, llm)
                        : integration.safeModeSummary(results))
                        : "No project-level review required.";

                new ProjectReportGenerator().write(
                        files,
                        results,
                        projectAnalysis,
                        outputRoot.resolve("project")
                );

                System.out.println(
                        "Project report: "
                                + outputRoot
                                .resolve("project/project-review.md")
                                .toAbsolutePath()
                );

            } catch (Exception e) {
                failed++;
                System.err.println(
                        "Project integration review failed: "
                                + e.getMessage());
            }
        }

        System.out.println();
        System.out.println(
                "Reviewed "
                        + (files.size() - failed)
                        + " of "
                        + files.size()
                        + " file(s)."
        );

        if (failed > 0) {
            System.exit(1);
        }
    }

    /**
     * Collect all supported source/configuration files.
     *
     * Supports both:
     * - a single file
     * - a directory (including the default "src" directory)
     */
    private static void collectSourceFiles(
            Path path,
            List<Path> out) throws IOException {

        if (Files.isDirectory(path)) {

            try (Stream<Path> walk = Files.walk(path)) {
                walk.filter(Files::isRegularFile)
                        .filter(f ->
                                LanguageDetector.detect(f)
                                        != Language.UNKNOWN)
                        .sorted()
                        .forEach(out::add);
            }

        } else if (LanguageDetector.detect(path) != Language.UNKNOWN) {

            out.add(path);

        } else {

            System.err.println(
                    "Skipping unsupported file: " + path);
        }
    }

    private static String envOr(String name, String fallback) {
        String value = System.getenv(name);

        return (value == null || value.isBlank())
                ? fallback
                : value;
    }
}