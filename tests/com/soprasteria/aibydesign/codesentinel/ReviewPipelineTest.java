package com.soprasteria.aibydesign.codesentinel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** End-to-end tests of the whole squad using a scripted stub instead of the real AI service. */
class ReviewPipelineTest {

    private static final Path VULNERABLE = Path.of("tests/samples/SampleVulnerableService.java");
    private static final Path GOD_CLASS = Path.of("tests/samples/SampleGodClassService.java");

    /** Records prompts and answers by looking at the system prompt. */
    static class StubLlm implements LlmClient {
        final List<String> prompts = new ArrayList<>();
        final List<String> criticReplies = new ArrayList<>();
        boolean configured = true;
        boolean failSecurity = false;

        @Override public boolean isConfigured() { return configured; }

        @Override
        public String ask(String system, String user) {
            prompts.add(user);
            if (system.contains("application security")) {
                if (failSecurity) throw new IllegalStateException("simulated outage");
                return "security advice";
            }
            if (system.contains("QA reviewer")) {
                return criticReplies.isEmpty() ? "APPROVED" : criticReplies.remove(0);
            }
            if (system.contains("testing expert")) {
                return "```java\nclass XTest { @Test void t() { assertTrue(true); } }\n```";
            }
            if (system.contains("technical writer")) {
                return "/** doc */ class X {}\nPLAIN_SUMMARY: a class";
            }
            return "architecture advice";
        }
    }

    private SquadResult run(StubLlm llm, Path source, Path tmp) throws Exception {
        return new ReviewPipeline(llm, new MemoryStore(tmp.resolve("mem")), tmp.resolve("out"),
                new PrintStream(new ByteArrayOutputStream())).review(source);
    }

    @Test
    void safeModeNeverCallsTheAiService(@TempDir Path tmp) throws Exception {
        StubLlm llm = new StubLlm();
        llm.configured = false;

        SquadResult r = run(llm, VULNERABLE, tmp);

        assertTrue(r.safeMode);
        assertTrue(llm.prompts.isEmpty());
        assertTrue(Files.readString(tmp.resolve("out/SampleVulnerableService/review.md")).contains("Safe Mode"));
    }

    @Test
    void vulnerableSampleDispatchesSecurityTestAndDocsButNotArchitecture(@TempDir Path tmp) throws Exception {
        SquadResult r = run(new StubLlm(), VULNERABLE, tmp);

        assertTrue(r.dispatchedAgents.contains("Security Agent"));
        assertTrue(r.dispatchedAgents.contains(TestAgent.NAME));
        assertTrue(r.dispatchedAgents.contains(DocsAgent.NAME));
        assertTrue(r.skippedAgents.contains("Architecture Review Agent"));
        assertTrue(r.testsApproved);
        assertTrue(Files.exists(tmp.resolve("out/SampleVulnerableService/SampleVulnerableServiceTest.java")));
    }

    @Test
    void godClassRoutesToArchitectureAgent(@TempDir Path tmp) throws Exception {
        SquadResult r = run(new StubLlm(), GOD_CLASS, tmp);
        assertTrue(r.dispatchedAgents.contains("Architecture Review Agent"));
        assertTrue(r.skippedAgents.contains("Security Agent"));
    }

    @Test
    void criticRevisionLoopFeedsFeedbackBackToTestAgent(@TempDir Path tmp) throws Exception {
        StubLlm llm = new StubLlm();
        llm.criticReplies.add("REVISE: assert the SQL is parameterised");

        SquadResult r = run(llm, VULNERABLE, tmp);

        assertTrue(r.testsApproved, "second attempt should be approved");
        assertTrue(llm.prompts.stream().anyMatch(p -> p.contains("assert the SQL is parameterised")),
                "critic feedback must reach the Test Agent");
        assertTrue(r.criticTranscript.size() >= 3);
    }

    @Test
    void revisionIsLimitedToOneRetryAndEndsUnapproved(@TempDir Path tmp) throws Exception {
        StubLlm llm = new StubLlm();
        llm.criticReplies.add("REVISE: no");
        llm.criticReplies.add("REVISE: still no");

        SquadResult r = run(llm, VULNERABLE, tmp);
        assertFalse(r.testsApproved);
    }

    @Test
    void oneAgentFailureDoesNotStopTheReview(@TempDir Path tmp) throws Exception {
        StubLlm llm = new StubLlm();
        llm.failSecurity = true;

        SquadResult r = run(llm, VULNERABLE, tmp);

        assertEquals(1, r.warnings.stream().filter(w -> w.contains("Security Agent")).count());
        assertTrue(r.agentOutputs.containsKey(TestAgent.NAME), "other agents still ran");
        assertTrue(Files.readString(tmp.resolve("out/SampleVulnerableService/review.md")).contains("## Warnings"));
    }

    @Test
    void secretsAreRedactedBeforeReachingTheAiService(@TempDir Path tmp) throws Exception {
        StubLlm llm = new StubLlm();
        run(llm, VULNERABLE, tmp);

        assertFalse(llm.prompts.isEmpty());
        assertTrue(llm.prompts.stream().noneMatch(p -> p.contains("EXAMPLE_NOT_A_REAL_SECRET")));
    }

    @Test
    void secondRunReportsRecurringFindings(@TempDir Path tmp) throws Exception {
        StubLlm llm = new StubLlm();
        llm.configured = false;

        assertTrue(run(llm, VULNERABLE, tmp).recurringFindings.isEmpty());
        SquadResult second = run(llm, VULNERABLE, tmp);

        assertFalse(second.recurringFindings.isEmpty());
        assertEquals(second.findings.size(), second.recurringFindings.size());
    }

    @Test
    void markdownFencesAreStrippedFromGeneratedTests(@TempDir Path tmp) throws Exception {
        run(new StubLlm(), VULNERABLE, tmp);
        String generated = Files.readString(tmp.resolve("out/SampleVulnerableService/SampleVulnerableServiceTest.java"));
        assertFalse(generated.contains("```"));
    }
}
