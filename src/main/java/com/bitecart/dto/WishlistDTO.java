package com.bitecart.dto;

import java.util.List;

public record WishlistDTO(
        List<WishlistItemDTO> items,
        Integer totalItems
) {
}
