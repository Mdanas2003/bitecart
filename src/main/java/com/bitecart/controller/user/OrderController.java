package com.bitecart.controller.user;

import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        model.addAttribute("orders", orderService.getCurrentUsersOrders());
        return "orders";
    }

    @GetMapping("/orders/{orderId}")
    public String orderDetails(@PathVariable Long orderId, Model model) {
        try {
            model.addAttribute("order", orderService.getCurrentUsersOrder(orderId));
            return "order-details";
        } catch (ResourceNotFoundException exception) {
            return "order-not-found";
        }
    }

    @GetMapping("/orders/{orderId}/success")
    public String orderSuccess(@PathVariable Long orderId, Model model) {
        try {
            model.addAttribute("order", orderService.getCurrentUsersOrder(orderId));
            return "order-success";
        } catch (ResourceNotFoundException exception) {
            return "order-not-found";
        }
    }

    @GetMapping("/orders/{orderId}/tracking")
    public String orderTracking(@PathVariable Long orderId, Model model) {
        try {
            model.addAttribute("tracking", orderService.getCurrentUsersOrderTracking(orderId));
            return "order-tracking";
        } catch (ResourceNotFoundException exception) {
            return "order-not-found";
        }
    }
}
