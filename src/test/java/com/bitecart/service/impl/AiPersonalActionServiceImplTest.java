package com.bitecart.service.impl;

import com.bitecart.dto.CartDTO;
import com.bitecart.dto.CartItemDTO;
import com.bitecart.dto.CatalogFilter;
import com.bitecart.dto.ProductDTO;
import com.bitecart.dto.WishlistDTO;
import com.bitecart.dto.WishlistItemDTO;
import com.bitecart.service.CartService;
import com.bitecart.service.ProductService;
import com.bitecart.service.WishlistService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiPersonalActionServiceImplTest {

    private final ProductService productService = mock(ProductService.class);
    private final CartService cartService = mock(CartService.class);
    private final WishlistService wishlistService = mock(WishlistService.class);
    private final AiPersonalActionServiceImpl service = new AiPersonalActionServiceImpl(productService, cartService, wishlistService);

    @Test
    void addsOnlyAnActiveProductResolvedThroughProductServiceToCurrentUsersCart() {
        ProductDTO product = product(12L, "Chicken Biryani");
        when(productService.getCatalog(any(CatalogFilter.class))).thenReturn(new PageImpl<>(List.of(product)));
        when(cartService.addProduct(12L, 2)).thenReturn(new CartDTO(1L, List.of(), 2, new BigDecimal("500.00")));

        String reply = service.handleAction("add 2 Chicken Biryani to cart").orElseThrow();

        assertThat(reply).contains("added to your cart");
        verify(cartService).addProduct(12L, 2);
        verify(wishlistService, never()).addProduct(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void resolvesTheCustomerPluralProductWordingBeforeCallingTheSecuredCartService() {
        ProductDTO product = product(26L, "Tandoori BBQ Chicken Leg");
        when(productService.getCatalog(any(CatalogFilter.class))).thenAnswer(invocation -> {
            CatalogFilter filter = invocation.getArgument(0);
            return new PageImpl<>("tandoori bbq chicken leg".equals(filter.search()) ? List.of(product) : List.of());
        });
        when(cartService.addProduct(26L, 2)).thenReturn(new CartDTO(1L, List.of(), 2, new BigDecimal("458.00")));

        String reply = service.handleAction("Add 2 Tandoori BBQ Chicken Legs to my cart").orElseThrow();

        assertThat(reply).contains("Tandoori BBQ Chicken Leg was added");
        verify(cartService).addProduct(26L, 2);
    }

    @Test
    void asksForConfirmationBeforeRemovingCurrentUsersCartItem() {
        CartItemDTO item = new CartItemDTO(30L, 12L, "Chicken Biryani", null, 1,
                new BigDecimal("250.00"), new BigDecimal("250.00"));
        when(cartService.findCurrentUsersCart()).thenReturn(Optional.of(new CartDTO(1L, List.of(item), 1, new BigDecimal("250.00"))));

        String reply = service.handleAction("remove Chicken Biryani from cart").orElseThrow();

        assertThat(reply).contains("Please confirm").contains("confirm remove Chicken Biryani from cart");
        verify(cartService, never()).removeItem(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void removesOnlyCurrentUsersCartItemAfterExplicitConfirmation() {
        CartItemDTO item = new CartItemDTO(30L, 12L, "Chicken Biryani", null, 1,
                new BigDecimal("250.00"), new BigDecimal("250.00"));
        when(cartService.findCurrentUsersCart()).thenReturn(Optional.of(new CartDTO(1L, List.of(item), 1, new BigDecimal("250.00"))));

        service.handleAction("confirm remove Chicken Biryani from cart");

        verify(cartService).removeItem(30L);
    }

    @Test
    void changesOnlyCurrentUsersCartItemQuantity() {
        CartItemDTO item = new CartItemDTO(30L, 12L, "Chicken Biryani", null, 1,
                new BigDecimal("250.00"), new BigDecimal("250.00"));
        when(cartService.findCurrentUsersCart()).thenReturn(Optional.of(new CartDTO(1L, List.of(item), 1, new BigDecimal("250.00"))));

        String reply = service.handleAction("change Chicken Biryani quantity to 3").orElseThrow();

        assertThat(reply).contains("quantity 3");
        verify(cartService).updateItemQuantity(30L, 3);
    }

    @Test
    void addsOnlyAnActiveProductResolvedThroughProductServiceToCurrentUsersWishlist() {
        ProductDTO product = product(12L, "Chicken Biryani");
        when(productService.getCatalog(any(CatalogFilter.class))).thenReturn(new PageImpl<>(List.of(product)));

        String reply = service.handleAction("add Chicken Biryani to wishlist").orElseThrow();

        assertThat(reply).contains("added to your wishlist");
        verify(wishlistService).addProduct(12L);
        verify(cartService, never()).addProduct(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void usesOnlyCurrentUsersWishlistItemForConfirmedRemoval() {
        WishlistItemDTO item = new WishlistItemDTO(41L, 12L, "Chicken Biryani", null, new BigDecimal("300.00"),
                new BigDecimal("250.00"), "Biryani", true);
        when(wishlistService.getCurrentUsersWishlist()).thenReturn(new WishlistDTO(List.of(item), 1));

        service.handleAction("confirm remove Chicken Biryani from wishlist");

        verify(wishlistService).removeItem(41L);
        verify(cartService, never()).removeItem(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void rejectsAProductNotReturnedByTheActiveCatalogService() {
        when(productService.getCatalog(any(CatalogFilter.class))).thenReturn(new PageImpl<>(List.of()));

        assertThatThrownBy(() -> service.handleAction("add Unknown Dish to cart"))
                .isInstanceOf(RuntimeException.class);
        verify(cartService, never()).addProduct(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt());
    }

    private ProductDTO product(Long id, String name) {
        return new ProductDTO(id, name, "description", new BigDecimal("300.00"), new BigDecimal("250.00"), null,
                10, 1L, "Biryani", new BigDecimal("4.5"), false, true);
    }
}
