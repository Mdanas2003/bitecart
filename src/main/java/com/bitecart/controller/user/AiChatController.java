package com.bitecart.controller.user;

import com.bitecart.dto.ChatMessageRequest;
import com.bitecart.dto.ChatMessageResponse;
import com.bitecart.exception.ChatUnavailableException;
import com.bitecart.service.AiChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@Validated
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/messages")
    public ResponseEntity<?> sendMessage(@Valid @RequestBody ChatMessageRequest request) {
        try {
            return ResponseEntity.ok(new ChatMessageResponse(aiChatService.answerGeneralQuestion(request.message())));
        } catch (ChatUnavailableException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", exception.getMessage()));
        }
    }
}
