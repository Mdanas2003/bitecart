package com.bitecart.dto;

import java.math.BigDecimal;

public record TopProductSalesDTO(Long productId, String productName, long quantitySold, BigDecimal revenue) {
}
