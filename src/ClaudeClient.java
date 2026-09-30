import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Small Anthropic Claude API client.
 *
 * Uses the JDK HttpClient so no additional runtime dependency is needed.
 */
public class ClaudeClient {

    private static final String ENDPOINT =
            "https://api.anthropic.com/v1/messages";

    private static final String MODEL =
            "claude-sonnet-4-6";

    private final String apiKey;
    private final HttpClient httpClient;

    public ClaudeClient(String apiKey) {

        this.apiKey = apiKey;

        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(
                                Duration.ofSeconds(15))
                        .build();
    }

    public boolean isConfigured() {
        return apiKey != null
                && !apiKey.isBlank();
    }

    /**
     * Sends one request to Claude and returns the text response.
     */
    public String ask(
            String systemPrompt,
            String userPrompt) throws Exception {

        if (!isConfigured()) {
            throw new IllegalStateException(
                    "ANTHROPIC_API_KEY is not configured.");
        }

        String escapedSystem =
                escapeJson(systemPrompt);

        String escapedUser =
                escapeJson(userPrompt);

        String body = """
                {
                  "model": "%s",
                  "max_tokens": 2000,
                  "system": "%s",
                  "messages": [
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ]
                }
                """.formatted(
                MODEL,
                escapedSystem,
                escapedUser);

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(ENDPOINT))
                        .timeout(
                                Duration.ofSeconds(60))
                        .header(
                                "content-type",
                                "application/json")
                        .header(
                                "x-api-key",
                                apiKey)
                        .header(
                                "anthropic-version",
                                "2023-06-01")
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(body))
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {

            throw new RuntimeException(
                    "Claude API call failed: HTTP "
                            + response.statusCode()
                            + " - "
                            + response.body());
        }

        return extractTextFromResponse(
                response.body());
    }

    /**
     * Extracts the text fields from the simple response structure.
     *
     * This is intentionally dependency-free for this prototype.
     */
    private String extractTextFromResponse(
            String json) {

        StringBuilder result =
                new StringBuilder();

        String marker = "\"text\":\"";

        int index = 0;

        while ((index =
                json.indexOf(marker, index)) != -1) {

            int start =
                    index + marker.length();

            int end = start;

            while (end < json.length()
                    && json.charAt(end) != '"') {

                if (json.charAt(end) == '\\') {
                    end++;
                }

                end++;
            }

            result.append(
                    unescapeJson(
                            json.substring(start, end)));

            index = end;
        }

        return result.toString();
    }

    private String escapeJson(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String value) {

        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}