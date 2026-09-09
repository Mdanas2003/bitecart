package com.bitecart.entity;

import com.bitecart.repository.AddressRepository;
import com.bitecart.repository.CartRepository;
import com.bitecart.repository.CategoryRepository;
import com.bitecart.repository.CouponRepository;
import com.bitecart.repository.OrderItemRepository;
import com.bitecart.repository.OrderRepository;
import com.bitecart.repository.ProductRepository;
import com.bitecart.repository.UserRepository;
import com.bitecart.repository.WishlistRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CheckoutOrderControllerIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private CouponRepository couponRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private WishlistRepository wishlistRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;

    private User firstUser;
    private Address firstUsersAddress;
    private Product product;

    @BeforeEach
    void setUp() {
        clearDatabase();
        firstUser = userRepository.save(testUser("one@example.com"));
        firstUsersAddress = addressRepository.save(address(firstUser));
        userRepository.save(testUser("two@example.com"));
        product = productRepository.save(product());
    }

    @AfterEach
    void tearDown() {
        clearDatabase();
    }

    @Test
    void anonymousUsersAreRedirectedFromCheckoutAndOrders() throws Exception {
        mockMvc.perform(get("/checkout")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/orders")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "one@example.com", roles = "USER")
    void checkoutAppliesCouponPlacesOrderAndShowsHistory() throws Exception {
        mockMvc.perform(post("/cart/items").with(csrf()).param("productId", product.getId().toString()).param("quantity", "2"))
                .andExpect(status().is3xxRedirection());
        couponRepository.save(coupon("SAVE10"));

        MvcResult couponResult = mockMvc.perform(post("/checkout/coupon").with(csrf()).param("code", "SAVE10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout"))
                .andExpect(flash().attributeExists("checkoutSuccess"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) couponResult.getRequest().getSession(false);

        mockMvc.perform(get("/checkout").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"))
                .andExpect(model().attributeExists("cart", "addresses", "couponCode", "couponDiscount"));

        mockMvc.perform(post("/checkout/order").session(session).with(csrf())
                        .param("addressId", firstUsersAddress.getId().toString())
                        .param("paymentMethod", "CASH_ON_DELIVERY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/orders/*/success"));

        assertThat(orderRepository.count()).isEqualTo(1);
        Order order = orderRepository.findAll().get(0);
        assertThat(order.getCouponCode()).isEqualTo("SAVE10");
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("12.00");
        assertThat(couponRepository.findByCodeIgnoreCase("SAVE10").orElseThrow().getUsedCount()).isEqualTo(1);
        assertThat(cartRepository.findByUserEmailIgnoreCase("one@example.com").orElseThrow().getItems()).isEmpty();
        assertThat(session.getAttribute("checkoutCouponCode")).isNull();

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"));
        mockMvc.perform(get("/orders/{id}", order.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("order-details"));
        mockMvc.perform(get("/orders/{id}/success", order.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("order-success"));
    }

    @Test
    @WithMockUser(username = "one@example.com", roles = "USER")
    void invalidCouponAndCheckoutInputReturnSafeFeedback() throws Exception {
        mockMvc.perform(post("/checkout/coupon").with(csrf()).param("code", "UNKNOWN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("checkoutError"));

        mockMvc.perform(post("/checkout/order").with(csrf()).param("paymentMethod", "ONLINE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout"))
                .andExpect(flash().attributeExists("checkoutError"));
    }

    @Test
    @WithMockUser(username = "one@example.com", roles = "USER")
    void couponCanBeRemovedAndCheckoutPostsRequireCsrf() throws Exception {
        mockMvc.perform(post("/cart/items").with(csrf()).param("productId", product.getId().toString()).param("quantity", "1"))
                .andExpect(status().is3xxRedirection());
        couponRepository.save(coupon("REMOVE10"));

        MvcResult applyResult = mockMvc.perform(post("/checkout/coupon").with(csrf()).param("code", "REMOVE10"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession session = (MockHttpSession) applyResult.getRequest().getSession(false);
        mockMvc.perform(post("/checkout/coupon/remove").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("checkoutSuccess"));
        assertThat(session.getAttribute("checkoutCouponCode")).isNull();
        assertThat(couponRepository.findByCodeIgnoreCase("REMOVE10").orElseThrow().getUsedCount()).isZero();

        mockMvc.perform(post("/checkout/coupon").param("code", "REMOVE10"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crossUserRequestsCannotUseAnotherAddressOrViewAnotherOrder() throws Exception {
        createOrderForFirstUser();
        Long orderId = orderRepository.findAll().get(0).getId();

        mockMvc.perform(get("/orders/{id}", orderId).with(user("two@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("order-not-found"));

        mockMvc.perform(post("/cart/items").with(user("two@example.com").roles("USER")).with(csrf())
                        .param("productId", product.getId().toString()).param("quantity", "1"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/checkout/order").with(user("two@example.com").roles("USER")).with(csrf())
                        .param("addressId", firstUsersAddress.getId().toString()).param("paymentMethod", "ONLINE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout"))
                .andExpect(flash().attributeExists("checkoutError"));
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    private void createOrderForFirstUser() throws Exception {
        mockMvc.perform(post("/cart/items").with(user("one@example.com").roles("USER")).with(csrf())
                        .param("productId", product.getId().toString()).param("quantity", "1"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/checkout/order").with(user("one@example.com").roles("USER")).with(csrf())
                        .param("addressId", firstUsersAddress.getId().toString()).param("paymentMethod", "ONLINE"))
                .andExpect(status().is3xxRedirection());
    }

    private void clearDatabase() {
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
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setPhone("5550100");
        user.setRole(Role.USER);
        return user;
    }

    private Address address(User user) {
        Address address = new Address();
        address.setUser(user);
        address.setAddressLine1("12 Market Street");
        address.setCity("Chennai");
        address.setState("Tamil Nadu");
        address.setPostalCode("600001");
        address.setCountry("India");
        address.setPhone("5550100");
        return address;
    }

    private Product product() {
        Category category = new Category();
        category.setName("Checkout meals");
        category = categoryRepository.save(category);
        Product product = new Product();
        product.setName("Checkout meal");
        product.setDescription("Checkout meal");
        product.setPrice(new BigDecimal("60.00"));
        product.setStockQuantity(5);
        product.setCategory(category);
        product.setActive(true);
        return product;
    }

    private Coupon coupon(String code) {
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDescription("Checkout coupon");
        coupon.setDiscountType(DiscountType.PERCENTAGE);
        coupon.setDiscountValue(new BigDecimal("10.00"));
        coupon.setMinimumOrderAmount(BigDecimal.ZERO);
        coupon.setExpiryDate(LocalDateTime.now().plusDays(1));
        coupon.setUsageLimit(10);
        return coupon;
    }
}
