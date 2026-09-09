package com.bitecart.controller.user;

import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.service.WishlistService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping("/wishlist")
    public String wishlist(Model model) {
        model.addAttribute("wishlist", wishlistService.getCurrentUsersWishlist());
        return "wishlist";
    }

    @PostMapping("/wishlist/items")
    public String addItem(@RequestParam Long productId, RedirectAttributes redirectAttributes) {
        try {
            wishlistService.addProduct(productId);
            redirectAttributes.addFlashAttribute("wishlistSuccess", "Saved to your wishlist.");
        } catch (ResourceNotFoundException exception) {
            redirectAttributes.addFlashAttribute("wishlistError", "That product is unavailable.");
        }
        return "redirect:/products/" + productId;
    }

    @PostMapping("/wishlist/items/{itemId}/remove")
    public String removeItem(@PathVariable Long itemId, RedirectAttributes redirectAttributes) {
        try {
            wishlistService.removeItem(itemId);
            redirectAttributes.addFlashAttribute("wishlistSuccess", "Removed from your wishlist.");
        } catch (ResourceNotFoundException exception) {
            redirectAttributes.addFlashAttribute("wishlistError", "That wishlist item is unavailable.");
        }
        return "redirect:/wishlist";
    }
}
