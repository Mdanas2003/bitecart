package com.bitecart.catalog;

import com.bitecart.dto.CatalogFilter;
import com.bitecart.dto.ProductSort;
import com.bitecart.entity.Category;
import com.bitecart.entity.Product;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.repository.CategoryRepository;
import com.bitecart.repository.ProductRepository;
import com.bitecart.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogIntegrationTests {

    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductService productService;
    @Autowired private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void catalogExcludesInactiveProductsAndInactiveCategories() {
        Category active = category("Active", true);
        Category inactive = category("Hidden", false);
        product("Visible bowl", active, true, false, "120.00", "4.5");
        product("Hidden dish", active, false, false, "80.00", "4.8");
        product("Hidden category dish", inactive, true, false, "90.00", "4.7");

        Page<?> catalog = productService.getCatalog(filter(null, null, null, null, ProductSort.NEWEST, 0, 12));

        assertThat(catalog.getTotalElements()).isEqualTo(1);
    }

    @Test
    void catalogSearchCategoryPriceRatingAndSortingWork() {
        Category bowls = category("Bowls", true);
        Category wraps = category("Wraps", true);
        product("Spicy Bowl", bowls, true, false, "250.00", "4.8");
        product("Simple Bowl", bowls, true, false, "150.00", "3.2");
        product("Spicy Wrap", wraps, true, false, "200.00", "4.6");

        Page<?> filtered = productService.getCatalog(filter("spicy", bowls.getId(), new BigDecimal("200"),
                new BigDecimal("300"), ProductSort.RATING_DESC, 0, 12));
        Page<?> sorted = productService.getCatalog(filter(null, null, null, null, ProductSort.PRICE_ASC, 0, 12));

        assertThat(filtered.getTotalElements()).isEqualTo(1);
        assertThat(sorted.getContent()).extracting(item -> ((com.bitecart.dto.ProductDTO) item).name())
                .containsExactly("Simple Bowl", "Spicy Wrap", "Spicy Bowl");
    }

    @Test
    void catalogPaginatesFeaturedAndRelatedProductsSafely() {
        Category category = category("Street food", true);
        Product current = product("Current", category, true, true, "100.00", "4.0");
        product("Related one", category, true, false, "110.00", "4.1");
        product("Related two", category, true, false, "120.00", "4.2");
        product("Related three", category, true, false, "130.00", "4.3");

        Page<?> page = productService.getCatalog(filter(null, null, null, null, ProductSort.NEWEST, 0, 2));

        assertThat(page.getSize()).isEqualTo(2);
        assertThat(productService.getFeaturedProducts()).extracting("name").contains("Current");
        assertThat(productService.getRelatedProducts(current.getId())).extracting("id").doesNotContain(current.getId());
    }

    @Test
    void missingProductIsNotExposed() {
        assertThatThrownBy(() -> productService.getActiveProductById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void publicCatalogPagesAreAvailableAndInvalidFiltersRedirectSafely() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("home"));
        mockMvc.perform(get("/products")).andExpect(status().isOk()).andExpect(view().name("products"));
        mockMvc.perform(get("/products").param("minPrice", "100").param("maxPrice", "10"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/products/999")).andExpect(status().isNotFound()).andExpect(view().name("catalog-not-found"));
    }

    @Test
    void catalogControllerBindsSearchFilterSortAndPaginationParameters() throws Exception {
        Category category = category("Controller bowls", true);
        product("Spicy controller bowl", category, true, false, "220.00", "4.7");
        product("Mild controller bowl", category, true, false, "180.00", "4.4");

        mockMvc.perform(get("/products").param("search", "spicy").param("categoryId", category.getId().toString())
                        .param("minPrice", "200.00").param("minRating", "4").param("sort", "priceAsc").param("page", "0"))
                .andExpect(status().isOk()).andExpect(view().name("products"))
                .andExpect(model().attributeExists("products", "filter", "categories", "sortOptions"));
    }

    @Test
    void categoryNavigationUsesTheCatalogCategoryIdFilter() throws Exception {
        Category biryani = category("Biryani", true);
        Category burgers = category("Burgers", true);
        product("Ambur Chicken Biryani", biryani, true, false, "289.00", "4.6");
        product("Chennai Chicken Burger", burgers, true, false, "199.00", "4.5");

        mockMvc.perform(get("/categories/{id}", biryani.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl(
                        "/products?categoryId=" + biryani.getId()));

        mockMvc.perform(get("/products").param("categoryId", biryani.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("products", org.hamcrest.Matchers.hasProperty("totalElements", org.hamcrest.Matchers.is(1L))));
    }

    private CatalogFilter filter(String search, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                 ProductSort sort, int page, int size) {
        return new CatalogFilter(search, categoryId, minPrice, maxPrice, null, sort, page, size);
    }

    private Category category(String name, boolean active) {
        Category category = new Category();
        category.setName(name);
        category.setActive(active);
        return categoryRepository.save(category);
    }

    private Product product(String name, Category category, boolean active, boolean featured, String price, String rating) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(name + " description");
        product.setPrice(new BigDecimal(price));
        product.setStockQuantity(10);
        product.setCategory(category);
        product.setRating(new BigDecimal(rating));
        product.setActive(active);
        product.setFeatured(featured);
        return productRepository.save(product);
    }
}
