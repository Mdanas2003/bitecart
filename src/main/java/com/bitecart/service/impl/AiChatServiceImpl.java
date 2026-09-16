package com.bitecart.service.impl;

import com.bitecart.ai.AiProvider;
import com.bitecart.config.AiProperties;
import com.bitecart.exception.ChatUnavailableException;
import com.bitecart.service.AiChatService;
import com.bitecart.service.AiPersonalActionService;
import com.bitecart.service.CatalogKnowledgeService;
import com.bitecart.service.PersonalContextService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AiChatServiceImpl implements AiChatService {

    private static final String INSTRUCTIONS = "You are BiteCart's helpful customer assistant. Answer only general questions "
            + "about BiteCart, food ordering, browsing the menu, using the website, and general customer support. "
            + "You may use only the supplied read-only catalog data and private context for the authenticated customer. "
            + "Never claim access to another customer, addresses, payment details, or admin data, and never claim to make changes. "
            + "Keep answers concise, friendly, and helpful.";

    private final Map<String, AiProvider> providers;
    private final AiProperties properties;
    private final CatalogKnowledgeService catalogKnowledgeService;
    private final PersonalContextService personalContextService;
    private final AiPersonalActionService personalActionService;

    public AiChatServiceImpl(java.util.List<AiProvider> providers, AiProperties properties,
                             CatalogKnowledgeService catalogKnowledgeService,
                             PersonalContextService personalContextService,
                             AiPersonalActionService personalActionService) {
        this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(
                provider -> provider.providerName().toLowerCase(), Function.identity()));
        this.properties = properties;
        this.catalogKnowledgeService = catalogKnowledgeService;
        this.personalContextService = personalContextService;
        this.personalActionService = personalActionService;
    }

    @Override
    public String answerGeneralQuestion(String message) {
        Optional<String> actionResponse = personalActionService.handleAction(message);
        if (actionResponse.isPresent()) return actionResponse.get();
        String selected = properties.getProvider() == null ? "" : properties.getProvider().trim().toLowerCase();
        AiProvider provider = providers.get(selected);
        if (provider == null) {
            throw new ChatUnavailableException("BiteCart Assistant is not configured right now. Please try again later.");
        }
        return provider.answer(INSTRUCTIONS + "\n\n" + catalogKnowledgeService.findCatalogContext(message)
                + "\n\n" + personalContextService.findPersonalContext(message), message);
    }
}
