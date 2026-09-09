package com.bitecart.dto;

import java.math.BigDecimal;

public record ProductDTO(
        Long id,
        String name,
        String description,
        BigDecimal price,
        BigDecimal discountPrice,
        String imageUrl,
        Integer stockQuantity,
        Long categoryId,
        String categoryName,
        BigDecimal rating,
        boolean featured,
        boolean active
) {
    public BigDecimal currentPrice() {
        return discountPrice != null ? discountPrice : price;
    }

    public boolean hasDiscount() {
        return discountPrice != null && discountPrice.compareTo(price) < 0;
    }
}
