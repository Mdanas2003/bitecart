package com.bitecart.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdminReportDTO(LocalDate startDate, LocalDate endDate, long totalOrders,
                             long salesOrderCount, BigDecimal totalRevenue, BigDecimal averageOrderValue,
                             List<OrderStatusCountDTO> orderStatusCounts,
                             List<TopProductSalesDTO> topProducts,
                             List<CategorySalesDTO> categorySales) {
}
