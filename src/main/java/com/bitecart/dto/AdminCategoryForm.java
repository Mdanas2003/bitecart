package com.bitecart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminCategoryForm {
    @NotBlank @Size(max = 120) private String name;
    @Size(max = 2000) private String description;
    @Size(max = 500) private String imageUrl;
    private boolean active = true;
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; } public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public boolean isActive() { return active; } public void setActive(boolean active) { this.active = active; }
}
