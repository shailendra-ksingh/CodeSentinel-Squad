package com.soprasteria.aibydesign.codesentinel;

import java.util.List;

/**
 * Common contract for every specialist agent in the CodeSentinel squad.
 *
 * Each specialist decides FOR ITSELF whether it has anything useful to do
 * on this file (shouldRun) before the CoordinatorAgent ever calls it. This
 * keeps LLM usage proportional to actual need — a file with no security
 * findings never triggers the SecurityAgent, a small clean class never
 * triggers the ArchitectureAgent, and so on.
 */
public interface SpecialistAgent {

    /** Short display name, used in the report and demo output. */
    String name();

    /** Cheap, local, offline check: is this agent even relevant for this file? */
    boolean shouldRun(String sourceCode, List<Finding> findings);

    /** Does the specialist's actual work, calling the LLM as needed. */
    String execute(String className, String sourceCode, List<Finding> findings, LlmClient client) throws Exception;
}
