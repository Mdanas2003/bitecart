package com.bitecart.service.impl;

import com.bitecart.dto.CartDTO;
import com.bitecart.dto.CartItemDTO;
import com.bitecart.dto.CatalogFilter;
import com.bitecart.dto.ProductDTO;
import com.bitecart.dto.ProductSort;
import com.bitecart.dto.WishlistDTO;
import com.bitecart.dto.WishlistItemDTO;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.service.AiPersonalActionService;
import com.bitecart.service.CartService;
import com.bitecart.service.ProductService;
import com.bitecart.service.WishlistService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiPersonalActionServiceImpl implements AiPersonalActionService {

    private static final int PRODUCT_SEARCH_LIMIT = 10;
    private static final Pattern ADD_CART = Pattern.compile("^add\\s+(?:(\\d+)\\s+)?(.+?)\\s+to\\s+(?:my\\s+)?cart$", Pattern.CASE_INSENSITIVE);
    private static final Pattern UPDATE_CART = Pattern.compile("^(?:set|change|update)\\s+(.+?)\\s+(?:quantity\\s+)?to\\s+(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern REMOVE_CART = Pattern.compile("^(confirm\\s+)?remove\\s+(.+?)\\s+from\\s+(?:my\\s+)?cart$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADD_WISHLIST = Pattern.compile("^add\\s+(.+?)\\s+to\\s+(?:my\\s+)?(?:wishlist|wish\\s+list)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern REMOVE_WISHLIST = Pattern.compile("^(confirm\\s+)?remove\\s+(.+?)\\s+from\\s+(?:my\\s+)?(?:wishlist|wish\\s+list)$", Pattern.CASE_INSENSITIVE);

    private final ProductService productService;
    private final CartService cartService;
    private final WishlistService wishlistService;

    public AiPersonalActionServiceImpl(ProductService productService, CartService cartService,
                                       WishlistService wishlistService) {
        this.productService = productService;
        this.cartService = cartService;
        this.wishlistService = wishlistService;
    }

    @Override
    @Transactional
    public Optional<String> handleAction(String message) {
        String normalized = normalize(message);
        if (normalized.isEmpty()) return Optional.empty();

        Matcher addCart = ADD_CART.matcher(normalized);
        if (addCart.matches()) {
            int quantity = addCart.group(1) == null ? 1 : parseQuantity(addCart.group(1));
            ProductDTO product = resolveActiveProduct(addCart.group(2));
            CartDTO cart = cartService.addProduct(product.id(), quantity);
            return Optional.of(product.name() + " was added to your cart. Cart now has " + cart.totalItems() + " item(s).");
        }

        Matcher updateCart = UPDATE_CART.matcher(normalized);
        if (updateCart.matches()) {
            int quantity = parseQuantity(updateCart.group(2));
            CartItemDTO item = resolveOwnedCartItem(updateCart.group(1));
            cartService.updateItemQuantity(item.id(), quantity);
            return Optional.of("Updated " + item.productName() + " to quantity " + quantity + " in your cart.");
        }

        Matcher removeCart = REMOVE_CART.matcher(normalized);
        if (removeCart.matches()) {
            CartItemDTO item = resolveOwnedCartItem(removeCart.group(2));
            if (removeCart.group(1) == null) {
                return Optional.of("Please confirm: reply ‘confirm remove " + item.productName() + " from cart’ to remove it.");
            }
            cartService.removeItem(item.id());
            return Optional.of(item.productName() + " was removed from your cart.");
        }

        Matcher addWishlist = ADD_WISHLIST.matcher(normalized);
        if (addWishlist.matches()) {
            ProductDTO product = resolveActiveProduct(addWishlist.group(1));
            wishlistService.addProduct(product.id());
            return Optional.of(product.name() + " was added to your wishlist.");
        }

        Matcher removeWishlist = REMOVE_WISHLIST.matcher(normalized);
        if (removeWishlist.matches()) {
            WishlistItemDTO item = resolveOwnedWishlistItem(removeWishlist.group(2));
            if (removeWishlist.group(1) == null) {
                return Optional.of("Please confirm: reply ‘confirm remove " + item.productName() + " from wishlist’ to remove it.");
            }
            wishlistService.removeItem(item.id());
            return Optional.of(item.productName() + " was removed from your wishlist.");
        }
        return Optional.empty();
    }

    private ProductDTO resolveActiveProduct(String productName) {
        String query = normalize(productName);
        String canonicalQuery = canonicalizeProductName(query);
        Map<Long, ProductDTO> candidates = new LinkedHashMap<>();
        addCandidates(candidates, query);
        if (!canonicalQuery.equals(query)) addCandidates(candidates, canonicalQuery);
        List<ProductDTO> matches = candidates.values().stream()
                .filter(product -> canonicalizeProductName(normalize(product.name())).equals(canonicalQuery))
                .toList();
        if (matches.size() == 1) return matches.get(0);
        if (matches.isEmpty()) throw new ResourceNotFoundException("Active product", null);
        throw new IllegalArgumentException("More than one active product matches that name. Please use the exact product name.");
    }

    private void addCandidates(Map<Long, ProductDTO> candidates, String query) {
        Page<ProductDTO> page = productService.getCatalog(new CatalogFilter(query, null, null, null, null,
                ProductSort.NAME_ASC, 0, PRODUCT_SEARCH_LIMIT));
        page.getContent().forEach(product -> candidates.putIfAbsent(product.id(), product));
    }

    private CartItemDTO resolveOwnedCartItem(String productName) {
        String query = normalize(productName);
        CartDTO cart = cartService.findCurrentUsersCart().orElseThrow(() -> new ResourceNotFoundException("Cart item", null));
        return exactOne(cart.items(), CartItemDTO::productName, query, "cart");
    }

    private WishlistItemDTO resolveOwnedWishlistItem(String productName) {
        String query = normalize(productName);
        WishlistDTO wishlist = wishlistService.getCurrentUsersWishlist();
        return exactOne(wishlist.items(), WishlistItemDTO::productName, query, "wishlist");
    }

    private <T> T exactOne(List<T> items, java.util.function.Function<T, String> name, String query, String location) {
        List<T> matches = items.stream().filter(item -> normalize(name.apply(item)).equals(query)).toList();
        if (matches.size() == 1) return matches.get(0);
        if (matches.isEmpty()) throw new ResourceNotFoundException("Item in " + location, null);
        throw new IllegalArgumentException("More than one item has that name. Please use the exact product name.");
    }

    private int parseQuantity(String value) {
        try {
            int quantity = Integer.parseInt(value);
            if (quantity <= 0) throw new NumberFormatException();
            return quantity;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("[.?!]+$", "").replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String canonicalizeProductName(String value) {
        String[] words = value.split(" ");
        for (int index = 0; index < words.length; index++) {
            String word = words[index];
            if (word.endsWith("ies") && word.length() > 3) {
                words[index] = word.substring(0, word.length() - 3) + "y";
            } else if (word.endsWith("s") && !word.endsWith("ss") && word.length() > 2) {
                words[index] = word.substring(0, word.length() - 1);
            }
        }
        return String.join(" ", words);
    }
}
