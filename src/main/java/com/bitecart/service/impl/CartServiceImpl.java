package com.bitecart.service.impl;

import com.bitecart.dto.CartDTO;
import com.bitecart.dto.CartItemDTO;
import com.bitecart.entity.Cart;
import com.bitecart.entity.CartItem;
import com.bitecart.entity.Product;
import com.bitecart.entity.User;
import com.bitecart.exception.InsufficientStockException;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.repository.CartItemRepository;
import com.bitecart.repository.CartRepository;
import com.bitecart.repository.ProductRepository;
import com.bitecart.repository.UserRepository;
import com.bitecart.service.CartService;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartServiceImpl(CartRepository cartRepository, CartItemRepository cartItemRepository,
                           ProductRepository productRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public CartDTO getCurrentUsersCart() {
        return toDto(getOrCreateCart());
    }

    @Override
    @Transactional
    public CartDTO addProduct(Long productId, int quantity) {
        validateQuantity(quantity);
        Product product = getActiveProduct(productId);
        Cart cart = getOrCreateCart();
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId).orElse(null);
        int requestedQuantity = quantity + (item == null ? 0 : item.getQuantity());
        validateStock(product, requestedQuantity);

        if (item == null) {
            item = new CartItem();
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setPrice(currentPrice(product));
            cart.addItem(item);
        } else {
            item.setQuantity(requestedQuantity);
            item.setPrice(currentPrice(product));
        }
        Cart savedCart = cartRepository.save(cart);
        return toDto(savedCart);
    }

    @Override
    @Transactional
    public CartDTO updateItemQuantity(Long cartItemId, int quantity) {
        validateQuantity(quantity);
        CartItem item = getOwnedCartItem(cartItemId);
        Product product = getActiveProduct(item.getProduct().getId());
        validateStock(product, quantity);
        item.setQuantity(quantity);
        item.setPrice(currentPrice(product));
        return toDto(item.getCart());
    }

    @Override
    @Transactional
    public CartDTO removeItem(Long cartItemId) {
        CartItem item = getOwnedCartItem(cartItemId);
        Cart cart = item.getCart();
        cart.removeItem(item);
        return toDto(cart);
    }

    @Override
    @Transactional
    public CartDTO clearCart() {
        Cart cart = getOrCreateCart();
        cart.getItems().clear();
        return toDto(cart);
    }

    private Cart getOrCreateCart() {
        String email = currentAuthenticatedEmail();
        return cartRepository.findByUserEmailIgnoreCase(email).orElseGet(() -> {
            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Authenticated user was not found."));
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }

    private CartItem getOwnedCartItem(Long cartItemId) {
        return cartItemRepository.findByIdAndCartUserEmailIgnoreCase(cartItemId, currentAuthenticatedEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", cartItemId));
    }

    private Product getActiveProduct(Long productId) {
        return productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
    }

    private String currentAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required to access a cart.");
        }
        return authentication.getName();
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
    }

    private void validateStock(Product product, int quantity) {
        if (product.getStockQuantity() == null || quantity > product.getStockQuantity()) {
            throw new InsufficientStockException();
        }
    }

    private BigDecimal currentPrice(Product product) {
        return product.getDiscountPrice() != null && product.getDiscountPrice().compareTo(product.getPrice()) < 0
                ? product.getDiscountPrice() : product.getPrice();
    }

    private CartDTO toDto(Cart cart) {
        List<CartItemDTO> items = cart.getItems().stream().map(item -> new CartItemDTO(
                item.getId(), item.getProduct().getId(), item.getProduct().getName(), item.getProduct().getImageUrl(),
                item.getQuantity(), item.getPrice(), item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))).toList();
        BigDecimal subtotal = items.stream().map(CartItemDTO::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalItems = items.stream().mapToInt(CartItemDTO::quantity).sum();
        return new CartDTO(cart.getId(), items, totalItems, subtotal);
    }
}
