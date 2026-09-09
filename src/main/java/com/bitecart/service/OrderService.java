package com.bitecart.service;

import com.bitecart.dto.OrderDTO;
import com.bitecart.entity.PaymentMethod;

public interface OrderService {

    OrderDTO createOrder(Long addressId, PaymentMethod paymentMethod, String couponCode);

    OrderDTO getCurrentUsersOrder(Long orderId);
}
