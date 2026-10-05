package com.soprasteria.aibydesign.codesentinel;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProjectIntegrationAgentTest {

    @Test
    void onlyRunsForMultipleFiles() {
        ProjectIntegrationAgent agent = new ProjectIntegrationAgent();
        assertFalse(agent.shouldRun(List.of("A.java")));
        assertTrue(agent.shouldRun(List.of("A.java", "B.java")));
    }

    @Test
    void safeModeProducesUsefulProjectSummary() {
        ProjectIntegrationAgent agent = new ProjectIntegrationAgent();
        SquadResult result = new SquadResult();
        result.findings.add(new Finding(
                Finding.Severity.CRITICAL, 10, "Security", "Test issue"));

        Map<String, SquadResult> results = new LinkedHashMap<>();
        results.put("A.java", result);

        String summary = agent.safeModeSummary(results);
        assertTrue(summary.contains("Safe Mode"));
        assertTrue(summary.contains("1 critical"));
    }

    @Test
    void aiReviewSeesAllFilesAndExistingEvidence() throws Exception {
        ProjectIntegrationAgent agent = new ProjectIntegrationAgent();
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("OrderService.java", "class OrderService {}");
        sources.put("CustomerService.java", "class CustomerService {}");

        SquadResult order = new SquadResult();
        order.findings.add(new Finding(
                Finding.Severity.MAJOR, 7, "Architecture", "Cross-boundary smell"));
        Map<String, SquadResult> results = new LinkedHashMap<>();
        results.put("OrderService.java", order);
        results.put("CustomerService.java", new SquadResult());

        final String[] captured = new String[1];
        LlmClient llm = new LlmClient() {
            public boolean isConfigured() { return true; }
            public String ask(String system, String user) {
                captured[0] = user;
                return "## Cross-File Risks\n- example\n## Strong Design Signals\n- example\n## Top 3 Actions\n1. example";
            }
        };

        String result = agent.execute(sources, results, llm);
        assertTrue(result.contains("Cross-File Risks"));
        assertTrue(captured[0].contains("OrderService.java"));
        assertTrue(captured[0].contains("CustomerService.java"));
        assertTrue(captured[0].contains("Cross-boundary smell"));
    }

    @Test
    void scorePenalisesHigherRisk() {
        assertEquals(100, ProjectReportGenerator.calculateScore(0, 0, 0, 0));
        assertTrue(ProjectReportGenerator.calculateScore(1, 0, 0, 0)
                < ProjectReportGenerator.calculateScore(0, 1, 0, 0));
    }
}
