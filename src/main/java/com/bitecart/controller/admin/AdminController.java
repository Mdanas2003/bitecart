package com.bitecart.controller.admin;

import com.bitecart.dto.*;
import com.bitecart.entity.DiscountType;
import com.bitecart.entity.OrderStatus;
import com.bitecart.entity.Role;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;
    public AdminController(AdminService adminService) { this.adminService = adminService; }

    @GetMapping public String dashboard(Model model) { model.addAttribute("summary", adminService.dashboard()); return "admin/dashboard"; }

    @GetMapping("/products") public String products(Model model) { model.addAttribute("products", adminService.products()); return "admin/products"; }
    @GetMapping("/products/new") public String newProduct(Model model) { productForm(model, new AdminProductForm(), "Create product"); return "admin/product-form"; }
    @GetMapping("/products/{id}/edit") public String editProduct(@PathVariable Long id, Model model) { ProductDTO product = adminService.product(id); AdminProductForm form = new AdminProductForm(); form.setName(product.name()); form.setDescription(product.description()); form.setPrice(product.price()); form.setDiscountPrice(product.discountPrice()); form.setImageUrl(product.imageUrl()); form.setStockQuantity(product.stockQuantity()); form.setCategoryId(product.categoryId()); form.setFeatured(product.featured()); form.setActive(product.active()); productForm(model, form, "Edit product"); model.addAttribute("productId", id); return "admin/product-form"; }
    @PostMapping("/products") public String saveProduct(@RequestParam(required = false) Long id, @Valid @ModelAttribute("form") AdminProductForm form, BindingResult result, RedirectAttributes flash) { if (result.hasErrors()) return error(flash, "/admin/products", "Check the product fields."); try { adminService.saveProduct(id, form); return success(flash, "/admin/products", "Product saved."); } catch (IllegalArgumentException exception) { return error(flash, "/admin/products", exception.getMessage()); } }
    @PostMapping("/products/{id}/delete") public String deleteProduct(@PathVariable Long id, RedirectAttributes flash) { try { adminService.deleteProduct(id); return success(flash, "/admin/products", "Product removed."); } catch (RuntimeException exception) { return error(flash, "/admin/products", "The product could not be removed while it is referenced by existing data."); } }

    @GetMapping("/categories") public String categories(Model model) { model.addAttribute("categories", adminService.categories()); return "admin/categories"; }
    @GetMapping("/categories/new") public String newCategory(Model model) { model.addAttribute("form", new AdminCategoryForm()); model.addAttribute("title", "Create category"); return "admin/category-form"; }
    @GetMapping("/categories/{id}/edit") public String editCategory(@PathVariable Long id, Model model) { CategoryDTO category = adminService.category(id); AdminCategoryForm form = new AdminCategoryForm(); form.setName(category.name()); form.setDescription(category.description()); form.setImageUrl(category.imageUrl()); form.setActive(category.active()); model.addAttribute("form", form); model.addAttribute("categoryId", id); model.addAttribute("title", "Edit category"); return "admin/category-form"; }
    @PostMapping("/categories") public String saveCategory(@RequestParam(required = false) Long id, @Valid @ModelAttribute("form") AdminCategoryForm form, BindingResult result, RedirectAttributes flash) { if (result.hasErrors()) return error(flash, "/admin/categories", "Check the category fields."); try { adminService.saveCategory(id, form); return success(flash, "/admin/categories", "Category saved."); } catch (RuntimeException exception) { return error(flash, "/admin/categories", "A category with that name may already exist."); } }
    @PostMapping("/categories/{id}/delete") public String deleteCategory(@PathVariable Long id, RedirectAttributes flash) { try { adminService.deleteCategory(id); return success(flash, "/admin/categories", "Category removed."); } catch (RuntimeException exception) { return error(flash, "/admin/categories", "The category cannot be removed while products use it."); } }

    @GetMapping("/users") public String users(Model model) { model.addAttribute("users", adminService.users()); model.addAttribute("roles", Role.values()); return "admin/users"; }
    @PostMapping("/users/{id}") public String updateUser(@PathVariable Long id, @Valid @ModelAttribute AdminUserForm form, BindingResult result, RedirectAttributes flash) { if (result.hasErrors()) return error(flash, "/admin/users", "Select a valid user role."); adminService.updateUser(id, form); return success(flash, "/admin/users", "User access updated."); }

    @GetMapping("/orders") public String orders(Model model) { model.addAttribute("orders", adminService.orders()); return "admin/orders"; }
    @GetMapping("/orders/{id}") public String order(@PathVariable Long id, Model model) { try { model.addAttribute("order", adminService.order(id)); model.addAttribute("statuses", OrderStatus.values()); return "admin/order-details"; } catch (ResourceNotFoundException exception) { return "admin/not-found"; } }
    @PostMapping("/orders/{id}/status") public String updateOrderStatus(@PathVariable Long id, @RequestParam OrderStatus status, RedirectAttributes flash) { adminService.updateOrderStatus(id, status); return success(flash, "/admin/orders/" + id, "Order status updated."); }

    @GetMapping("/reviews") public String reviews(Model model) { model.addAttribute("reviews", adminService.reviews()); return "admin/reviews"; }
    @PostMapping("/reviews/{id}/delete") public String deleteReview(@PathVariable Long id, RedirectAttributes flash) { adminService.deleteReview(id); return success(flash, "/admin/reviews", "Review removed and product rating refreshed."); }

    @GetMapping("/coupons") public String coupons(Model model) { model.addAttribute("coupons", adminService.coupons()); return "admin/coupons"; }
    @GetMapping("/coupons/new") public String newCoupon(Model model) { couponForm(model, new AdminCouponForm(), "Create coupon"); return "admin/coupon-form"; }
    @GetMapping("/coupons/{id}/edit") public String editCoupon(@PathVariable Long id, Model model) { CouponDTO coupon = adminService.coupon(id); AdminCouponForm form = new AdminCouponForm(); form.setCode(coupon.code()); form.setDescription(coupon.description()); form.setDiscountType(coupon.discountType()); form.setDiscountValue(coupon.discountValue()); form.setMinimumOrderAmount(coupon.minimumOrderAmount()); form.setMaximumDiscount(coupon.maximumDiscount()); form.setExpiryDate(coupon.expiryDate()); form.setUsageLimit(coupon.usageLimit()); form.setActive(coupon.active()); couponForm(model, form, "Edit coupon"); model.addAttribute("couponId", id); return "admin/coupon-form"; }
    @PostMapping("/coupons") public String saveCoupon(@RequestParam(required = false) Long id, @Valid @ModelAttribute("form") AdminCouponForm form, BindingResult result, RedirectAttributes flash) { if (result.hasErrors()) return error(flash, "/admin/coupons", "Check the coupon fields and expiry date."); try { adminService.saveCoupon(id, form); return success(flash, "/admin/coupons", "Coupon saved."); } catch (IllegalArgumentException exception) { return error(flash, "/admin/coupons", exception.getMessage()); } }
    @PostMapping("/coupons/{id}/delete") public String deleteCoupon(@PathVariable Long id, RedirectAttributes flash) { adminService.deleteCoupon(id); return success(flash, "/admin/coupons", "Coupon removed."); }

    private void productForm(Model model, AdminProductForm form, String title) { model.addAttribute("form", form); model.addAttribute("categories", adminService.categories()); model.addAttribute("title", title); }
    private void couponForm(Model model, AdminCouponForm form, String title) { model.addAttribute("form", form); model.addAttribute("types", DiscountType.values()); model.addAttribute("title", title); }
    private String success(RedirectAttributes flash, String path, String message) { flash.addFlashAttribute("adminSuccess", message); return "redirect:" + path; }
    private String error(RedirectAttributes flash, String path, String message) { flash.addFlashAttribute("adminError", message); return "redirect:" + path; }
}
