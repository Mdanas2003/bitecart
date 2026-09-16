package com.bitecart.service.impl;

import com.bitecart.dto.CategoryDTO;
import com.bitecart.dto.ProductDTO;
import com.bitecart.service.CategoryService;
import com.bitecart.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogKnowledgeServiceImplTest {

    @Test
    void providesOnlyActiveCatalogFactsForCategoryPriceAndAvailabilityQuestions() {
        ProductService products = mock(ProductService.class);
        CategoryService categories = mock(CategoryService.class);
        CategoryDTO biryani = new CategoryDTO(3L, "Biryani", "Rice meals", null, true);
        ProductDTO product = new ProductDTO(11L, "Chicken Biryani", "Aromatic basmati rice with chicken.",
                new BigDecimal("299.00"), new BigDecimal("249.00"), null, 8, 3L, "Biryani",
                new BigDecimal("4.6"), true, true);
        when(categories.getActiveCategories()).thenReturn(List.of(biryani));
        when(products.getCatalog(any())).thenReturn(new PageImpl<>(List.of(product)));

        String context = new CatalogKnowledgeServiceImpl(products, categories)
                .findCatalogContext("What is the price and availability of Chicken Biryani?");

        assertThat(context).contains("Biryani", "Chicken Biryani", "₹249.00", "in stock (8 available)",
                "Aromatic basmati rice with chicken.");
        assertThat(context).contains("active listings only");
    }

    @Test
    void refusesToInventCatalogDataWhenControlledSearchHasNoResults() {
        ProductService products = mock(ProductService.class);
        CategoryService categories = mock(CategoryService.class);
        when(categories.getActiveCategories()).thenReturn(List.of(new CategoryDTO(1L, "Burgers", null, null, true)));
        when(products.getCatalog(any())).thenReturn(new PageImpl<>(List.of()));

        String context = new CatalogKnowledgeServiceImpl(products, categories).findCatalogContext("Do you have sushi?");

        assertThat(context).contains("No matching active product was found");
        assertThat(context).doesNotContain("sushi | category");
    }
}
