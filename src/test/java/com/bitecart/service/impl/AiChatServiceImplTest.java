package com.bitecart.service.impl;

import com.bitecart.ai.AiProvider;
import com.bitecart.config.AiProperties;
import com.bitecart.exception.ChatUnavailableException;
import com.bitecart.service.CatalogKnowledgeService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiChatServiceImplTest {

    @Test
    void delegatesToTheConfiguredProvider() {
        AiProperties properties = new AiProperties();
        properties.setProvider("ollama");
        AiProvider openAi = provider("openai", "OpenAI reply");
        AiProvider ollama = provider("ollama", "Ollama reply");

        assertThat(new AiChatServiceImpl(List.of(openAi, ollama), properties, context()).answerGeneralQuestion("Help"))
                .isEqualTo("Ollama reply");
    }

    @Test
    void rejectsUnknownProviderWithoutFallingBack() {
        AiProperties properties = new AiProperties();
        properties.setProvider("unknown");

        assertThatThrownBy(() -> new AiChatServiceImpl(List.of(provider("openai", "reply")), properties, context())
                .answerGeneralQuestion("Help")).isInstanceOf(ChatUnavailableException.class);
    }

    private AiProvider provider(String name, String reply) {
        return new AiProvider() {
            @Override public String providerName() { return name; }
            @Override public String answer(String instructions, String message) { return reply; }
        };
    }

    private CatalogKnowledgeService context() {
        return question -> "Authoritative BiteCart catalog data";
    }
}
