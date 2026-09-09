package com.bitecart.dto;

public record CategoryDTO(
        Long id,
        String name,
        String description,
        String imageUrl,
        boolean active
) {
}
