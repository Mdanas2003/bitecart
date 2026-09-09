package com.bitecart.dto;

import org.springframework.data.domain.Sort;

import java.util.Arrays;

public enum ProductSort {
    NEWEST("newest", Sort.by(Sort.Direction.DESC, "createdAt")),
    PRICE_ASC("priceAsc", Sort.by(Sort.Direction.ASC, "price")),
    PRICE_DESC("priceDesc", Sort.by(Sort.Direction.DESC, "price")),
    RATING_DESC("ratingDesc", Sort.by(Sort.Direction.DESC, "rating")),
    NAME_ASC("nameAsc", Sort.by(Sort.Direction.ASC, "name"));

    private final String parameterValue;
    private final Sort sort;

    ProductSort(String parameterValue, Sort sort) {
        this.parameterValue = parameterValue;
        this.sort = sort;
    }

    public String getParameterValue() {
        return parameterValue;
    }

    public Sort getSort() {
        return sort;
    }

    public static ProductSort from(String value) {
        return Arrays.stream(values())
                .filter(sort -> sort.parameterValue.equals(value))
                .findFirst()
                .orElse(NEWEST);
    }
}
