package com.bitecart.service.impl;

import com.bitecart.dto.CatalogFilter;
import com.bitecart.dto.ProductDTO;
import com.bitecart.entity.Product;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.mapper.CatalogMapper;
import com.bitecart.repository.ProductRepository;
import com.bitecart.service.ProductService;
import com.bitecart.specification.ProductSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private static final int FEATURED_LIMIT = 6;
    private static final int RELATED_LIMIT = 4;

    private final ProductRepository productRepository;
    private final CatalogMapper catalogMapper;

    public ProductServiceImpl(ProductRepository productRepository, CatalogMapper catalogMapper) {
        this.productRepository = productRepository;
        this.catalogMapper = catalogMapper;
    }

    @Override
    public Page<ProductDTO> getCatalog(CatalogFilter filter) {
        PageRequest pageable = PageRequest.of(filter.page(), filter.size(), filter.sort().getSort());
        return productRepository.findAll(ProductSpecifications.activeCatalog(filter), pageable).map(catalogMapper::toProductDto);
    }

    @Override
    public ProductDTO getActiveProductById(Long id) {
        return productRepository.findByIdAndActiveTrue(id).map(catalogMapper::toProductDto)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Override
    public List<ProductDTO> getFeaturedProducts() {
        return productRepository.findTop6ByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc().stream()
                .limit(FEATURED_LIMIT)
                .filter(product -> product.getCategory().isActive())
                .map(catalogMapper::toProductDto)
                .toList();
    }

    @Override
    public List<ProductDTO> getRelatedProducts(Long productId) {
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        if (!product.getCategory().isActive()) {
            return List.of();
        }
        return productRepository.findTop4ByActiveTrueAndCategoryIdAndIdNotOrderByCreatedAtDesc(
                        product.getCategory().getId(), productId, PageRequest.of(0, RELATED_LIMIT))
                .stream()
                .filter(related -> related.getCategory().isActive())
                .map(catalogMapper::toProductDto)
                .toList();
    }
}
