package com.bitecart.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class AdminProductForm {
    @NotBlank @Size(max = 200) private String name;
    @Size(max = 5000) private String description;
    @NotNull @DecimalMin(value = "0.00", inclusive = false) private BigDecimal price;
    @DecimalMin(value = "0.00", inclusive = false) private BigDecimal discountPrice;
    @Size(max = 500) private String imageUrl;
    @NotNull @Min(0) private Integer stockQuantity;
    @NotNull private Long categoryId;
    private boolean featured;
    private boolean active = true;
    public String getName() { return name; } public void setName(String value) { name = value; }
    public String getDescription() { return description; } public void setDescription(String value) { description = value; }
    public BigDecimal getPrice() { return price; } public void setPrice(BigDecimal value) { price = value; }
    public BigDecimal getDiscountPrice() { return discountPrice; } public void setDiscountPrice(BigDecimal value) { discountPrice = value; }
    public String getImageUrl() { return imageUrl; } public void setImageUrl(String value) { imageUrl = value; }
    public Integer getStockQuantity() { return stockQuantity; } public void setStockQuantity(Integer value) { stockQuantity = value; }
    public Long getCategoryId() { return categoryId; } public void setCategoryId(Long value) { categoryId = value; }
    public boolean isFeatured() { return featured; } public void setFeatured(boolean value) { featured = value; }
    public boolean isActive() { return active; } public void setActive(boolean value) { active = value; }
}
