package com.bitecart.config;

import com.bitecart.entity.Category;
import com.bitecart.entity.Product;
import com.bitecart.repository.CategoryRepository;
import com.bitecart.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Opt-in demo catalogue data. It is deliberately disabled for production and only inserts missing names.
 */
@Component
@Profile("demo & !prod")
public class DemoDataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public DemoDataSeeder(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, Category> categories = new HashMap<>();
        for (CategorySeed seed : CATEGORY_SEEDS) {
            categories.put(seed.name(), categoryRepository.findByNameIgnoreCase(seed.name())
                    .orElseGet(() -> createCategory(seed)));
        }
        for (ProductSeed seed : PRODUCT_SEEDS) {
            if (!productRepository.existsByNameIgnoreCase(seed.name())) {
                productRepository.save(createProduct(seed, categories.get(seed.category())));
            }
        }
    }

    private Category createCategory(CategorySeed seed) {
        Category category = new Category();
        category.setName(seed.name());
        category.setDescription(seed.description());
        category.setImageUrl(image(seed.name()));
        category.setActive(true);
        return categoryRepository.save(category);
    }

    private Product createProduct(ProductSeed seed, Category category) {
        Product product = new Product();
        product.setName(seed.name());
        product.setDescription(seed.description());
        product.setPrice(money(seed.price()));
        product.setDiscountPrice(seed.discountPrice() == null ? null : money(seed.discountPrice()));
        product.setImageUrl(image(seed.name()));
        product.setStockQuantity(seed.stock());
        product.setCategory(category);
        product.setRating(new BigDecimal(seed.rating()));
        product.setFeatured(seed.featured());
        product.setActive(true);
        return product;
    }

    private BigDecimal money(String value) { return new BigDecimal(value); }

    private String image(String label) {
        return "https://placehold.co/800x600/fff4e8/e95b2b?text=" + label.replace(" ", "%20");
    }

    private record CategorySeed(String name, String description) { }
    private record ProductSeed(String category, String name, String description, String price, String discountPrice,
                               int stock, String rating, boolean featured) { }

    private static final CategorySeed[] CATEGORY_SEEDS = {
            new CategorySeed("Biryani", "Fragrant dum biryanis made for Chennai cravings."),
            new CategorySeed("Burgers", "Juicy burgers, loaded fries, and comfort-food favourites."),
            new CategorySeed("Shawarma", "Freshly rolled shawarmas with bold Mediterranean flavour."),
            new CategorySeed("BBQ Chicken", "Char-grilled chicken with smoky house marinades."),
            new CategorySeed("Waffles", "Golden waffles with sweet and savoury toppings."),
            new CategorySeed("Desserts", "Celebration-worthy Indian and global sweet treats."),
            new CategorySeed("Ice Cream", "Creamy scoops, sundaes, and kulfi-inspired desserts."),
            new CategorySeed("Cool Drinks", "Chilled coolers, shakes, and refreshing beverages."),
            new CategorySeed("South Indian Snacks", "Crisp, comforting Chennai tea-time classics.")
    };

    private static final ProductSeed[] PRODUCT_SEEDS = {
            new ProductSeed("Biryani", "Ambur Chicken Biryani", "Seeraga samba rice, tender chicken, and Ambur-style spice.", "289.00", "259.00", 28, "4.6", true),
            new ProductSeed("Biryani", "Dindigul Mutton Biryani", "Slow-cooked mutton with fragrant jeera samba rice.", "379.00", null, 16, "4.7", true),
            new ProductSeed("Biryani", "Paneer Dum Biryani", "Paneer, saffron rice, and caramelised onions with raita.", "249.00", "219.00", 22, "4.4", false),
            new ProductSeed("Burgers", "Chennai Chicken Burger", "Crispy chicken, peri-peri mayo, onions, and a toasted bun.", "199.00", "179.00", 30, "4.5", true),
            new ProductSeed("Burgers", "Smoky Paneer Burger", "Tandoori paneer patty, mint slaw, and chipotle sauce.", "189.00", null, 24, "4.3", false),
            new ProductSeed("Burgers", "Double Cheese Veg Burger", "Two veggie patties, cheddar, lettuce, and house sauce.", "209.00", "189.00", 20, "4.2", false),
            new ProductSeed("Shawarma", "Arabic Chicken Shawarma", "Garlic chicken, pickles, fries, and creamy toum in khuboos.", "179.00", "159.00", 35, "4.6", true),
            new ProductSeed("Shawarma", "Spicy Mexican Shawarma", "Grilled chicken with jalapeños, salsa, and smoky mayo.", "189.00", null, 26, "4.4", false),
            new ProductSeed("Shawarma", "Falafel Veg Shawarma", "Crisp falafel, hummus, lettuce, and tangy tahini.", "159.00", null, 18, "4.2", false),
            new ProductSeed("BBQ Chicken", "Half Peri Peri BBQ Chicken", "Flame-grilled half chicken with a fiery peri-peri glaze.", "329.00", "299.00", 18, "4.7", true),
            new ProductSeed("BBQ Chicken", "Honey Chilli BBQ Wings", "Eight sticky grilled wings with sesame and spring onion.", "279.00", null, 21, "4.5", false),
            new ProductSeed("BBQ Chicken", "Tandoori BBQ Chicken Leg", "Charred chicken leg marinated in yoghurt and Kashmiri chilli.", "229.00", null, 20, "4.4", false),
            new ProductSeed("Waffles", "Belgian Chocolate Waffle", "Warm waffle with chocolate sauce, chips, and vanilla cream.", "189.00", "169.00", 20, "4.5", true),
            new ProductSeed("Waffles", "Lotus Biscoff Waffle", "Golden waffle drizzled with Biscoff spread and biscuit crumbs.", "229.00", null, 14, "4.7", false),
            new ProductSeed("Waffles", "Nutella Banana Waffle", "Nutella, sliced banana, and roasted almonds on a crisp waffle.", "219.00", null, 16, "4.6", false),
            new ProductSeed("Desserts", "Gulab Jamun Sundae", "Warm gulab jamun, vanilla ice cream, pistachio, and rose syrup.", "149.00", null, 25, "4.4", false),
            new ProductSeed("Desserts", "Baked Chocolate Brownie", "Fudgy dark chocolate brownie served warm.", "129.00", null, 30, "4.5", false),
            new ProductSeed("Desserts", "Elaneer Payasam", "Tender coconut payasam with cashews and a light cardamom finish.", "119.00", "99.00", 18, "4.3", false),
            new ProductSeed("Ice Cream", "Madagascar Vanilla Bean Scoop", "Creamy vanilla bean ice cream made for simple indulgence.", "89.00", null, 40, "4.3", false),
            new ProductSeed("Ice Cream", "Tender Coconut Ice Cream", "Coconut ice cream with soft coconut pieces.", "109.00", null, 34, "4.6", true),
            new ProductSeed("Ice Cream", "Mango Kulfi Stick", "Dense alphonso mango kulfi with pistachio bits.", "99.00", null, 28, "4.4", false),
            new ProductSeed("Cool Drinks", "Nannari Lemon Soda", "Chilled nannari syrup, lemon, and sparkling soda.", "69.00", null, 45, "4.5", false),
            new ProductSeed("Cool Drinks", "Rose Milk", "Classic Chennai rose milk, cold and creamy.", "79.00", null, 38, "4.4", false),
            new ProductSeed("Cool Drinks", "Cold Coffee Frappe", "Iced coffee blended with milk, cocoa, and whipped cream.", "129.00", "109.00", 26, "4.5", true),
            new ProductSeed("South Indian Snacks", "Mini Ghee Podi Idli", "Soft mini idlis tossed with ghee, podi, and curry leaves.", "139.00", null, 30, "4.6", false),
            new ProductSeed("South Indian Snacks", "Chettinad Chicken 65", "Crispy chicken bites with curry leaves and Chettinad masala.", "219.00", "199.00", 24, "4.7", true),
            new ProductSeed("South Indian Snacks", "Masala Vada Basket", "Four crunchy chana dal vadas with coconut chutney.", "99.00", null, 35, "4.2", false)
    };
}
