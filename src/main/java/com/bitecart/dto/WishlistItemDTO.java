package com.bitecart.dto;

import java.math.BigDecimal;

public record WishlistItemDTO(
        Long id,
        Long productId,
        String productName,
        String imageUrl,
        BigDecimal price,
        BigDecimal discountPrice,
        String categoryName,
        boolean active
) {
}
