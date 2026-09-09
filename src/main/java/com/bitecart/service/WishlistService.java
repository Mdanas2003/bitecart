package com.bitecart.service;

import com.bitecart.dto.WishlistDTO;

public interface WishlistService {

    WishlistDTO getCurrentUsersWishlist();

    WishlistDTO addProduct(Long productId);

    WishlistDTO removeItem(Long wishlistItemId);

    boolean isProductWishlisted(Long productId);
}
