package com.bitecart.config;

import com.bitecart.repository.CategoryRepository;
import com.bitecart.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"test", "demo"})
class DemoDataSeederIntegrationTests {

    @Autowired
    private DemoDataSeeder demoDataSeeder;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void demoProfileSeedsCatalogueOnceAndCanRunAgainWithoutDuplicates() throws Exception {
        long categoryCount = categoryRepository.count();
        long productCount = productRepository.count();
        assertThat(categoryCount).isGreaterThanOrEqualTo(9);
        assertThat(productCount).isGreaterThanOrEqualTo(27);
        assertThat(categoryRepository.findByNameIgnoreCase("Biryani")).isPresent();
        assertThat(productRepository.existsByNameIgnoreCase("Ambur Chicken Biryani")).isTrue();
        assertThat(productRepository.findAll()).allSatisfy(product -> {
            assertThat(product.isActive()).isTrue();
            assertThat(product.getCategory()).isNotNull();
        });

        demoDataSeeder.run();

        assertThat(categoryRepository.count()).isEqualTo(categoryCount);
        assertThat(productRepository.count()).isEqualTo(productCount);
    }
}
