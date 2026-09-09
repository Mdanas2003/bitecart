package com.bitecart.service;

import com.bitecart.dto.CatalogFilter;
import com.bitecart.dto.ProductDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductService {

    Page<ProductDTO> getCatalog(CatalogFilter filter);

    ProductDTO getActiveProductById(Long id);

    List<ProductDTO> getFeaturedProducts();

    List<ProductDTO> getRelatedProducts(Long productId);
}
