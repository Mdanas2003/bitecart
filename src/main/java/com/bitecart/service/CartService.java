package com.bitecart.service;

import com.bitecart.dto.CartDTO;

import java.util.Optional;

public interface CartService {

    CartDTO getCurrentUsersCart();

    /**
     * Returns the authenticated user's existing cart without creating one.
     * This is intended for read-only consumers such as the assistant context.
     */
    Optional<CartDTO> findCurrentUsersCart();

    CartDTO addProduct(Long productId, int quantity);

    CartDTO updateItemQuantity(Long cartItemId, int quantity);

    CartDTO removeItem(Long cartItemId);

    CartDTO clearCart();
}
