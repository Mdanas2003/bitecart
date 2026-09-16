package com.bitecart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private String provider = "openai";

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
