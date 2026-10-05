package com.soprasteria.aibydesign.codesentinel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Small local memory for CodeSentinel.
 *
 * It remembers findings from previous reviews so the next review can show
 * which issue signatures were detected again.
 *
 * No database or extra library is required.
 */
public class MemoryStore {

    private final Path memoryDir;

    public MemoryStore(Path memoryDir) {
        this.memoryDir = memoryDir;
    }

    /**
     * Loads findings from the previous review of the given class.
     *
     * Invalid or incomplete log lines are ignored instead of stopping
     * the complete review.
     */
    public List<Finding> loadPrevious(String className) throws IOException {

        Path file = memoryDir.resolve(className + ".log");

        if (!Files.exists(file)) {
            return List.of();
        }

        List<Finding> previous = new ArrayList<>();

        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {

            if (line.isBlank()) {
                continue;
            }

            try {
                String[] parts = line.split("\\|", 5);

                if (parts.length < 5) {
                    continue;
                }

                Finding.Severity severity =
                        Finding.Severity.valueOf(parts[1]);

                int lineNumber =
                        Integer.parseInt(parts[2]);

                previous.add(
                        new Finding(
                                severity,
                                lineNumber,
                                parts[3],
                                parts[4]));
            } catch (Exception ignored) {
                /*
                 * One bad memory entry should not make the complete
                 * review fail.
                 */
            }
        }

        return previous;
    }

    /**
     * Saves the current findings for the next review.
     */
    public void save(
            String className,
            List<Finding> findings) throws IOException {

        Files.createDirectories(memoryDir);

        Path file =
                memoryDir.resolve(className + ".log");

        String timestamp = Instant.now().toString();

        List<String> lines = findings.stream()
                .map(f -> toLogLine(timestamp, f))
                .collect(Collectors.toList());

        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    private String toLogLine(String timestamp, Finding finding) {

        /*
         * The memory format uses | as a separator.
         * Replace it in the description so one finding remains one line.
         */
        String description =
                finding.getDescription()
                        .replace("|", "/");

        return timestamp
                + "|" + finding.getSeverity()
                + "|" + finding.getLineNumber()
                + "|" + finding.getCategory()
                + "|" + description;
    }

    /**
     * Returns current findings whose category and description were also
     * present in the previous review.
     *
     * Line number is deliberately ignored because code can move between
     * reviews while the underlying issue remains the same.
     */
    public List<Finding> recurring(
            List<Finding> current,
            List<Finding> previous) {

        Set<String> previousKeys =
                previous.stream()
                        .map(Finding::signature)
                        .collect(Collectors.toSet());

        return current.stream()
                .filter(f -> previousKeys.contains(f.signature()))
                .collect(Collectors.toList());
    }

}