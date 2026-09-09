package com.bitecart.mapper;

import com.bitecart.dto.CategoryDTO;
import com.bitecart.dto.ProductDTO;
import com.bitecart.entity.Category;
import com.bitecart.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class CatalogMapper {

    public CategoryDTO toCategoryDto(Category category) {
        return new CategoryDTO(category.getId(), category.getName(), category.getDescription(),
                category.getImageUrl(), category.isActive());
    }

    public ProductDTO toProductDto(Product product) {
        Category category = product.getCategory();
        return new ProductDTO(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getDiscountPrice(), product.getImageUrl(), product.getStockQuantity(), category.getId(),
                category.getName(), product.getRating(), product.isFeatured(), product.isActive());
    }
}
