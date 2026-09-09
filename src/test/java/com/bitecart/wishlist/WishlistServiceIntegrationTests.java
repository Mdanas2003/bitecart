package com.bitecart.wishlist;

import com.bitecart.dto.WishlistDTO;
import com.bitecart.entity.Category;
import com.bitecart.entity.Product;
import com.bitecart.entity.Role;
import com.bitecart.entity.User;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.repository.CategoryRepository;
import com.bitecart.repository.CartRepository;
import com.bitecart.repository.ProductRepository;
import com.bitecart.repository.UserRepository;
import com.bitecart.repository.WishlistRepository;
import com.bitecart.service.WishlistService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class WishlistServiceIntegrationTests {

    @Autowired private WishlistService wishlistService;
    @Autowired private WishlistRepository wishlistRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;

    private User firstUser;
    private Product activeProduct;

    @BeforeEach
    void setUp() {
        wishlistRepository.deleteAll();
        cartRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        firstUser = user("one@example.com");
        Category category = new Category();
        category.setName("Desserts");
        category = categoryRepository.save(category);
        activeProduct = product("Active dessert", category, true);
        authenticate(firstUser.getEmail());
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addsAndRetrievesCurrentUsersWishlist() {
        WishlistDTO wishlist = wishlistService.addProduct(activeProduct.getId());

        assertThat(wishlist.totalItems()).isEqualTo(1);
        assertThat(wishlist.items().get(0).productId()).isEqualTo(activeProduct.getId());
        assertThat(wishlistService.isProductWishlisted(activeProduct.getId())).isTrue();
    }

    @Test
    void duplicateAddsRemainSingleWishlistEntry() {
        wishlistService.addProduct(activeProduct.getId());
        WishlistDTO wishlist = wishlistService.addProduct(activeProduct.getId());

        assertThat(wishlist.totalItems()).isEqualTo(1);
        assertThat(wishlistRepository.findByUserEmailIgnoreCaseOrderByCreatedAtDesc(firstUser.getEmail())).hasSize(1);
    }

    @Test
    void removesOwnedWishlistItem() {
        Long itemId = wishlistService.addProduct(activeProduct.getId()).items().get(0).id();

        WishlistDTO wishlist = wishlistService.removeItem(itemId);

        assertThat(wishlist.items()).isEmpty();
        assertThat(wishlistService.isProductWishlisted(activeProduct.getId())).isFalse();
    }

    @Test
    void rejectsInactiveProduct() {
        Product inactiveProduct = product("Inactive dessert", activeProduct.getCategory(), false);

        assertThatThrownBy(() -> wishlistService.addProduct(inactiveProduct.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cannotRemoveOrInspectAnotherUsersWishlistItem() {
        Long firstItemId = wishlistService.addProduct(activeProduct.getId()).items().get(0).id();
        User secondUser = user("two@example.com");
        authenticate(secondUser.getEmail());

        assertThatThrownBy(() -> wishlistService.removeItem(firstItemId)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(wishlistService.getCurrentUsersWishlist().items()).isEmpty();
        assertThat(wishlistService.isProductWishlisted(activeProduct.getId())).isFalse();
    }

    private User user(String email) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setPhone("5550100");
        user.setRole(Role.USER);
        return userRepository.save(user);
    }

    private Product product(String name, Category category, boolean active) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(name);
        product.setPrice(new BigDecimal("55.00"));
        product.setStockQuantity(5);
        product.setCategory(category);
        product.setActive(active);
        return productRepository.save(product);
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
