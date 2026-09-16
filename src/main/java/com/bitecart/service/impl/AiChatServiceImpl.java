package com.bitecart.service.impl;

import com.bitecart.ai.AiProvider;
import com.bitecart.config.AiProperties;
import com.bitecart.exception.ChatUnavailableException;
import com.bitecart.service.AiChatService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AiChatServiceImpl implements AiChatService {

    private static final String INSTRUCTIONS = "You are BiteCart's helpful customer assistant. Answer only general questions "
            + "about BiteCart, food ordering, browsing the menu, using the website, and general customer support. "
            + "You have no access to customer accounts, orders, carts, addresses, stock, payments, or admin data. "
            + "Never claim that you can view or change those details. Keep answers concise, friendly, and helpful.";

    private final Map<String, AiProvider> providers;
    private final AiProperties properties;

    public AiChatServiceImpl(java.util.List<AiProvider> providers, AiProperties properties) {
        this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(
                provider -> provider.providerName().toLowerCase(), Function.identity()));
        this.properties = properties;
    }

    @Override
    public String answerGeneralQuestion(String message) {
        String selected = properties.getProvider() == null ? "" : properties.getProvider().trim().toLowerCase();
        AiProvider provider = providers.get(selected);
        if (provider == null) {
            throw new ChatUnavailableException("BiteCart Assistant is not configured right now. Please try again later.");
        }
        return provider.answer(INSTRUCTIONS, message);
    }
}
