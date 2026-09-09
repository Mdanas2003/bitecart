package com.bitecart.service.impl;

import com.bitecart.dto.CategoryDTO;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.mapper.CatalogMapper;
import com.bitecart.repository.CategoryRepository;
import com.bitecart.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CatalogMapper catalogMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository, CatalogMapper catalogMapper) {
        this.categoryRepository = categoryRepository;
        this.catalogMapper = catalogMapper;
    }

    @Override
    public List<CategoryDTO> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream().map(catalogMapper::toCategoryDto).toList();
    }

    @Override
    public CategoryDTO getActiveCategoryById(Long id) {
        return categoryRepository.findByIdAndActiveTrue(id).map(catalogMapper::toCategoryDto)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    @Override
    public CategoryDTO getActiveCategoryByName(String name) {
        return categoryRepository.findByNameIgnoreCaseAndActiveTrue(name).map(catalogMapper::toCategoryDto)
                .orElseThrow(() -> new ResourceNotFoundException("Category", null));
    }
}
