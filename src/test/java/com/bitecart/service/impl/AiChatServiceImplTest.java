package com.bitecart.service.impl;

import com.bitecart.ai.AiProvider;
import com.bitecart.config.AiProperties;
import com.bitecart.exception.ChatUnavailableException;
import com.bitecart.service.CatalogKnowledgeService;
import com.bitecart.service.PersonalContextService;
import com.bitecart.service.AiPersonalActionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class AiChatServiceImplTest {

    @Test
    void delegatesToTheConfiguredProvider() {
        AiProperties properties = new AiProperties();
        properties.setProvider("ollama");
        AiProvider openAi = provider("openai", "OpenAI reply");
        AiProvider ollama = provider("ollama", "Ollama reply");

        assertThat(new AiChatServiceImpl(List.of(openAi, ollama), properties, context(), personalContext(), actions()).answerGeneralQuestion("Help"))
                .isEqualTo("Ollama reply");
    }

    @Test
    void rejectsUnknownProviderWithoutFallingBack() {
        AiProperties properties = new AiProperties();
        properties.setProvider("unknown");

        assertThatThrownBy(() -> new AiChatServiceImpl(List.of(provider("openai", "reply")), properties, context(), personalContext(), actions())
                .answerGeneralQuestion("Help")).isInstanceOf(ChatUnavailableException.class);
    }

    @Test
    void executesAnAllowlistedActionBeforeEitherConfiguredProviderIsCalled() {
        AiProperties properties = new AiProperties();
        properties.setProvider("openai");
        AiProvider provider = mock(AiProvider.class);
        org.mockito.Mockito.when(provider.providerName()).thenReturn("openai");
        AiPersonalActionService action = message -> java.util.Optional.of("Added to your cart.");

        String reply = new AiChatServiceImpl(List.of(provider), properties, context(), personalContext(), action)
                .answerGeneralQuestion("Add 2 Tandoori BBQ Chicken Legs to my cart");

        assertThat(reply).isEqualTo("Added to your cart.");
        verify(provider).providerName();
        verifyNoMoreInteractions(provider);
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

    private PersonalContextService personalContext() {
        return question -> "Read-only private context";
    }

    private AiPersonalActionService actions() {
        return message -> java.util.Optional.empty();
    }
}
