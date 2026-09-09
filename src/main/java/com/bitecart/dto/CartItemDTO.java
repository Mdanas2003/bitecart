package com.bitecart.dto;

import java.math.BigDecimal;

public record CartItemDTO(
        Long id,
        Long productId,
        String productName,
        String imageUrl,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
