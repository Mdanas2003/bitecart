package com.bitecart.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AiProperties.class, OpenAiProperties.class, OllamaProperties.class})
public class OpenAiConfig {
}
