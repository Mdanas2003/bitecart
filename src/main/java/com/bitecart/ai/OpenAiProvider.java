package com.bitecart.ai;

import com.bitecart.config.OpenAiProperties;
import com.bitecart.exception.ChatUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.regex.Pattern;

@Component
public class OpenAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiProvider.class);
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)(bearer\\s+)[^\\s]+");
    private static final Pattern OPENAI_KEY = Pattern.compile("(?i)sk-[a-z0-9_-]+");

    private final RestClient restClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiProvider(RestClient.Builder restClientBuilder, OpenAiProperties properties, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.baseUrl("https://api.openai.com").build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerName() { return "openai"; }

    @Override
    public String answer(String instructions, String message) {
        if (!properties.isConfigured()) {
            log.warn("OpenAI chat is unavailable because OPENAI_API_KEY is not configured.");
            throw new ChatUnavailableException("BiteCart Assistant is not configured right now. Please try again later.");
        }
        try {
            JsonNode response = restClient.post().uri("/v1/responses").contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(properties.getApiKey()))
                    .body(Map.of("model", properties.getModel(), "instructions", instructions, "input", message.trim(), "store", false))
                    .retrieve().body(JsonNode.class);
            String answer = extractOutputText(response);
            if (answer == null || answer.isBlank()) {
                throw new ChatUnavailableException("BiteCart Assistant could not answer just now. Please try again.");
            }
            return answer.trim();
        } catch (RestClientResponseException exception) {
            OpenAiErrorDetails error = readOpenAiError(exception);
            log.warn("OpenAI Responses request failed: status={}, errorType={}, errorCode={}, message={}",
                    exception.getStatusCode().value(), error.type(), error.code(), sanitizeForLog(error.message()));
            throw new ChatUnavailableException("BiteCart Assistant is temporarily unavailable. Please try again later.");
        } catch (RestClientException exception) {
            log.warn("OpenAI Responses request failed before a valid HTTP response: errorType={}, message={}",
                    exception.getClass().getSimpleName(), sanitizeForLog(exception.getMessage()));
            throw new ChatUnavailableException("BiteCart Assistant is temporarily unavailable. Please try again later.");
        }
    }

    private OpenAiErrorDetails readOpenAiError(RestClientResponseException exception) {
        try {
            JsonNode error = objectMapper.readTree(exception.getResponseBodyAsString()).path("error");
            if (!error.isMissingNode() && !error.isNull()) {
                return new OpenAiErrorDetails(error.path("type").asText(exception.getClass().getSimpleName()),
                        error.path("code").asText("none"), error.path("message").asText(exception.getStatusText()));
            }
        } catch (Exception ignored) { }
        return new OpenAiErrorDetails(exception.getClass().getSimpleName(), "none", exception.getStatusText());
    }

    public static String sanitizeForLog(String message) {
        if (message == null || message.isBlank()) return "no message";
        String sanitized = BEARER_TOKEN.matcher(message).replaceAll("$1[REDACTED]");
        sanitized = OPENAI_KEY.matcher(sanitized).replaceAll("[REDACTED]");
        sanitized = sanitized.replaceAll("[\\r\\n\\t]", " ").trim();
        return sanitized.length() > 500 ? sanitized.substring(0, 500) + "…" : sanitized;
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) return null;
        if (response.hasNonNull("output_text")) return response.path("output_text").asText();
        for (JsonNode item : response.path("output")) {
            for (JsonNode part : item.path("content")) {
                if ("output_text".equals(part.path("type").asText()) && !part.path("text").asText().isBlank()) return part.path("text").asText();
            }
        }
        return null;
    }

    private record OpenAiErrorDetails(String type, String code, String message) { }
}
