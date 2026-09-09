package com.bitecart.cart;

import com.bitecart.entity.Category;
import com.bitecart.entity.Product;
import com.bitecart.entity.Role;
import com.bitecart.entity.User;
import com.bitecart.repository.CartRepository;
import com.bitecart.repository.CategoryRepository;
import com.bitecart.repository.ProductRepository;
import com.bitecart.repository.UserRepository;
import com.bitecart.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartWishlistControllerIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private CartRepository cartRepository;
    @Autowired private WishlistRepository wishlistRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;

    private Product activeProduct;

    @BeforeEach
    void setUp() {
        wishlistRepository.deleteAll();
        cartRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        user("one@example.com");
        user("two@example.com");
        Category category = new Category();
        category.setName("Snacks");
        category = categoryRepository.save(category);
        activeProduct = product("Active snack", category, true, 5);
    }

    @Test
    void anonymousUsersAreRedirectedFromCartAndWishlist() throws Exception {
        mockMvc.perform(get("/cart")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/wishlist")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "one@example.com", roles = "USER")
    void cartControllerAddsUpdatesRemovesAndClearsCart() throws Exception {
        mockMvc.perform(post("/cart/items").with(csrf()).param("productId", activeProduct.getId().toString()).param("quantity", "1"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/products/" + activeProduct.getId()));
        Long itemId = cartRepository.findByUserEmailIgnoreCase("one@example.com").orElseThrow().getItems().get(0).getId();
        mockMvc.perform(post("/cart/items/{id}", itemId).with(csrf()).param("quantity", "2"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/cart"));
        mockMvc.perform(post("/cart/items/{id}/remove", itemId).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/cart"));
        mockMvc.perform(post("/cart/clear").with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/cart"));
    }

    @Test
    @WithMockUser(username = "one@example.com", roles = "USER")
    void invalidCartInputReturnsSafeFeedback() throws Exception {
        mockMvc.perform(post("/cart/items").with(csrf()).param("productId", activeProduct.getId().toString()).param("quantity", "6"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("cartError"));
        mockMvc.perform(post("/cart/items").with(csrf()).param("productId", "99999").param("quantity", "1"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("cartError"));
    }

    @Test
    @WithMockUser(username = "one@example.com", roles = "USER")
    void wishlistControllerAddsAndRemovesItem() throws Exception {
        mockMvc.perform(post("/wishlist/items").with(csrf()).param("productId", activeProduct.getId().toString()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/products/" + activeProduct.getId()));
        Long itemId = wishlistRepository.findByUserEmailIgnoreCaseOrderByCreatedAtDesc("one@example.com").get(0).getId();
        mockMvc.perform(post("/wishlist/items/{id}/remove", itemId).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/wishlist"));
        mockMvc.perform(get("/wishlist")).andExpect(status().isOk()).andExpect(view().name("wishlist"));
    }

    @Test
    void crossUserControllerRequestsCannotMutateAnotherUsersItems() throws Exception {
        addCartAndWishlistForFirstUser();
        Long cartItemId = cartRepository.findByUserEmailIgnoreCase("one@example.com").orElseThrow().getItems().get(0).getId();
        Long wishlistItemId = wishlistRepository.findByUserEmailIgnoreCaseOrderByCreatedAtDesc("one@example.com").get(0).getId();

        mockMvc.perform(post("/cart/items/{id}/remove", cartItemId).with(csrf()).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("two@example.com").roles("USER")))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("cartError"));
        mockMvc.perform(post("/wishlist/items/{id}/remove", wishlistItemId).with(csrf()).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("two@example.com").roles("USER")))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("wishlistError"));

        assertThat(cartRepository.findByUserEmailIgnoreCase("one@example.com").orElseThrow().getItems()).hasSize(1);
        assertThat(wishlistRepository.findByUserEmailIgnoreCaseOrderByCreatedAtDesc("one@example.com")).hasSize(1);
    }

    private void addCartAndWishlistForFirstUser() throws Exception {
        mockMvc.perform(post("/cart/items").with(csrf()).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("one@example.com").roles("USER"))
                .param("productId", activeProduct.getId().toString()).param("quantity", "1"));
        mockMvc.perform(post("/wishlist/items").with(csrf()).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("one@example.com").roles("USER"))
                .param("productId", activeProduct.getId().toString()));
    }

    private void user(String email) {
        User user = new User();
        user.setFirstName("Test"); user.setLastName("User"); user.setEmail(email); user.setPassword("encoded-password");
        user.setPhone("5550100"); user.setRole(Role.USER); userRepository.save(user);
    }

    private Product product(String name, Category category, boolean active, int stock) {
        Product product = new Product();
        product.setName(name); product.setDescription(name); product.setPrice(new BigDecimal("60.00"));
        product.setStockQuantity(stock); product.setCategory(category); product.setActive(active);
        return productRepository.save(product);
    }
}
