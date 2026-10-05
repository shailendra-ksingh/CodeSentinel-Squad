package com.soprasteria.aibydesign.codesentinel;

/**
 * Minimal contract the agents need from a language-model service.
 *
 * Keeping this as an interface lets the agents be tested with a stub and
 * lets the team swap the model provider without touching any agent.
 */
public interface LlmClient {

    /** True when the service has credentials and can be called. */
    boolean isConfigured();

    /** Sends one prompt and returns the model's text reply. */
    String ask(String systemPrompt, String userPrompt) throws Exception;
}
