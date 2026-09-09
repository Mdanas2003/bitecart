package com.bitecart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartDTO(
        Long id,
        List<CartItemDTO> items,
        Integer totalItems,
        BigDecimal subtotal
) {
}
