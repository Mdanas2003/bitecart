package com.bitecart.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiProviderTest {

    @Test
    void redactsCredentialsFromDiagnosticMessages() {
        String diagnostic = com.bitecart.ai.OpenAiProvider.sanitizeForLog(
                "Request failed with Authorization: Bearer sk-test-secret-value");

        assertThat(diagnostic).contains("Bearer [REDACTED]");
        assertThat(diagnostic).doesNotContain("sk-test-secret-value");
    }
}
