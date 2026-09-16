package com.bitecart.service;

import java.util.Optional;

/**
 * Executes a deliberately small set of authenticated, user-owned assistant actions.
 * Requests outside the allowlist are left to the conversational provider.
 */
public interface AiPersonalActionService {

    Optional<String> handleAction(String message);
}
