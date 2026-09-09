package com.bitecart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReviewSubmissionRequest {

    @NotNull(message = "Select the delivered order for this review.")
    private Long orderId;

    @NotNull(message = "Choose a rating.")
    @Min(value = 1, message = "Rating must be between 1 and 5.")
    @Max(value = 5, message = "Rating must be between 1 and 5.")
    private Integer rating;

    @NotBlank(message = "Share a short review comment.")
    @Size(min = 3, max = 1000, message = "Review comments must be between 3 and 1000 characters.")
    private String comment;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
