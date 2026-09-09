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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class OrderCouponPersistenceIntegrationTests {

    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private CouponRepository couponRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private WishlistRepository wishlistRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    @AfterEach
    void clearDatabase() {
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

    @Test
    void persistsOrderWithSnapshotItemAndEnumStates() {
        User user = userRepository.save(user());
        Address address = addressRepository.save(address(user));
        Product product = productRepository.save(product());

        Order order = new Order();
        order.setUser(user);
        order.setAddress(address);
        order.setSubtotal(new BigDecimal("240.00"));
        order.setDiscountAmount(new BigDecimal("20.00"));
        order.setDeliveryFee(new BigDecimal("30.00"));
        order.setTotalAmount(new BigDecimal("250.00"));
        order.setCouponCode("WELCOME10");
        order.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setProductName("Original Veggie Burger");
        item.setQuantity(2);
        item.setPrice(new BigDecimal("120.00"));
        item.setSubtotal(new BigDecimal("240.00"));
        order.addItem(item);

        Order saved = orderRepository.saveAndFlush(order);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getOrderStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(orderRepository.findByIdAndUserEmailIgnoreCase(saved.getId(), user.getEmail())).isPresent();

        OrderItem persistedItem = orderItemRepository.findAll().get(0);
        assertThat(persistedItem.getProductName()).isEqualTo("Original Veggie Burger");
        assertThat(persistedItem.getPrice()).isEqualByComparingTo("120.00");
        assertThat(persistedItem.getSubtotal()).isEqualByComparingTo("240.00");
        assertThat(persistedItem.getQuantity()).isEqualTo(2);
    }

    @Test
    void persistsCouponAndEnforcesUniqueCode() {
        Coupon saved = couponRepository.saveAndFlush(coupon("WELCOME10"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getDiscountType()).isEqualTo(DiscountType.PERCENTAGE);
        assertThat(couponRepository.findByCodeIgnoreCase("welcome10")).isPresent();

        assertThatThrownBy(() -> couponRepository.saveAndFlush(coupon("WELCOME10")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User user() {
        User user = new User();
        user.setFirstName("Order");
        user.setLastName("Customer");
        user.setEmail("order.customer@example.com");
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
        address.setDefaultAddress(true);
        return address;
    }

    private Product product() {
        Category category = new Category();
        category.setName("Order Test Meals");
        category = categoryRepository.save(category);

        Product product = new Product();
        product.setName("Veggie Burger");
        product.setDescription("Freshly prepared burger");
        product.setPrice(new BigDecimal("120.00"));
        product.setStockQuantity(10);
        product.setCategory(category);
        product.setActive(true);
        return product;
    }

    private Coupon coupon(String code) {
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDescription("New customer discount");
        coupon.setDiscountType(DiscountType.PERCENTAGE);
        coupon.setDiscountValue(new BigDecimal("10.00"));
        coupon.setMinimumOrderAmount(new BigDecimal("200.00"));
        coupon.setMaximumDiscount(new BigDecimal("100.00"));
        coupon.setExpiryDate(LocalDateTime.now().plusDays(30));
        coupon.setUsageLimit(100);
        return coupon;
    }
}
