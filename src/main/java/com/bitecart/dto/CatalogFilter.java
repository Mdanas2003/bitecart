package com.bitecart.dto;

import java.math.BigDecimal;

public record CatalogFilter(
        String search,
        Long categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal minRating,
        ProductSort sort,
        int page,
        int size
) {
    public CatalogFilter {
        search = search == null ? null : search.trim();
        sort = sort == null ? ProductSort.NEWEST : sort;
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 24);
    }
}
