package com.bitecart.entity;

import com.bitecart.dto.OrderTrackingDTO;
import com.bitecart.dto.ReviewSubmissionRequest;
import com.bitecart.exception.DuplicateReviewException;
import com.bitecart.exception.ReviewNotAllowedException;
import com.bitecart.repository.AddressRepository;
import com.bitecart.repository.CartRepository;
import com.bitecart.repository.CategoryRepository;
import com.bitecart.repository.CouponRepository;
import com.bitecart.repository.OrderItemRepository;
import com.bitecart.repository.OrderRepository;
import com.bitecart.repository.ProductRepository;
import com.bitecart.repository.ReviewRepository;
import com.bitecart.repository.UserRepository;
import com.bitecart.repository.WishlistRepository;
import com.bitecart.service.OrderService;
import com.bitecart.service.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderTrackingReviewIntegrationTests {

    @Autowired private ReviewService reviewService;
    @Autowired private OrderService orderService;
    @Autowired private MockMvc mockMvc;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private CouponRepository couponRepository;
    @Autowired private WishlistRepository wishlistRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;

    private User firstUser;
    private User secondUser;
    private Product product;
    private Order deliveredOrder;

    @BeforeEach
    void setUp() {
        clearDatabase();
        firstUser = userRepository.save(testUser("one@example.com"));
        secondUser = userRepository.save(testUser("two@example.com"));
        product = productRepository.save(product());
        deliveredOrder = orderRepository.save(order(firstUser, product, OrderStatus.DELIVERED));
        authenticate(firstUser.getEmail());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        clearDatabase();
    }

    @Test
    void createsReviewOnlyForDeliveredPurchasedProductAndUpdatesRating() {
        ReviewSubmissionRequest request = request(deliveredOrder.getId(), 5, "Fresh, warm, and delicious.");

        reviewService.submitReview(product.getId(), request);

        assertThat(reviewRepository.findByProductIdOrderByCreatedAtDesc(product.getId())).hasSize(1);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getRating()).isEqualByComparingTo("5.0");
        assertThat(reviewService.getCurrentUsersReviewableOrders(product.getId())).isEmpty();
        assertThatThrownBy(() -> reviewService.submitReview(product.getId(), request)).isInstanceOf(DuplicateReviewException.class);
    }

    @Test
    void rejectsUndeliveredOrUnpurchasedProductsAndAnotherUsersOrders() {
        Order placedOrder = orderRepository.save(order(firstUser, product, OrderStatus.PLACED));
        assertThatThrownBy(() -> reviewService.submitReview(product.getId(), request(placedOrder.getId(), 4, "Still waiting.")))
                .isInstanceOf(ReviewNotAllowedException.class);

        Product unpurchased = productRepository.save(secondProduct());
        assertThatThrownBy(() -> reviewService.submitReview(unpurchased.getId(), request(deliveredOrder.getId(), 4, "Not purchased.")))
                .isInstanceOf(ReviewNotAllowedException.class);

        Order secondUsersDeliveredOrder = orderRepository.save(order(secondUser, product, OrderStatus.DELIVERED));
        assertThatThrownBy(() -> reviewService.submitReview(product.getId(), request(secondUsersDeliveredOrder.getId(), 4, "Not mine.")))
                .isInstanceOf(ReviewNotAllowedException.class);
    }

    @Test
    void returnsTrackingForOwnerAndDeniesOtherUsers() {
        OrderTrackingDTO tracking = orderService.getCurrentUsersOrderTracking(deliveredOrder.getId());
        assertThat(tracking.orderStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(tracking.steps()).hasSize(5);
        assertThat(tracking.steps()).allMatch(step -> step.completed());

        authenticate(secondUser.getEmail());
        assertThatThrownBy(() -> orderService.getCurrentUsersOrderTracking(deliveredOrder.getId()))
                .isInstanceOf(com.bitecart.exception.ResourceNotFoundException.class);
    }

    @Test
    @WithMockUser(username = "one@example.com", roles = "USER")
    void reviewControllerAndTrackingEnforceCsrfAndOwnership() throws Exception {
        mockMvc.perform(post("/products/{id}/reviews", product.getId()).with(csrf())
                        .param("orderId", deliveredOrder.getId().toString()).param("rating", "4").param("comment", "Really good meal."))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("reviewSuccess"));
        mockMvc.perform(get("/orders/{id}/tracking", deliveredOrder.getId()))
                .andExpect(status().isOk()).andExpect(view().name("order-tracking"));
        mockMvc.perform(post("/products/{id}/reviews", product.getId()).param("orderId", deliveredOrder.getId().toString())
                        .param("rating", "4").param("comment", "No csrf."))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/orders/{id}/tracking", deliveredOrder.getId()).with(user("two@example.com").roles("USER")))
                .andExpect(status().isOk()).andExpect(view().name("order-not-found"));
    }

    @Test
    void productDetailsShowsReviewUiOnlyForEligibleDeliveredPurchase() throws Exception {
        mockMvc.perform(get("/products/{id}", product.getId()).with(user(firstUser.getEmail()).roles("USER")))
                .andExpect(status().isOk()).andExpect(view().name("product-details"))
                .andExpect(model().attributeExists("reviews", "reviewableOrders"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Write a Review")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("rating-5")));

        mockMvc.perform(get("/products/{id}", product.getId()).with(anonymous()))
                .andExpect(status().isOk()).andExpect(view().name("product-details"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Write a Review"))));
    }

    private void clearDatabase() {
        reviewRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        couponRepository.deleteAll();
        wishlistRepository.deleteAll();
        cartRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        addressRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User testUser(String email) {
        User user = new User(); user.setFirstName("Test"); user.setLastName("User"); user.setEmail(email);
        user.setPassword("encoded-password"); user.setPhone("5550100"); user.setRole(Role.USER); return user;
    }

    private Product product() {
        Category category = new Category(); category.setName("Review meals"); category = categoryRepository.save(category);
        Product value = new Product(); value.setName("Review meal"); value.setDescription("Review meal");
        value.setPrice(new BigDecimal("90.00")); value.setStockQuantity(10); value.setCategory(category); value.setActive(true); return value;
    }

    private Product secondProduct() {
        Product value = new Product(); value.setName("Unpurchased meal"); value.setDescription("Unpurchased meal");
        value.setPrice(new BigDecimal("70.00")); value.setStockQuantity(10); value.setCategory(product.getCategory()); value.setActive(true); return value;
    }

    private Order order(User user, Product orderedProduct, OrderStatus status) {
        Address address = new Address(); address.setUser(user); address.setAddressLine1("12 Market Street"); address.setCity("Chennai");
        address.setState("Tamil Nadu"); address.setPostalCode("600001"); address.setCountry("India"); address.setPhone("5550100");
        address = addressRepository.save(address);
        Order order = new Order(); order.setUser(user); order.setAddress(address); order.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        order.setOrderStatus(status); order.setSubtotal(new BigDecimal("90.00")); order.setDiscountAmount(BigDecimal.ZERO);
        order.setDeliveryFee(BigDecimal.ZERO); order.setTotalAmount(new BigDecimal("90.00"));
        OrderItem item = new OrderItem(); item.setProduct(orderedProduct); item.setProductName(orderedProduct.getName()); item.setQuantity(1);
        item.setPrice(new BigDecimal("90.00")); item.setSubtotal(new BigDecimal("90.00")); order.addItem(item); return order;
    }

    private ReviewSubmissionRequest request(Long orderId, int rating, String comment) {
        ReviewSubmissionRequest request = new ReviewSubmissionRequest(); request.setOrderId(orderId); request.setRating(rating); request.setComment(comment); return request;
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
