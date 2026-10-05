package com.soprasteria.aibydesign.codesentinel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small Anthropic Messages API client built on the JDK HttpClient
 * (no runtime dependencies).
 *
 * Configuration (environment variables):
 *   ANTHROPIC_API_KEY   required for AI mode; absent means Safe Mode
 *   CODESENTINEL_MODEL  optional model override
 *
 * Transient failures (HTTP 429, 5xx, 529 and I/O errors) are retried with
 * exponential back-off. The API key is never logged or included in errors.
 */
public class ClaudeClient implements LlmClient {

    static final String DEFAULT_MODEL = "claude-sonnet-4-6";

    private static final String ENDPOINT =
            "https://api.anthropic.com/v1/messages";

    private static final int MAX_ATTEMPTS = 3;

    private static final Pattern TEXT_FIELD =
            Pattern.compile("\"text\"\\s*:\\s*\"");

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;

    public ClaudeClient(String apiKey) {
        this(apiKey, System.getenv("CODESENTINEL_MODEL"));
    }

    public ClaudeClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = (model == null || model.isBlank()) ? DEFAULT_MODEL : model.trim();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String ask(String systemPrompt, String userPrompt) throws Exception {

        if (!isConfigured()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY is not configured.");
        }

        String body = "{\"model\":\"" + escapeJson(model) + "\","
                + "\"max_tokens\":4096,"
                + "\"system\":\"" + escapeJson(systemPrompt) + "\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\""
                + escapeJson(userPrompt) + "\"}]}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(90))
                .header("content-type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        Exception last = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString());

                int status = response.statusCode();

                if (status == 200) {
                    return extractText(response.body());
                }

                String message = "Claude API call failed: HTTP " + status;
                if (!isRetryable(status)) {
                    throw new IllegalStateException(message);
                }
                last = new IllegalStateException(message);

            } catch (java.io.IOException e) {
                last = e;
            }

            if (attempt < MAX_ATTEMPTS) {
                Thread.sleep(1000L * (1L << (attempt - 1)));
            }
        }
        throw last;
    }

    private static boolean isRetryable(int status) {
        return status == 429 || status == 529 || status >= 500;
    }

    /** Concatenates every "text" content block of a Messages API response. */
    static String extractText(String json) {

        StringBuilder out = new StringBuilder();
        Matcher m = TEXT_FIELD.matcher(json);
        int from = 0;

        while (m.find(from)) {
            int i = m.end();
            StringBuilder s = new StringBuilder();

            while (i < json.length() && json.charAt(i) != '"') {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char n = json.charAt(++i);
                    switch (n) {
                        case 'n' -> s.append('\n');
                        case 'r' -> s.append('\r');
                        case 't' -> s.append('\t');
                        case 'b' -> s.append('\b');
                        case 'f' -> s.append('\f');
                        case 'u' -> {
                            if (i + 4 < json.length()) {
                                s.append((char) Integer.parseInt(
                                        json.substring(i + 1, i + 5), 16));
                                i += 4;
                            }
                        }
                        default -> s.append(n); // \\ \" \/ 
                    }
                } else {
                    s.append(c);
                }
                i++;
            }
            out.append(s);
            from = i;
        }
        return out.toString();
    }

    static String escapeJson(String value) {

        StringBuilder sb = new StringBuilder(value.length() + 16);

        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
