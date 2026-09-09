package com.bitecart.controller.user;

import com.bitecart.exception.InsufficientStockException;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    public String cart(Model model) {
        model.addAttribute("cart", cartService.getCurrentUsersCart());
        return "cart";
    }

    @PostMapping("/cart/items")
    public String addItem(@RequestParam Long productId, @RequestParam(defaultValue = "1") int quantity,
                          RedirectAttributes redirectAttributes) {
        try {
            cartService.addProduct(productId, quantity);
            redirectAttributes.addFlashAttribute("cartSuccess", "Added to your cart.");
        } catch (ResourceNotFoundException | InsufficientStockException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("cartError", exception.getMessage());
        }
        return "redirect:/products/" + productId;
    }

    @PostMapping("/cart/items/{itemId}")
    public String updateItem(@PathVariable Long itemId, @RequestParam int quantity, RedirectAttributes redirectAttributes) {
        try {
            cartService.updateItemQuantity(itemId, quantity);
            redirectAttributes.addFlashAttribute("cartSuccess", "Cart updated.");
        } catch (ResourceNotFoundException | InsufficientStockException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("cartError", exception.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/items/{itemId}/remove")
    public String removeItem(@PathVariable Long itemId, RedirectAttributes redirectAttributes) {
        try {
            cartService.removeItem(itemId);
            redirectAttributes.addFlashAttribute("cartSuccess", "Item removed from your cart.");
        } catch (ResourceNotFoundException exception) {
            redirectAttributes.addFlashAttribute("cartError", "That cart item is unavailable.");
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String clearCart(RedirectAttributes redirectAttributes) {
        cartService.clearCart();
        redirectAttributes.addFlashAttribute("cartSuccess", "Your cart has been cleared.");
        return "redirect:/cart";
    }
}
