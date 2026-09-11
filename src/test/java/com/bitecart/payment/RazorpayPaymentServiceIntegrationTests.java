package com.bitecart.payment;

import com.bitecart.dto.OrderDTO;
import com.bitecart.dto.RazorpayCheckoutDTO;
import com.bitecart.entity.Address;
import com.bitecart.entity.Category;
import com.bitecart.entity.Order;
import com.bitecart.entity.PaymentMethod;
import com.bitecart.entity.PaymentStatus;
import com.bitecart.entity.Product;
import com.bitecart.entity.Role;
import com.bitecart.entity.User;
import com.bitecart.exception.PaymentGatewayException;
import com.bitecart.exception.PaymentVerificationException;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.repository.AddressRepository;
import com.bitecart.repository.CartRepository;
import com.bitecart.repository.CategoryRepository;
import com.bitecart.repository.OrderItemRepository;
import com.bitecart.repository.OrderRepository;
import com.bitecart.repository.ProductRepository;
import com.bitecart.repository.UserRepository;
import com.bitecart.repository.WishlistRepository;
import com.bitecart.service.CartService;
import com.bitecart.service.OrderService;
import com.bitecart.service.RazorpayPaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {"razorpay.key-id=rzp_test_mock", "razorpay.key-secret=test-only-secret"})
@ActiveProfiles("test")
class RazorpayPaymentServiceIntegrationTests {

    @Autowired private RazorpayPaymentService razorpayPaymentService;
    @Autowired private OrderService orderService;
    @Autowired private CartService cartService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private WishlistRepository wishlistRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private RazorpayGateway razorpayGateway;

    private User owner;
    private Address address;
    private Product product;

    @BeforeEach
    void setUp() {
        clear();
        owner = userRepository.save(user("payment-owner@example.com"));
        address = addressRepository.save(address(owner));
        product = productRepository.save(product());
        authenticate(owner.getEmail());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        clear();
    }

    @Test
    void createsOneTrustedRazorpayOrderAndMarksPaidOnlyAfterValidSignature() throws Exception {
        OrderDTO order = onlineOrder();
        when(razorpayGateway.createOrder(anyLong(), anyString()))
                .thenReturn(new RazorpayGatewayOrder("order_test_123", 12000L, "INR"));

        RazorpayCheckoutDTO checkout = razorpayPaymentService.startPayment(order.id());
        RazorpayCheckoutDTO retry = razorpayPaymentService.startPayment(order.id());

        assertThat(checkout.amountInPaise()).isEqualTo(12000L);
        assertThat(checkout.keyId()).isEqualTo("rzp_test_mock");
        assertThat(retry.razorpayOrderId()).isEqualTo("order_test_123");
        ArgumentCaptor<Long> amount = ArgumentCaptor.forClass(Long.class);
        verify(razorpayGateway, times(1)).createOrder(amount.capture(), anyString());
        assertThat(amount.getValue()).isEqualTo(12000L);

        String paymentId = "pay_test_456";
        razorpayPaymentService.verifyPayment(order.id(), "order_test_123", paymentId,
                signature("order_test_123", paymentId));

        Order persisted = orderRepository.findById(order.id()).orElseThrow();
        assertThat(persisted.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(persisted.getRazorpayPaymentId()).isEqualTo(paymentId);
    }

    @Test
    void rejectedSignatureLeavesOrderPendingAndDoesNotAcceptClientOrderId() {
        OrderDTO order = onlineOrder();
        when(razorpayGateway.createOrder(anyLong(), anyString()))
                .thenReturn(new RazorpayGatewayOrder("order_test_123", 12000L, "INR"));
        razorpayPaymentService.startPayment(order.id());

        assertThatThrownBy(() -> razorpayPaymentService.verifyPayment(order.id(), "order_tampered", "pay_test_456", "bad"))
                .isInstanceOf(PaymentVerificationException.class);
        assertThat(orderRepository.findById(order.id()).orElseThrow().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void gatewayFailureLeavesPaymentPendingForSafeRetryAndOwnershipIsEnforced() {
        OrderDTO order = onlineOrder();
        doThrow(new PaymentGatewayException("Online payment could not be started. Please try again."))
                .doReturn(new RazorpayGatewayOrder("order_test_retry", 12000L, "INR"))
                .when(razorpayGateway).createOrder(anyLong(), anyString());

        assertThatThrownBy(() -> razorpayPaymentService.startPayment(order.id())).isInstanceOf(PaymentGatewayException.class);
        assertThat(orderRepository.findById(order.id()).orElseThrow().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(razorpayPaymentService.startPayment(order.id()).razorpayOrderId()).isEqualTo("order_test_retry");

        User other = userRepository.save(user("payment-other@example.com"));
        authenticate(other.getEmail());
        assertThatThrownBy(() -> razorpayPaymentService.getPendingPayment(order.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void mapsRazorpayOrdersApiAmountFieldToPaise() throws Exception {
        RazorpayGatewayOrder gatewayOrder = objectMapper.readValue(
                "{\"id\":\"order_test_123\",\"amount\":12000,\"currency\":\"INR\"}", RazorpayGatewayOrder.class);

        assertThat(gatewayOrder.amountInPaise()).isEqualTo(12000L);
    }

    private OrderDTO onlineOrder() {
        cartService.addProduct(product.getId(), 2);
        return orderService.createOrder(address.getId(), PaymentMethod.ONLINE, null);
    }

    private String signature(String orderId, String paymentId) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-only-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte value : digest) { hex.append(String.format("%02x", value)); }
        return hex.toString();
    }

    private void clear() {
        orderItemRepository.deleteAll(); orderRepository.deleteAll(); wishlistRepository.deleteAll(); cartRepository.deleteAll();
        productRepository.deleteAll(); categoryRepository.deleteAll(); addressRepository.deleteAll(); userRepository.deleteAll();
    }

    private User user(String email) {
        User user = new User(); user.setFirstName("Payment"); user.setLastName("User"); user.setEmail(email);
        user.setPassword("encoded-password"); user.setPhone("9876543210"); user.setRole(Role.USER); return user;
    }

    private Address address(User user) {
        Address address = new Address(); address.setUser(user); address.setAddressLine1("12 Market Street");
        address.setCity("Chennai"); address.setState("Tamil Nadu"); address.setPostalCode("600001");
        address.setCountry("India"); address.setPhone("9876543210"); return address;
    }

    private Product product() {
        Category category = new Category(); category.setName("Payment meals"); category = categoryRepository.save(category);
        Product product = new Product(); product.setName("Payment meal"); product.setDescription("Payment meal");
        product.setPrice(new BigDecimal("60.00")); product.setStockQuantity(5); product.setCategory(category); product.setActive(true); return product;
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
