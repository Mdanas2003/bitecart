package com.bitecart.entity;

import com.bitecart.dto.OrderDTO;
import com.bitecart.exception.CouponValidationException;
import com.bitecart.exception.InsufficientStockException;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.repository.AddressRepository;
import com.bitecart.repository.CartRepository;
import com.bitecart.repository.CategoryRepository;
import com.bitecart.repository.CouponRepository;
import com.bitecart.repository.OrderItemRepository;
import com.bitecart.repository.OrderRepository;
import com.bitecart.repository.ProductRepository;
import com.bitecart.repository.UserRepository;
import com.bitecart.repository.WishlistRepository;
import com.bitecart.service.CartService;
import com.bitecart.service.CouponService;
import com.bitecart.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class OrderCouponServiceIntegrationTests {

    @Autowired private OrderService orderService;
    @Autowired private CouponService couponService;
    @Autowired private CartService cartService;
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
    private Product product;
    private Address firstUsersAddress;

    @BeforeEach
    void setUp() {
        clearDatabase();
        firstUser = userRepository.save(user("first@example.com"));
        firstUsersAddress = addressRepository.save(address(firstUser));
        product = productRepository.save(product("Order meal", 5, "120.00", "100.00"));
        authenticate(firstUser.getEmail());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        clearDatabase();
    }

    @Test
    void createsTransactionalOrderFromCartWithCurrentPriceCouponAndStockUpdate() {
        cartService.addProduct(product.getId(), 2);
        couponRepository.save(coupon("SAVE10", DiscountType.PERCENTAGE, "10.00", "0.00", null));

        OrderDTO order = orderService.createOrder(firstUsersAddress.getId(), PaymentMethod.CASH_ON_DELIVERY, "SAVE10");

        assertThat(order.id()).isNotNull();
        assertThat(order.subtotal()).isEqualByComparingTo("200.00");
        assertThat(order.discountAmount()).isEqualByComparingTo("20.00");
        assertThat(order.deliveryFee()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.totalAmount()).isEqualByComparingTo("180.00");
        assertThat(order.couponCode()).isEqualTo("SAVE10");
        assertThat(order.items()).singleElement().satisfies(item -> {
            assertThat(item.productName()).isEqualTo("Order meal");
            assertThat(item.price()).isEqualByComparingTo("100.00");
            assertThat(item.subtotal()).isEqualByComparingTo("200.00");
        });
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(3);
        assertThat(couponRepository.findByCodeIgnoreCase("SAVE10").orElseThrow().getUsedCount()).isEqualTo(1);
        assertThat(cartService.getCurrentUsersCart().items()).isEmpty();
    }

    @Test
    void validatesCouponStateMinimumAndMaximumDiscount() {
        Coupon cappedCoupon = couponRepository.save(coupon("CAP50", DiscountType.PERCENTAGE, "50.00", "100.00", "25.00"));
        assertThat(couponService.calculateDiscount(cappedCoupon.getCode(), new BigDecimal("200.00")))
                .isEqualByComparingTo("25.00");

        Coupon inactiveCoupon = coupon("INACTIVE", DiscountType.FIXED, "10.00", "0.00", null);
        inactiveCoupon.setActive(false);
        couponRepository.save(inactiveCoupon);
        assertThatThrownBy(() -> couponService.calculateDiscount("INACTIVE", new BigDecimal("100.00")))
                .isInstanceOf(CouponValidationException.class);

        Coupon expiredCoupon = coupon("EXPIRED", DiscountType.FIXED, "10.00", "0.00", null);
        expiredCoupon.setExpiryDate(LocalDateTime.now().minusMinutes(1));
        couponRepository.save(expiredCoupon);
        assertThatThrownBy(() -> couponService.calculateDiscount("EXPIRED", new BigDecimal("100.00")))
                .isInstanceOf(CouponValidationException.class);

        Coupon exhaustedCoupon = coupon("USEDUP", DiscountType.FIXED, "10.00", "0.00", null);
        exhaustedCoupon.setUsageLimit(1);
        exhaustedCoupon.setUsedCount(1);
        couponRepository.save(exhaustedCoupon);
        assertThatThrownBy(() -> couponService.calculateDiscount("USEDUP", new BigDecimal("100.00")))
                .isInstanceOf(CouponValidationException.class);

        Coupon minimumCoupon = coupon("MINIMUM", DiscountType.FIXED, "10.00", "200.00", null);
        couponRepository.save(minimumCoupon);
        assertThatThrownBy(() -> couponService.calculateDiscount("MINIMUM", new BigDecimal("199.00")))
                .isInstanceOf(CouponValidationException.class);
    }

    @Test
    void rejectsInsufficientStockAtOrderCreation() {
        cartService.addProduct(product.getId(), 2);
        product.setStockQuantity(1);
        productRepository.saveAndFlush(product);

        assertThatThrownBy(() -> orderService.createOrder(firstUsersAddress.getId(), PaymentMethod.ONLINE, null))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(orderRepository.count()).isZero();
        assertThat(cartService.getCurrentUsersCart().items()).hasSize(1);
    }

    @Test
    void preventsCrossUserAddressAndOrderAccess() {
        User secondUser = userRepository.save(user("second@example.com"));
        Address secondUsersAddress = addressRepository.save(address(secondUser));
        cartService.addProduct(product.getId(), 1);

        assertThatThrownBy(() -> orderService.createOrder(secondUsersAddress.getId(), PaymentMethod.ONLINE, null))
                .isInstanceOf(ResourceNotFoundException.class);

        OrderDTO created = orderService.createOrder(firstUsersAddress.getId(), PaymentMethod.ONLINE, null);
        authenticate(secondUser.getEmail());
        assertThatThrownBy(() -> orderService.getCurrentUsersOrder(created.id()))
                .isInstanceOf(ResourceNotFoundException.class);
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

    private User user(String email) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("Customer");
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

    private Product product(String name, int stock, String price, String discountPrice) {
        Category category = new Category();
        category.setName("Order service meals");
        category = categoryRepository.save(category);

        Product product = new Product();
        product.setName(name);
        product.setDescription(name);
        product.setPrice(new BigDecimal(price));
        product.setDiscountPrice(new BigDecimal(discountPrice));
        product.setStockQuantity(stock);
        product.setCategory(category);
        product.setActive(true);
        return product;
    }

    private Coupon coupon(String code, DiscountType type, String value, String minimum, String maximum) {
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDescription("Order service test coupon");
        coupon.setDiscountType(type);
        coupon.setDiscountValue(new BigDecimal(value));
        coupon.setMinimumOrderAmount(new BigDecimal(minimum));
        coupon.setMaximumDiscount(maximum == null ? null : new BigDecimal(maximum));
        coupon.setExpiryDate(LocalDateTime.now().plusDays(1));
        coupon.setUsageLimit(10);
        return coupon;
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
