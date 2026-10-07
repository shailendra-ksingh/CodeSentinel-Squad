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
 * Command-line entry point for CodeSentinel.
 *
 * Usage:
 *   java -jar codesentinel.jar
 *   java -jar codesentinel.jar <file-or-directory> [more paths...]
 *
 * When no path is supplied, the default source directory is "src".
 *
 * Environment variables:
 *   ANTHROPIC_API_KEY        enables AI mode; without it Safe Mode is used
 *   CODESENTINEL_MODEL       optional model override
 *   CODESENTINEL_OUTPUT_DIR  report directory; default is "output"
 *   CODESENTINEL_MEMORY_DIR  history directory; default is "memory"
 */
public class Main {

    public static void main(String[] args) throws Exception {

        // No argument means: review the default source directory.
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
            System.err.println(
                    "No supported source/configuration files found."
            );
            System.exit(2);
        }

        LlmClient llm =
                new ClaudeClient(
                        System.getenv("ANTHROPIC_API_KEY")
                );

        MemoryStore memory =
                new MemoryStore(
                        Path.of(
                                envOr(
                                        "CODESENTINEL_MEMORY_DIR",
                                        "memory"
                                )
                        )
                );

        Path outputRoot =
                Path.of(
                        envOr(
                                "CODESENTINEL_OUTPUT_DIR",
                                "output"
                        )
                );

        ReviewPipeline pipeline =
                new ReviewPipeline(
                        llm,
                        memory,
                        outputRoot,
                        System.out
                );

        int fileReviewFailures = 0;

        Map<String, SquadResult> results =
                new LinkedHashMap<>();

        // Review each file independently so one failure does not stop
        // the remaining files from being reviewed.
        for (Path file : files) {
            try {
                results.put(
                        file.toString(),
                        pipeline.review(file)
                );
            } catch (Exception e) {

                fileReviewFailures++;

                System.err.println(
                        "Review of "
                                + file
                                + " failed: "
                                + e.getMessage()
                );
            }
        }

        boolean projectReviewFailed = false;

        /*
         * Once two or more files have been reviewed successfully,
         * run the project-level integration review.
         */
        if (results.size() > 1) {
            try {
                ProjectIntegrationAgent integration =
                        new ProjectIntegrationAgent();

                Map<String, String> sources =
                        new LinkedHashMap<>();

                for (Path file : files) {
                    if (results.containsKey(file.toString())) {

                        sources.put(
                                file.toString(),
                                SecretRedactor.redact(
                                        Files.readString(file)
                                )
                        );
                    }
                }

                String projectAnalysis;

                if (integration.shouldRun(files)) {

                    projectAnalysis = llm.isConfigured()
                            ? integration.execute(sources, results, llm)
                            : integration.safeModeSummary(sources, results);

                } else {

                    projectAnalysis =
                            "No project-level review required.";
                }

                new ProjectReportGenerator().write(
                        files,
                        results,
                        projectAnalysis,
                        outputRoot.resolve("project")
                );

                System.out.println(
                        "Project report: "
                                + outputRoot
                                .resolve(
                                        "project/project-review.md"
                                )
                                .toAbsolutePath()
                );

            } catch (Exception e) {

                projectReviewFailed = true;

                System.err.println(
                        "Project integration review failed: "
                                + e.getMessage()
                );
            }
        }

        System.out.println();

        System.out.println(
                "Reviewed "
                        + (files.size() - fileReviewFailures)
                        + " of "
                        + files.size()
                        + " file(s)."
        );

        if (fileReviewFailures > 0) {
            System.out.println(
                    "File review failures: "
                            + fileReviewFailures
            );
        }

        if (projectReviewFailed) {
            System.out.println(
                    "Project integration review: FAILED "
                            + "- file reviews are still available."
            );
        }

        /*
         * A failure in either the file review or the project review
         * means the command did not complete successfully.
         *
         * Individual reports are still kept so that a failure in one
         * stage does not hide useful review results.
         */
        if (fileReviewFailures > 0
                || projectReviewFailed) {

            System.exit(1);
        }
    }

    /**
     * Collect all supported source/configuration files from a file
     * or directory.
     */
    private static void collectSourceFiles(
            Path path,
            List<Path> files) throws IOException {

        if (Files.isDirectory(path)) {

            try (Stream<Path> walk = Files.walk(path)) {

                walk.filter(Files::isRegularFile)
                        .filter(file ->
                                LanguageDetector.detect(file)
                                        != Language.UNKNOWN)
                        .sorted()
                        .forEach(files::add);
            }

            return;
        }

        if (LanguageDetector.detect(path)
                != Language.UNKNOWN) {

            files.add(path);
            return;
        }

        System.err.println(
                "Skipping unsupported file: " + path
        );
    }

    private static String envOr(
            String name,
            String fallback) {

        String value =
                System.getenv(name);

        return value == null || value.isBlank()
                ? fallback
                : value;
    }
}