package com.bitecart.service.impl;

import com.bitecart.dto.WishlistDTO;
import com.bitecart.dto.WishlistItemDTO;
import com.bitecart.entity.Product;
import com.bitecart.entity.User;
import com.bitecart.entity.Wishlist;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.repository.ProductRepository;
import com.bitecart.repository.UserRepository;
import com.bitecart.repository.WishlistRepository;
import com.bitecart.service.WishlistService;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public WishlistServiceImpl(WishlistRepository wishlistRepository, ProductRepository productRepository,
                               UserRepository userRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    public WishlistDTO getCurrentUsersWishlist() {
        return toDto(wishlistRepository.findByUserEmailIgnoreCaseOrderByCreatedAtDesc(currentAuthenticatedEmail()));
    }

    @Override
    @Transactional
    public WishlistDTO addProduct(Long productId) {
        String email = currentAuthenticatedEmail();
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        if (wishlistRepository.existsByUserEmailIgnoreCaseAndProductId(email, productId)) {
            return getCurrentUsersWishlist();
        }
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Authenticated user was not found."));
        Wishlist item = new Wishlist();
        item.setUser(user);
        item.setProduct(product);
        wishlistRepository.save(item);
        return getCurrentUsersWishlist();
    }

    @Override
    @Transactional
    public WishlistDTO removeItem(Long wishlistItemId) {
        String email = currentAuthenticatedEmail();
        Wishlist item = wishlistRepository.findByIdAndUserEmailIgnoreCase(wishlistItemId, email)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item", wishlistItemId));
        wishlistRepository.delete(item);
        return getCurrentUsersWishlist();
    }

    @Override
    public boolean isProductWishlisted(Long productId) {
        return wishlistRepository.existsByUserEmailIgnoreCaseAndProductId(currentAuthenticatedEmail(), productId);
    }

    private String currentAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required to access a wishlist.");
        }
        return authentication.getName();
    }

    private WishlistDTO toDto(List<Wishlist> wishlistItems) {
        List<WishlistItemDTO> items = wishlistItems.stream().map(item -> {
            Product product = item.getProduct();
            return new WishlistItemDTO(item.getId(), product.getId(), product.getName(), product.getImageUrl(),
                    product.getPrice(), product.getDiscountPrice(), product.getCategory().getName(), product.isActive());
        }).toList();
        return new WishlistDTO(items, items.size());
    }
}
