package com.bitecart.cart;

import com.bitecart.dto.CartDTO;
import com.bitecart.entity.Cart;
import com.bitecart.entity.Category;
import com.bitecart.entity.Product;
import com.bitecart.entity.Role;
import com.bitecart.entity.User;
import com.bitecart.exception.InsufficientStockException;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.repository.CartRepository;
import com.bitecart.repository.CategoryRepository;
import com.bitecart.repository.ProductRepository;
import com.bitecart.repository.UserRepository;
import com.bitecart.service.CartService;
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
class CartServiceIntegrationTests {

    @Autowired private CartService cartService;
    @Autowired private CartRepository cartRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;

    private User firstUser;
    private Product availableProduct;

    @BeforeEach
    void setUp() {
        cartRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        firstUser = user("one@example.com");
        Category category = new Category();
        category.setName("Meals");
        category = categoryRepository.save(category);
        availableProduct = product("Available meal", category, true, 5, "120.00", "99.00");
        authenticate(firstUser.getEmail());
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsAndRetrievesCartForAuthenticatedUser() {
        CartDTO cart = cartService.getCurrentUsersCart();

        assertThat(cart.id()).isNotNull();
        assertThat(cart.items()).isEmpty();
        assertThat(cart.subtotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cartRepository.findByUserEmailIgnoreCase(firstUser.getEmail())).isPresent();
    }

    @Test
    void addsItemUsesCurrentDiscountedPriceAndCalculatesSubtotal() {
        CartDTO cart = cartService.addProduct(availableProduct.getId(), 2);

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().get(0).unitPrice()).isEqualByComparingTo("99.00");
        assertThat(cart.subtotal()).isEqualByComparingTo("198.00");
        assertThat(cart.totalItems()).isEqualTo(2);
    }

    @Test
    void updatesRemovesAndClearsItems() {
        CartDTO added = cartService.addProduct(availableProduct.getId(), 1);
        Long itemId = added.items().get(0).id();

        CartDTO updated = cartService.updateItemQuantity(itemId, 3);
        assertThat(updated.items().get(0).quantity()).isEqualTo(3);
        CartDTO removed = cartService.removeItem(itemId);
        assertThat(removed.items()).isEmpty();

        cartService.addProduct(availableProduct.getId(), 1);
        assertThat(cartService.clearCart().items()).isEmpty();
    }

    @Test
    void rejectsInactiveProductsAndInsufficientStock() {
        Product inactive = product("Inactive meal", availableProduct.getCategory(), false, 5, "20.00", null);

        assertThatThrownBy(() -> cartService.addProduct(inactive.getId(), 1)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> cartService.addProduct(availableProduct.getId(), 6)).isInstanceOf(InsufficientStockException.class);
        assertThatThrownBy(() -> cartService.addProduct(availableProduct.getId(), 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cannotAccessAnotherUsersCartItem() {
        CartDTO firstCart = cartService.addProduct(availableProduct.getId(), 1);
        Long firstItemId = firstCart.items().get(0).id();
        User secondUser = user("two@example.com");
        authenticate(secondUser.getEmail());

        assertThatThrownBy(() -> cartService.updateItemQuantity(firstItemId, 2)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> cartService.removeItem(firstItemId)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(cartService.getCurrentUsersCart().items()).isEmpty();
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

    private Product product(String name, Category category, boolean active, int stock, String price, String discountPrice) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(name);
        product.setPrice(new BigDecimal(price));
        product.setDiscountPrice(discountPrice == null ? null : new BigDecimal(discountPrice));
        product.setStockQuantity(stock);
        product.setCategory(category);
        product.setActive(active);
        return productRepository.save(product);
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
