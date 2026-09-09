package com.bitecart.service;

import com.bitecart.dto.OrderDTO;
import com.bitecart.dto.OrderTrackingDTO;
import com.bitecart.entity.PaymentMethod;

import java.util.List;

public interface OrderService {

    OrderDTO createOrder(Long addressId, PaymentMethod paymentMethod, String couponCode);

    OrderDTO getCurrentUsersOrder(Long orderId);

    List<OrderDTO> getCurrentUsersOrders();

    OrderTrackingDTO getCurrentUsersOrderTracking(Long orderId);
}
