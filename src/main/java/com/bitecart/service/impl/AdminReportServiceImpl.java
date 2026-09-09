package com.bitecart.service.impl;

import com.bitecart.dto.*;
import com.bitecart.entity.Order;
import com.bitecart.entity.OrderItem;
import com.bitecart.entity.OrderStatus;
import com.bitecart.repository.OrderRepository;
import com.bitecart.service.AdminReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AdminReportServiceImpl implements AdminReportService {
    private static final int TOP_RESULT_LIMIT = 5;
    private final OrderRepository orderRepository;
    public AdminReportServiceImpl(OrderRepository orderRepository) { this.orderRepository = orderRepository; }

    @Override public AdminReportDTO reportFor(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) throw new IllegalArgumentException("Choose a valid report date range.");
        List<Order> orders = orderRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        Map<OrderStatus, Long> statusCounts = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) statusCounts.put(status, 0L);
        BigDecimal revenue = BigDecimal.ZERO; long salesOrderCount = 0;
        Map<Long, ProductSales> products = new LinkedHashMap<>(); Map<String, CategorySales> categories = new LinkedHashMap<>();
        for (Order order : orders) {
            statusCounts.compute(order.getOrderStatus(), (ignored, count) -> count + 1);
            if (order.getOrderStatus() == OrderStatus.CANCELLED) continue;
            salesOrderCount++; revenue = revenue.add(order.getTotalAmount());
            for (OrderItem item : order.getItems()) {
                products.computeIfAbsent(item.getProduct().getId(), ignored -> new ProductSales(item.getProductName())).add(item.getQuantity(), item.getSubtotal());
                String categoryName = item.getProduct().getCategory().getName();
                categories.computeIfAbsent(categoryName, ignored -> new CategorySales()).add(item.getQuantity(), item.getSubtotal());
            }
        }
        BigDecimal average = salesOrderCount == 0 ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(salesOrderCount), 2, RoundingMode.HALF_UP);
        List<OrderStatusCountDTO> statuses = statusCounts.entrySet().stream().map(entry -> new OrderStatusCountDTO(entry.getKey(), entry.getValue())).toList();
        List<TopProductSalesDTO> topProducts = products.entrySet().stream().map(entry -> new TopProductSalesDTO(entry.getKey(), entry.getValue().name, entry.getValue().quantity, entry.getValue().revenue)).sorted(Comparator.comparing(TopProductSalesDTO::revenue).reversed().thenComparing(TopProductSalesDTO::quantitySold, Comparator.reverseOrder()).thenComparing(TopProductSalesDTO::productName)).limit(TOP_RESULT_LIMIT).toList();
        List<CategorySalesDTO> categorySales = categories.entrySet().stream().map(entry -> new CategorySalesDTO(entry.getKey(), entry.getValue().quantity, entry.getValue().revenue)).sorted(Comparator.comparing(CategorySalesDTO::revenue).reversed().thenComparing(CategorySalesDTO::quantitySold, Comparator.reverseOrder()).thenComparing(CategorySalesDTO::categoryName)).toList();
        return new AdminReportDTO(startDate, endDate, orders.size(), salesOrderCount, revenue, average, statuses, topProducts, categorySales);
    }
    private static final class ProductSales { private final String name; private long quantity; private BigDecimal revenue = BigDecimal.ZERO; private ProductSales(String name) { this.name = name; } private void add(int units, BigDecimal amount) { quantity += units; revenue = revenue.add(amount); } }
    private static final class CategorySales { private long quantity; private BigDecimal revenue = BigDecimal.ZERO; private void add(int units, BigDecimal amount) { quantity += units; revenue = revenue.add(amount); } }
}
