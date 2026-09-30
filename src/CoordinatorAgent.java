import java.util.List;

/**
 * Coordinates the specialist agents.
 *
 * The Coordinator does not perform the specialist work itself.
 * It looks at the findings, decides which agents are relevant,
 * and manages the Test Agent -> Critic -> Revision loop.
 *
 * A failure in one specialist should not stop the complete review.
 */
public class CoordinatorAgent {

    private static final int MAX_TEST_REVISIONS = 1;

    private final List<SpecialistAgent> specialists;
    private final CriticAgent critic;
    private final ClaudeClient claudeClient;

    public CoordinatorAgent(
            List<SpecialistAgent> specialists,
            CriticAgent critic,
            ClaudeClient claudeClient) {

        this.specialists = specialists;
        this.critic = critic;
        this.claudeClient = claudeClient;
    }

    public SquadResult run(
            String className,
            String sourceCode,
            List<Finding> findings,
            List<Finding> recurringFindings) throws Exception {

        SquadResult result = new SquadResult();

        result.findings = findings;
        result.recurringFindings = recurringFindings;

        /*
         * Safe Mode:
         * The deterministic analyzer can still provide useful results
         * even when an AI API key is not configured.
         */
        if (!claudeClient.isConfigured()) {
            result.safeMode = true;
            return result;
        }

        for (SpecialistAgent agent : specialists) {

            try {
                if (!agent.shouldRun(sourceCode, findings)) {
                    result.skippedAgents.add(agent.name());
                    continue;
                }

                result.dispatchedAgents.add(agent.name());

                if (agent instanceof TestAgent testAgent) {

                    runTestAgentWithCriticLoop(
                            className,
                            sourceCode,
                            findings,
                            testAgent,
                            result);

                } else {

                    String output = agent.execute(
                            className,
                            sourceCode,
                            findings,
                            claudeClient);

                    result.agentOutputs.put(agent.name(), output);
                }

            } catch (Exception e) {

                /*
                 * One specialist should not bring down the complete review.
                 * The warning is included in the final report so a developer
                 * knows that manual review may still be required.
                 */
                recordAgentFailure(agent, e, result);
            }
        }

        return result;
    }

    /**
     * Runs the Test Agent and then sends its output to the Critic.
     *
     * If the Critic rejects the first version, the feedback is sent back
     * to the Test Agent for one revision.
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
                        claudeClient,
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
                                claudeClient);

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
     * Records a specialist failure without stopping the complete review.
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