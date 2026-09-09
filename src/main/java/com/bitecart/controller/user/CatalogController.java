package com.bitecart.controller.user;

import com.bitecart.dto.CatalogFilter;
import com.bitecart.dto.ProductSort;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.service.CategoryService;
import com.bitecart.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
public class CatalogController {

    private static final int CATALOG_PAGE_SIZE = 12;

    private final ProductService productService;
    private final CategoryService categoryService;

    public CatalogController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("categories", categoryService.getActiveCategories());
        model.addAttribute("featuredProducts", productService.getFeaturedProducts());
        return "home";
    }

    @GetMapping("/products")
    public String products(@RequestParam(required = false) String search,
                           @RequestParam(required = false) Long category,
                           @RequestParam(required = false) BigDecimal minPrice,
                           @RequestParam(required = false) BigDecimal maxPrice,
                           @RequestParam(required = false) BigDecimal minRating,
                           @RequestParam(defaultValue = "newest") String sort,
                           @RequestParam(defaultValue = "0") Integer page,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (!validFilters(minPrice, maxPrice, minRating)) {
            redirectAttributes.addFlashAttribute("catalogError", "Use valid, non-negative catalog filters.");
            return "redirect:/products";
        }
        CatalogFilter filter = new CatalogFilter(search, category, minPrice, maxPrice, minRating,
                ProductSort.from(sort), page == null ? 0 : page, CATALOG_PAGE_SIZE);
        Page<?> products = productService.getCatalog(filter);
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getActiveCategories());
        model.addAttribute("filter", filter);
        model.addAttribute("sortOptions", ProductSort.values());
        return "products";
    }

    @GetMapping("/products/{id}")
    public String productDetails(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.getActiveProductById(id));
        model.addAttribute("relatedProducts", productService.getRelatedProducts(id));
        return "product-details";
    }

    @GetMapping("/categories/{id}")
    public String category(@PathVariable Long id) {
        categoryService.getActiveCategoryById(id);
        return "redirect:/products?category=" + id;
    }

    private boolean validFilters(BigDecimal minPrice, BigDecimal maxPrice, BigDecimal minRating) {
        if ((minPrice != null && minPrice.signum() < 0) || (maxPrice != null && maxPrice.signum() < 0)
                || (minRating != null && (minRating.signum() < 0 || minRating.compareTo(BigDecimal.valueOf(5)) > 0))) {
            return false;
        }
        return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
    }
}
