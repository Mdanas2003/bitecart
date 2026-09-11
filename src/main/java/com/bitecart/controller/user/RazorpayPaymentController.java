package com.bitecart.controller.user;

import com.bitecart.dto.OrderDTO;
import com.bitecart.exception.PaymentGatewayException;
import com.bitecart.exception.PaymentVerificationException;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.service.OrderService;
import com.bitecart.service.RazorpayPaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RazorpayPaymentController {

    private final OrderService orderService;
    private final RazorpayPaymentService razorpayPaymentService;

    public RazorpayPaymentController(OrderService orderService, RazorpayPaymentService razorpayPaymentService) {
        this.orderService = orderService;
        this.razorpayPaymentService = razorpayPaymentService;
    }

    @GetMapping("/orders/{orderId}/payment")
    public String payment(@PathVariable Long orderId, Model model, RedirectAttributes redirectAttributes) {
        try {
            OrderDTO order = orderService.getCurrentUsersOrder(orderId);
            model.addAttribute("order", order);
            try {
                model.addAttribute("razorpayCheckout", razorpayPaymentService.getPendingPayment(orderId));
            } catch (PaymentGatewayException exception) {
                model.addAttribute("paymentError", exception.getMessage());
            }
            return "razorpay-payment";
        } catch (ResourceNotFoundException exception) {
            redirectAttributes.addFlashAttribute("paymentError", "That order is unavailable.");
            return "redirect:/orders";
        }
    }

    @PostMapping("/orders/{orderId}/payment/retry")
    public String retryPayment(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            razorpayPaymentService.startPayment(orderId);
            redirectAttributes.addFlashAttribute("paymentSuccess", "Online payment is ready to continue.");
        } catch (ResourceNotFoundException | PaymentGatewayException exception) {
            redirectAttributes.addFlashAttribute("paymentError", exception.getMessage());
        }
        return "redirect:/orders/" + orderId + "/payment";
    }

    @PostMapping("/orders/{orderId}/payment/verify")
    public String verifyPayment(@PathVariable Long orderId,
                                @RequestParam(name = "razorpay_order_id", required = false) String razorpayOrderId,
                                @RequestParam(name = "razorpay_payment_id", required = false) String razorpayPaymentId,
                                @RequestParam(name = "razorpay_signature", required = false) String signature,
                                RedirectAttributes redirectAttributes) {
        try {
            razorpayPaymentService.verifyPayment(orderId, razorpayOrderId, razorpayPaymentId, signature);
            redirectAttributes.addFlashAttribute("paymentSuccess", "Payment verified successfully.");
            return "redirect:/orders/" + orderId;
        } catch (ResourceNotFoundException | PaymentGatewayException | PaymentVerificationException exception) {
            redirectAttributes.addFlashAttribute("paymentError", exception.getMessage());
            return "redirect:/orders/" + orderId + "/payment";
        }
    }
}
