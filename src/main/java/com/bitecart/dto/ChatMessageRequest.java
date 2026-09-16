package com.bitecart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotBlank(message = "Please enter a message.")
        @Size(max = 1000, message = "Messages can contain up to 1000 characters.")
        String message
) {
}
