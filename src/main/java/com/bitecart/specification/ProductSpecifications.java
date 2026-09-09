package com.bitecart.specification;

import com.bitecart.dto.CatalogFilter;
import com.bitecart.entity.Category;
import com.bitecart.entity.Product;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> activeCatalog(CatalogFilter filter) {
        return active()
                .and(inActiveCategory())
                .and(matchesSearch(filter.search()))
                .and(hasCategory(filter.categoryId()))
                .and(priceAtLeast(filter.minPrice()))
                .and(priceAtMost(filter.maxPrice()))
                .and(ratingAtLeast(filter.minRating()));
    }

    private static Specification<Product> active() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("active"));
    }

    private static Specification<Product> inActiveCategory() {
        return (root, query, criteriaBuilder) -> {
            Join<Product, Category> category = root.join("category");
            return criteriaBuilder.isTrue(category.get("active"));
        };
    }

    private static Specification<Product> matchesSearch(String search) {
        if (search == null || search.isBlank()) {
            return unrestricted();
        }
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern));
    }

    private static Specification<Product> hasCategory(Long categoryId) {
        if (categoryId == null) {
            return unrestricted();
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("category").get("id"), categoryId);
    }

    private static Specification<Product> priceAtLeast(BigDecimal minPrice) {
        return minPrice == null ? unrestricted()
                : (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    private static Specification<Product> priceAtMost(BigDecimal maxPrice) {
        return maxPrice == null ? unrestricted()
                : (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    private static Specification<Product> ratingAtLeast(BigDecimal minRating) {
        return minRating == null ? unrestricted()
                : (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("rating"), minRating);
    }

    private static Specification<Product> unrestricted() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }
}
