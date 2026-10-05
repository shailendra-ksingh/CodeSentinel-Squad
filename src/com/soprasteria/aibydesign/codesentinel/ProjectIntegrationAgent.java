package com.soprasteria.aibydesign.codesentinel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Project-level specialist: reviews relationships between multiple files, including polyglot application stacks.
 *
 * Unlike the file specialists, this agent looks for integration risks such as
 * contract mismatches, duplicated responsibilities, inconsistent error/security
 * handling and suspicious dependencies across the reviewed files.
 */
public class ProjectIntegrationAgent {

    public static final String NAME = "Project Integration Agent";

    public boolean shouldRun(List<?> sourceFiles) {
        return sourceFiles != null && sourceFiles.size() > 1;
    }

    public String execute(
            Map<String, String> sources,
            Map<String, SquadResult> results,
            LlmClient client) throws Exception {

        StringBuilder evidence = new StringBuilder();

        results.forEach((name, result) -> {
            evidence.append("\n## ").append(name).append("\n");
            evidence.append("Findings: ").append(result.findings.size())
                    .append(", recurring: ").append(result.recurringFindings.size()).append("\n");

            result.findings.stream().limit(12).forEach(f ->
                    evidence.append("- ").append(f).append("\n"));
        });

        StringBuilder code = new StringBuilder();
        sources.forEach((name, source) -> {
            code.append("\n===== FILE: ").append(name).append(" =====\n")
                    .append(source);
        });

        String system =
                "You are a Principal Architect performing a project-level integration review. "
                + "Review the supplied files as ONE system, including cross-language boundaries, not as isolated files. "
                + "Look specifically for cross-file contract mismatches, incorrect layering, "
                + "duplicated responsibilities, inconsistent security/error handling, "
                + "tight coupling, dependency cycles, shared-state risks and missing tests at boundaries. "
                + "Do not invent defects. Only report a risk when the supplied code provides evidence. "
                + "Return concise Markdown with exactly these sections: "
                + "## Cross-File Risks, ## Strong Design Signals, ## Top 3 Actions. "
                + "For each risk include the file(s), evidence, impact and recommended action.";

        String user = "Files under review:\n" + code
                + "\n\nExisting deterministic evidence:\n" + evidence;

        return client.ask(system, user);
    }

    /**
     * Safe-mode fallback: gives a useful project summary without sending source
     * code to an external service.
     */
    public String safeModeSummary(Map<String, SquadResult> results) {
        long findings = results.values().stream()
                .mapToLong(r -> r.findings.size())
                .sum();
        long recurring = results.values().stream()
                .mapToLong(r -> r.recurringFindings.size())
                .sum();
        long critical = results.values().stream()
                .flatMap(r -> r.findings.stream())
                .filter(f -> f.getSeverity() == Finding.Severity.CRITICAL)
                .count();

        return "## Cross-File Risks\n\n"
                + "Safe Mode did not send project source to an AI service. "
                + "Cross-file semantic analysis is therefore not available.\n\n"
                + "## Strong Design Signals\n\n"
                + "- Reviewed " + results.size() + " files as one project set.\n"
                + "- Deterministic evidence found " + findings + " finding(s), including "
                + critical + " critical.\n"
                + "- " + recurring + " finding(s) recur from previous reviews.\n\n"
                + "## Top 3 Actions\n\n"
                + "1. Resolve critical findings first.\n"
                + "2. Review contracts between APIs, services, clients, schemas, configuration and shared models.\n"
                + "3. Re-run CodeSentinel after fixes to confirm recurrence is reduced.";
    }
}
