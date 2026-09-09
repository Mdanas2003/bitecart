package com.bitecart.service;

import com.bitecart.dto.*;
import com.bitecart.entity.OrderStatus;
import java.util.List;

public interface AdminService {
    AdminDashboardDTO dashboard();
    List<ProductDTO> products(); ProductDTO product(Long id); ProductDTO saveProduct(Long id, AdminProductForm form); void deleteProduct(Long id);
    List<CategoryDTO> categories(); CategoryDTO category(Long id); CategoryDTO saveCategory(Long id, AdminCategoryForm form); void deleteCategory(Long id);
    List<OrderDTO> orders(); OrderDTO order(Long id); void updateOrderStatus(Long id, OrderStatus status);
    List<UserSummaryDTO> users(); void updateUser(Long id, AdminUserForm form);
    List<ReviewDTO> reviews(); void deleteReview(Long id);
    List<CouponDTO> coupons(); CouponDTO coupon(Long id); CouponDTO saveCoupon(Long id, AdminCouponForm form); void deleteCoupon(Long id);
}
