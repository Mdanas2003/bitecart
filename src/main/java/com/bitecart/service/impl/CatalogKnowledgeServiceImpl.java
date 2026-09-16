package com.bitecart.service.impl;

import com.bitecart.dto.CatalogFilter;
import com.bitecart.dto.CategoryDTO;
import com.bitecart.dto.ProductDTO;
import com.bitecart.dto.ProductSort;
import com.bitecart.service.CatalogKnowledgeService;
import com.bitecart.service.CategoryService;
import com.bitecart.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class CatalogKnowledgeServiceImpl implements CatalogKnowledgeService {

    private static final int MAX_PRODUCTS = 6;
    private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9 ]");
    private static final Set<String> STOP_WORDS = Set.of("what", "which", "show", "tell", "about", "price", "cost",
            "available", "availability", "stock", "have", "with", "from", "bitecart", "please", "the", "and",
            "for", "can", "you", "me", "is", "are", "do", "i", "want", "need", "details", "product", "products");

    private final ProductService productService;
    private final CategoryService categoryService;

    public CatalogKnowledgeServiceImpl(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @Override
    public String findCatalogContext(String customerQuestion) {
        List<CategoryDTO> categories = categoryService.getActiveCategories();
        CategoryDTO matchedCategory = matchCategory(customerQuestion, categories);
        LinkedHashMap<Long, ProductDTO> products = new LinkedHashMap<>();

        if (matchedCategory != null) {
            addProducts(products, productService.getCatalog(catalogFilter(null, matchedCategory.id())).getContent());
        }
        for (String query : searchQueries(customerQuestion)) {
            addProducts(products, productService.getCatalog(catalogFilter(query, null)).getContent());
            if (products.size() >= MAX_PRODUCTS) break;
        }

        StringBuilder context = new StringBuilder("Authoritative BiteCart catalog data (active listings only):\nCategories: ");
        context.append(categories.isEmpty() ? "none currently listed" : categories.stream().map(CategoryDTO::name).reduce((a, b) -> a + ", " + b).orElse("none"));
        if (matchedCategory != null) context.append("\nMatched category: ").append(matchedCategory.name());
        if (products.isEmpty()) {
            context.append("\nNo matching active product was found. Do not invent product names, prices, or availability.");
        } else {
            context.append("\nMatching products:");
            products.values().stream().limit(MAX_PRODUCTS).forEach(product -> context.append("\n- ").append(product.name())
                    .append(" | category: ").append(product.categoryName())
                    .append(" | current price: ₹").append(product.currentPrice())
                    .append(product.hasDiscount() ? " (regular price: ₹" + product.price() + ")" : "")
                    .append(" | availability: ").append(product.stockQuantity() > 0 ? "in stock (" + product.stockQuantity() + " available)" : "out of stock")
                    .append(" | rating: ").append(product.rating())
                    .append(" | details: ").append(product.description()));
        }
        context.append("\nUse only this catalog data for catalog facts. Do not claim access to carts, orders, payments, or customer accounts.");
        return context.toString();
    }

    private CatalogFilter catalogFilter(String search, Long categoryId) {
        return new CatalogFilter(search, categoryId, null, null, null, ProductSort.NAME_ASC, 0, MAX_PRODUCTS);
    }

    private CategoryDTO matchCategory(String question, List<CategoryDTO> categories) {
        String normalized = normalize(question);
        return categories.stream().filter(category -> normalized.contains(normalize(category.name())))
                .findFirst().orElse(null);
    }

    private List<String> searchQueries(String question) {
        List<String> words = List.of(normalize(question).split(" ")).stream()
                .filter(word -> word.length() > 1 && !STOP_WORDS.contains(word)).toList();
        LinkedHashSet<String> queries = new LinkedHashSet<>();
        if (words.size() > 1) queries.add(String.join(" ", words));
        for (int length = Math.min(3, words.size()); length >= 1 && queries.size() < 4; length--) {
            for (int start = 0; start + length <= words.size() && queries.size() < 4; start++) {
                queries.add(String.join(" ", words.subList(start, start + length)));
            }
        }
        return new ArrayList<>(queries);
    }

    private String normalize(String value) {
        return value == null ? "" : NON_WORD.matcher(value.toLowerCase(Locale.ROOT)).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }

    private void addProducts(Map<Long, ProductDTO> target, Collection<ProductDTO> source) {
        for (ProductDTO product : source) {
            if (target.size() >= MAX_PRODUCTS) return;
            target.putIfAbsent(product.id(), product);
        }
    }
}
