package com.bitecart.service.impl;

import com.bitecart.dto.*;
import com.bitecart.entity.*;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.mapper.CatalogMapper;
import com.bitecart.repository.*;
import com.bitecart.service.AdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {
    private final UserRepository userRepository; private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository; private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository; private final CouponRepository couponRepository;
    private final CatalogMapper catalogMapper;

    public AdminServiceImpl(UserRepository users, ProductRepository products, CategoryRepository categories,
                            OrderRepository orders, ReviewRepository reviews, CouponRepository coupons, CatalogMapper mapper) {
        userRepository = users; productRepository = products; categoryRepository = categories; orderRepository = orders;
        reviewRepository = reviews; couponRepository = coupons; catalogMapper = mapper;
    }
    @Override public AdminDashboardDTO dashboard() { return new AdminDashboardDTO(userRepository.count(), productRepository.count(), categoryRepository.count(), orderRepository.count(), reviewRepository.count(), couponRepository.count()); }
    @Override public List<ProductDTO> products() { return productRepository.findAll().stream().map(catalogMapper::toProductDto).toList(); }
    @Override public ProductDTO product(Long id) { return catalogMapper.toProductDto(findProduct(id)); }
    @Override @Transactional public ProductDTO saveProduct(Long id, AdminProductForm form) {
        if (form.getDiscountPrice() != null && form.getDiscountPrice().compareTo(form.getPrice()) >= 0) throw new IllegalArgumentException("Discount price must be lower than the regular price.");
        Product value = id == null ? new Product() : findProduct(id); Category category = findCategory(form.getCategoryId());
        value.setName(form.getName().trim()); value.setDescription(form.getDescription()); value.setPrice(form.getPrice()); value.setDiscountPrice(form.getDiscountPrice()); value.setImageUrl(form.getImageUrl()); value.setStockQuantity(form.getStockQuantity()); value.setCategory(category); value.setFeatured(form.isFeatured()); value.setActive(form.isActive());
        return catalogMapper.toProductDto(productRepository.save(value));
    }
    @Override @Transactional public void deleteProduct(Long id) { productRepository.delete(findProduct(id)); }
    @Override public List<CategoryDTO> categories() { return categoryRepository.findAll().stream().map(catalogMapper::toCategoryDto).toList(); }
    @Override public CategoryDTO category(Long id) { return catalogMapper.toCategoryDto(findCategory(id)); }
    @Override @Transactional public CategoryDTO saveCategory(Long id, AdminCategoryForm form) {
        Category value = id == null ? new Category() : findCategory(id); value.setName(form.getName().trim()); value.setDescription(form.getDescription()); value.setImageUrl(form.getImageUrl()); value.setActive(form.isActive()); return catalogMapper.toCategoryDto(categoryRepository.save(value));
    }
    @Override @Transactional public void deleteCategory(Long id) { categoryRepository.delete(findCategory(id)); }
    @Override public List<OrderDTO> orders() { return orderRepository.findAll().stream().map(this::toOrderDto).toList(); }
    @Override public OrderDTO order(Long id) { return toOrderDto(findOrder(id)); }
    @Override @Transactional public void updateOrderStatus(Long id, OrderStatus status) { if (status == null) throw new IllegalArgumentException("Select an order status."); findOrder(id).setOrderStatus(status); }
    @Override public List<UserSummaryDTO> users() { return userRepository.findAll().stream().map(this::toUserDto).toList(); }
    @Override @Transactional public void updateUser(Long id, AdminUserForm form) { User user = findUser(id); user.setRole(form.getRole()); user.setEnabled(form.isEnabled()); }
    @Override public List<ReviewDTO> reviews() { return reviewRepository.findAll().stream().map(this::toReviewDto).toList(); }
    @Override @Transactional public void deleteReview(Long id) { Review review = findReview(id); Long productId = review.getProduct().getId(); reviewRepository.delete(review); Double average = reviewRepository.calculateAverageRatingByProductId(productId); findProduct(productId).setRating(average == null ? BigDecimal.ZERO : BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP)); }
    @Override public List<CouponDTO> coupons() { return couponRepository.findAll().stream().map(this::toCouponDto).toList(); }
    @Override public CouponDTO coupon(Long id) { return toCouponDto(findCoupon(id)); }
    @Override @Transactional public CouponDTO saveCoupon(Long id, AdminCouponForm form) {
        if (form == null || form.getDiscountType() == null || form.getDiscountValue() == null
                || form.getMinimumOrderAmount() == null || form.getExpiryDate() == null) {
            throw new IllegalArgumentException("Check the coupon fields and expiry date.");
        }
        if (form.getDiscountType() == DiscountType.PERCENTAGE
                && form.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentage discounts cannot exceed 100%.");
        }
        Coupon value = id == null ? new Coupon() : findCoupon(id); couponRepository.findByCodeIgnoreCase(form.getCode().trim()).filter(found -> !found.getId().equals(id)).ifPresent(found -> { throw new IllegalArgumentException("A coupon with this code already exists."); });
        value.setCode(form.getCode().trim()); value.setDescription(form.getDescription()); value.setDiscountType(form.getDiscountType()); value.setDiscountValue(form.getDiscountValue()); value.setMinimumOrderAmount(form.getMinimumOrderAmount()); value.setMaximumDiscount(form.getMaximumDiscount()); value.setExpiryDate(form.getExpiryDate()); value.setUsageLimit(form.getUsageLimit()); value.setActive(form.isActive()); return toCouponDto(couponRepository.save(value));
    }
    @Override @Transactional public void deleteCoupon(Long id) { couponRepository.delete(findCoupon(id)); }
    private Product findProduct(Long id) { return productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id)); }
    private Category findCategory(Long id) { return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id)); }
    private Order findOrder(Long id) { return orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order", id)); }
    private User findUser(Long id) { return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id)); }
    private Review findReview(Long id) { return reviewRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Review", id)); }
    private Coupon findCoupon(Long id) { return couponRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Coupon", id)); }
    private UserSummaryDTO toUserDto(User user) { return new UserSummaryDTO(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getPhone(), user.getRole(), user.isEnabled(), user.getCreatedAt()); }
    private ReviewDTO toReviewDto(Review review) { return new ReviewDTO(review.getId(), review.getProduct().getId(), review.getOrder().getId(), review.getUser().getFirstName(), review.getRating(), review.getComment(), review.getCreatedAt()); }
    private CouponDTO toCouponDto(Coupon coupon) { return new CouponDTO(coupon.getId(), coupon.getCode(), coupon.getDescription(), coupon.getDiscountType(), coupon.getDiscountValue(), coupon.getMinimumOrderAmount(), coupon.getMaximumDiscount(), coupon.getExpiryDate(), coupon.getUsageLimit(), coupon.getUsedCount(), coupon.isActive()); }
    private OrderDTO toOrderDto(Order order) { List<OrderItemDTO> items = order.getItems().stream().map(item -> new OrderItemDTO(item.getId(), item.getProduct().getId(), item.getProductName(), item.getQuantity(), item.getPrice(), item.getSubtotal())).toList(); return new OrderDTO(order.getId(), order.getAddress().getId(), order.getSubtotal(), order.getDiscountAmount(), order.getDeliveryFee(), order.getTotalAmount(), order.getPaymentMethod(), order.getPaymentStatus(), order.getOrderStatus(), order.getCouponCode(), order.getCreatedAt(), items); }
}
