package com.bitecart.controller.user;

import com.bitecart.dto.ReviewSubmissionRequest;
import com.bitecart.exception.DuplicateReviewException;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.exception.ReviewNotAllowedException;
import com.bitecart.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/products/{productId}/reviews")
    public String submitReview(@PathVariable Long productId,
                               @Valid @ModelAttribute("reviewForm") ReviewSubmissionRequest reviewForm,
                               BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("reviewError", "Use a rating from 1 to 5 and a review comment between 3 and 1000 characters.");
            return "redirect:/products/" + productId;
        }
        try {
            reviewService.submitReview(productId, reviewForm);
            redirectAttributes.addFlashAttribute("reviewSuccess", "Thanks for sharing your experience.");
        } catch (ReviewNotAllowedException | DuplicateReviewException | ResourceNotFoundException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("reviewError", exception.getMessage());
        }
        return "redirect:/products/" + productId;
    }
}
