package com.bitecart.service;

import com.bitecart.dto.CartDTO;

public interface CartService {

    CartDTO getCurrentUsersCart();

    CartDTO addProduct(Long productId, int quantity);

    CartDTO updateItemQuantity(Long cartItemId, int quantity);

    CartDTO removeItem(Long cartItemId);

    CartDTO clearCart();
}
