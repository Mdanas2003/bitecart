package com.bitecart.admin;

import com.bitecart.entity.*;

import com.bitecart.dto.AdminCouponForm;
import com.bitecart.dto.AdminProductForm;
import com.bitecart.dto.AdminUserForm;
import com.bitecart.repository.*;
import com.bitecart.service.AdminService;
import com.bitecart.service.AdminReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminIntegrationTests {
    @Autowired private MockMvc mockMvc; @Autowired private AdminService adminService; @Autowired private AdminReportService adminReportService;
    @Autowired private ReviewRepository reviewRepository; @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private OrderRepository orderRepository; @Autowired private CouponRepository couponRepository;
    @Autowired private WishlistRepository wishlistRepository; @Autowired private CartRepository cartRepository;
    @Autowired private ProductRepository productRepository; @Autowired private CategoryRepository categoryRepository;
    @Autowired private AddressRepository addressRepository; @Autowired private UserRepository userRepository;
    private Category category; private Product product; private User customer;

    @BeforeEach void setUp() { clear(); category = new Category(); category.setName("Admin meals"); category = categoryRepository.save(category); product = product(); customer = user("customer@example.com", Role.USER); }
    @AfterEach void tearDown() { clear(); }

    @Test void adminRoutesRequireAdminRole() throws Exception {
        mockMvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/admin").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("customer@example.com").roles("USER"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin@example.com").roles("ADMIN"))).andExpect(status().isOk());
        mockMvc.perform(get("/admin/reports")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/admin/reports").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("customer@example.com").roles("USER"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/reports").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin@example.com").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminCanCreateProductAndCouponWithCsrf() throws Exception {
        mockMvc.perform(post("/admin/products").with(csrf()).param("name", "New meal").param("description", "Fresh").param("price", "50.00").param("stockQuantity", "4").param("categoryId", category.getId().toString()).param("active", "true"))
                .andExpect(status().is3xxRedirection());
        assertThat(productRepository.findAll()).anyMatch(value -> value.getName().equals("New meal"));
        mockMvc.perform(post("/admin/products").param("name", "Blocked")).andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/coupons").with(csrf()).param("code", "ADMIN10").param("discountType", "PERCENTAGE").param("discountValue", "10.00").param("minimumOrderAmount", "0.00").param("expiryDate", LocalDateTime.now().plusDays(1).withNano(0).toString()).param("active", "true"))
                .andExpect(status().is3xxRedirection());
        assertThat(couponRepository.findByCodeIgnoreCase("ADMIN10")).isPresent();
    }

    @Test void adminServiceManagesUserOrderAndReview() {
        AdminUserForm userForm = new AdminUserForm(); userForm.setRole(Role.ADMIN); userForm.setEnabled(false); adminService.updateUser(customer.getId(), userForm);
        assertThat(userRepository.findById(customer.getId()).orElseThrow().getRole()).isEqualTo(Role.ADMIN);
        assertThat(userRepository.findById(customer.getId()).orElseThrow().isEnabled()).isFalse();

        Order order = deliveredOrder(); adminService.updateOrderStatus(order.getId(), OrderStatus.OUT_FOR_DELIVERY);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getOrderStatus()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);
        Review review = new Review(); review.setUser(customer); review.setProduct(product); review.setOrder(order); review.setRating(4); review.setComment("Tasty meal"); review = reviewRepository.save(review); product.setRating(new BigDecimal("4.0")); productRepository.save(product);
        adminService.deleteReview(review.getId()); assertThat(reviewRepository.count()).isZero(); assertThat(productRepository.findById(product.getId()).orElseThrow().getRating()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test void reportsAggregateNonCancelledOrdersAndRespectDateRange() {
        deliveredOrder();
        Order confirmed = deliveredOrder(); confirmed.setOrderStatus(OrderStatus.CONFIRMED); orderRepository.save(confirmed);
        Order cancelled = deliveredOrder(); cancelled.setOrderStatus(OrderStatus.CANCELLED); orderRepository.save(cancelled);
        var report = adminReportService.reportFor(LocalDate.now(), LocalDate.now());
        assertThat(report.totalOrders()).isEqualTo(3);
        assertThat(report.salesOrderCount()).isEqualTo(2);
        assertThat(report.totalRevenue()).isEqualByComparingTo("180.00");
        assertThat(report.averageOrderValue()).isEqualByComparingTo("90.00");
        assertThat(report.orderStatusCounts()).anySatisfy(row -> { assertThat(row.status()).isEqualTo(OrderStatus.CANCELLED); assertThat(row.count()).isEqualTo(1); });
        assertThat(report.topProducts()).singleElement().satisfies(row -> { assertThat(row.quantitySold()).isEqualTo(2); assertThat(row.revenue()).isEqualByComparingTo("180.00"); });
        assertThat(adminReportService.reportFor(LocalDate.now().minusDays(1), LocalDate.now().minusDays(1)).totalOrders()).isZero();
    }

    private Product product() { Product value = new Product(); value.setName("Admin meal"); value.setDescription("Meal"); value.setPrice(new BigDecimal("90.00")); value.setStockQuantity(10); value.setCategory(category); value.setActive(true); return productRepository.save(value); }
    private User user(String email, Role role) { User value = new User(); value.setFirstName("Customer"); value.setLastName("One"); value.setEmail(email); value.setPassword("encoded"); value.setPhone("5550100"); value.setRole(role); return userRepository.save(value); }
    private Order deliveredOrder() { Address address = new Address(); address.setUser(customer); address.setAddressLine1("Street"); address.setCity("Chennai"); address.setState("TN"); address.setPostalCode("600001"); address.setCountry("India"); address.setPhone("5550100"); address = addressRepository.save(address); Order order = new Order(); order.setUser(customer); order.setAddress(address); order.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY); order.setOrderStatus(OrderStatus.DELIVERED); order.setSubtotal(new BigDecimal("90.00")); order.setDiscountAmount(BigDecimal.ZERO); order.setDeliveryFee(BigDecimal.ZERO); order.setTotalAmount(new BigDecimal("90.00")); OrderItem item = new OrderItem(); item.setProduct(product); item.setProductName(product.getName()); item.setQuantity(1); item.setPrice(new BigDecimal("90.00")); item.setSubtotal(new BigDecimal("90.00")); order.addItem(item); return orderRepository.save(order); }
    private void clear() { reviewRepository.deleteAll(); orderItemRepository.deleteAll(); orderRepository.deleteAll(); couponRepository.deleteAll(); wishlistRepository.deleteAll(); cartRepository.deleteAll(); productRepository.deleteAll(); categoryRepository.deleteAll(); addressRepository.deleteAll(); userRepository.deleteAll(); }
}
