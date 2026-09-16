package com.bitecart.service;

/**
 * Supplies bounded, read-only context for the authenticated customer's assistant request.
 * Implementations must use ownership-scoped services and must not accept a user identifier.
 */
public interface PersonalContextService {

    String findPersonalContext(String customerQuestion);
}
