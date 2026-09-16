package com.bitecart.ai;

import com.bitecart.config.OllamaProperties;
import com.bitecart.exception.ChatUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Component
public class OllamaAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaAiProvider.class);
    private final RestClient restClient;
    private final OllamaProperties properties;

    public OllamaAiProvider(RestClient.Builder restClientBuilder, OllamaProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        this.properties = properties;
    }

    @Override
    public String providerName() { return "ollama"; }

    @Override
    public String answer(String instructions, String message) {
        try {
            JsonNode response = restClient.post().uri("/api/chat").contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("model", properties.getModel(), "stream", false, "messages", List.of(
                            Map.of("role", "system", "content", instructions),
                            Map.of("role", "user", "content", message.trim()))))
                    .retrieve().body(JsonNode.class);
            String answer = response == null ? null : response.path("message").path("content").asText();
            if (answer == null || answer.isBlank()) {
                throw new ChatUnavailableException("BiteCart Assistant could not answer just now. Please try again.");
            }
            return answer.trim();
        } catch (RestClientResponseException exception) {
            log.warn("Ollama chat request failed: status={}, errorType={}", exception.getStatusCode().value(),
                    exception.getClass().getSimpleName());
            throw new ChatUnavailableException("BiteCart Assistant is temporarily unavailable. Please try again later.");
        } catch (RestClientException exception) {
            log.warn("Ollama chat request failed before a valid HTTP response: errorType={}", exception.getClass().getSimpleName());
            throw new ChatUnavailableException("BiteCart Assistant is temporarily unavailable. Please try again later.");
        }
    }
}
