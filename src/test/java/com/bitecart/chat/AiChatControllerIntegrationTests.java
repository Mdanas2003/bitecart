package com.bitecart.chat;

import com.bitecart.exception.ChatUnavailableException;
import com.bitecart.service.AiChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiChatControllerIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @MockBean private AiChatService aiChatService;

    @Test
    void requiresAuthenticationForChatMessages() throws Exception {
        mockMvc.perform(post("/api/chat/messages").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"How does BiteCart work?\"}"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "USER")
    void sendsAuthenticatedCsrfProtectedQuestionToGeneralChatService() throws Exception {
        when(aiChatService.answerGeneralQuestion("How does BiteCart work?"))
                .thenReturn("Browse the menu, add items, and check out when ready.");

        mockMvc.perform(post("/api/chat/messages").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"How does BiteCart work?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Browse the menu, add items, and check out when ready."));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "USER")
    void returnsSafeMessageWhenAssistantIsUnavailable() throws Exception {
        when(aiChatService.answerGeneralQuestion(anyString()))
                .thenThrow(new ChatUnavailableException("BiteCart Assistant is temporarily unavailable. Please try again later."));

        mockMvc.perform(post("/api/chat/messages").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Can you help?\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("BiteCart Assistant is temporarily unavailable. Please try again later."));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "USER")
    void enforcesCsrfForChatMessages() throws Exception {
        mockMvc.perform(post("/api/chat/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Can you help?\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "USER")
    void rendersFloatingChatWithAVisibleComposerOutsideThePageHeader() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"ai-chat\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"ai-chat-composer\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ask a general BiteCart question...")));
    }
}
