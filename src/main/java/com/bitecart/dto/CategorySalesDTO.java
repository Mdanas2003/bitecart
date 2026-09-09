package com.bitecart.dto;

import java.math.BigDecimal;

public record CategorySalesDTO(String categoryName, long quantitySold, BigDecimal revenue) {
}
