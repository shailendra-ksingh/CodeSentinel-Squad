package com.soprasteria.aibydesign.codesentinel;

import java.util.List;

/**
 * Coordinates the specialist agents during a code review.
 *
 * The Coordinator selects relevant specialists and manages the
 * Test Agent -> Critic -> Revision flow.
 *
 * A failure in one specialist does not stop the rest of the review.
 */
public class CoordinatorAgent {

    private static final int MAX_TEST_REVISIONS = 1;

    private final List<SpecialistAgent> specialists;
    private final CriticAgent critic;
    private final LlmClient llm;

    public CoordinatorAgent(
            List<SpecialistAgent> specialists,
            CriticAgent critic,
            LlmClient llm) {

        this.specialists = specialists;
        this.critic = critic;
        this.llm = llm;
    }

    public SquadResult run(
            String className,
            String sourceCode,
            List<Finding> findings,
            List<Finding> recurringFindings) throws Exception {

        SquadResult result = new SquadResult();

        // Keep original findings for reports and review memory.
        result.findings = findings;
        result.recurringFindings = recurringFindings;

        // Deterministic review still works without an AI API key.
        if (!llm.isConfigured()) {
            result.safeMode = true;
            return result;
        }

        /*
         * Only the redacted source is allowed to reach AI agents.
         * Local analysis has already been performed on the original source.
         */
        String safeSource = SecretRedactor.redact(sourceCode);

        /*
         * Findings can contain values taken from the source, so create
         * a separate safe copy for AI processing.
         */
        List<Finding> safeFindings = redactFindingsForAi(findings);

        for (SpecialistAgent agent : specialists) {

            try {
                if (!agent.shouldRun(safeSource, safeFindings)) {
                    result.skippedAgents.add(agent.name());
                    continue;
                }

                result.dispatchedAgents.add(agent.name());

                if (agent instanceof TestAgent testAgent) {

                    runTestAgentWithCriticLoop(
                            className,
                            safeSource,
                            safeFindings,
                            testAgent,
                            result);

                } else {

                    String output = agent.execute(
                            className,
                            safeSource,
                            safeFindings,
                            llm);

                    result.agentOutputs.put(
                            agent.name(),
                            output);
                }

            } catch (Exception e) {
                recordAgentFailure(agent, e, result);
            }
        }

        return result;
    }

    /**
     * Generates tests, checks them with the Critic and allows one revision.
     */
    private void runTestAgentWithCriticLoop(
            String className,
            String sourceCode,
            List<Finding> findings,
            TestAgent testAgent,
            SquadResult result) {

        String feedback = null;
        String testCode = null;

        for (int attempt = 1;
             attempt <= MAX_TEST_REVISIONS + 1;
             attempt++) {

            try {

                testCode = testAgent.execute(
                        className,
                        sourceCode,
                        findings,
                        llm,
                        feedback);

                CriticAgent.Verdict structural =
                        critic.structuralCheck(testCode);

                result.criticTranscript.add(
                        "Attempt " + attempt
                                + " - structural check: "
                                + (structural.approved
                                ? "PASSED"
                                : "FAILED - " + structural.feedback));

                if (!structural.approved) {
                    feedback = structural.feedback;

                    if (attempt > MAX_TEST_REVISIONS) {
                        break;
                    }

                    continue;
                }

                CriticAgent.Verdict semantic =
                        critic.semanticCheck(
                                testCode,
                                findings,
                                llm);

                result.criticTranscript.add(
                        "Attempt " + attempt
                                + " - semantic review: "
                                + semantic.feedback);

                if (semantic.approved) {
                    result.testsApproved = true;
                    break;
                }

                feedback = semantic.feedback;

            } catch (Exception e) {

                result.warnings.add(
                        "Test/Critic loop failed: "
                                + safeMessage(e));

                break;
            }
        }

        if (testCode != null) {
            result.agentOutputs.put(
                    testAgent.name(),
                    testCode);
        }
    }

    /**
     * Creates an AI-safe copy of the findings.
     *
     * The original findings remain unchanged for local reporting.
     */
    private List<Finding> redactFindingsForAi(List<Finding> findings) {

        return findings.stream()
                .map(finding -> new Finding(
                        finding.getSeverity(),
                        finding.getLineNumber(),
                        finding.getCategory(),
                        SecretRedactor.redact(
                                finding.getDescription())))
                .toList();
    }

    /**
     * Records a specialist failure without stopping the review.
     */
    private void recordAgentFailure(
            SpecialistAgent agent,
            Exception e,
            SquadResult result) {

        String message =
                "Agent '" + agent.name()
                        + "' failed: "
                        + safeMessage(e);

        result.warnings.add(message);

        result.agentOutputs.put(
                agent.name(),
                "Agent failed. Human review recommended.\n"
                        + message);
    }

    private String safeMessage(Exception e) {

        if (e.getMessage() == null || e.getMessage().isBlank()) {
            return e.getClass().getSimpleName();
        }

        return e.getMessage();
    }
}