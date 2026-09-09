package com.bitecart.service;

import com.bitecart.dto.CategoryDTO;

import java.util.List;

public interface CategoryService {

    List<CategoryDTO> getActiveCategories();

    CategoryDTO getActiveCategoryById(Long id);

    CategoryDTO getActiveCategoryByName(String name);
}
