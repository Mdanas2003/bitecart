package com.bitecart.dto;

public record AddressDTO(
        Long id,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        String phone,
        boolean defaultAddress
) {
}
