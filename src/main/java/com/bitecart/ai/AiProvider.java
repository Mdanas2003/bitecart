package com.bitecart.ai;

public interface AiProvider {

    String providerName();

    String answer(String instructions, String message);
}
