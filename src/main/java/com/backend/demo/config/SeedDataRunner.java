package com.backend.demo.config;

import com.backend.demo.model.Category;
import com.backend.demo.model.Collection;
import com.backend.demo.model.Product;
import com.backend.demo.model.Role;
import com.backend.demo.model.User;
import com.backend.demo.model.UserTier;
import java.util.HashSet;
import java.util.Set;
import com.backend.demo.repository.CategoryRepository;
import com.backend.demo.repository.CollectionRepository;
import com.backend.demo.repository.ProductRepository;
import com.backend.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeedDataRunner implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final CollectionRepository collectionRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("🌱 Starting seed data initialization...");
        
        try {
            // Seed Categories
            seedCategories();
            
            // Seed Collections
            seedCollections();
            
            // Seed Products
            seedProducts();
            
            // Seed Admin User
            seedAdminUser();
            
            log.info("✅ Seed data initialization completed!");
        } catch (Exception e) {
            log.error("❌ Error during seed data initialization: {}", e.getMessage());
        }
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            log.info("Categories already exist, skipping...");
            return;
        }

        categoryRepository.save(Category.builder()
            .name("Ruang Tamu")
            .imageUrl("/hero-living.png")
            .description("Furnitur untuk ruang tamu")
            .isActive(true)
            .build());

        categoryRepository.save(Category.builder()
            .name("Kamar Tidur")
            .imageUrl("/hero-bedroom.png")
            .description("Furnitur untuk kamar tidur")
            .isActive(true)
            .build());

        categoryRepository.save(Category.builder()
            .name("Ruang Makan")
            .imageUrl("/hero-dining.png")
            .description("Furnitur untuk ruang makan")
            .isActive(true)
            .build());

        categoryRepository.save(Category.builder()
            .name("Kursi")
            .imageUrl("/product-chair.png")
            .description(" berbagai kursi")
            .isActive(true)
            .build());

        categoryRepository.save(Category.builder()
            .name("Meja")
            .imageUrl("/product-table.png")
            .description(" berbagai meja")
            .isActive(true)
            .build());

        categoryRepository.save(Category.builder()
            .name("Lampu")
            .imageUrl("/product-lamp.png")
            .description(" berbagai lampu")
            .isActive(true)
            .build());

        categoryRepository.save(Category.builder()
            .name("Dekorasi")
            .imageUrl("/product-ceramic-vase.png")
            .description("Dekorasi rumah")
            .isActive(true)
            .build());

        log.info("✅ Seeded {} categories", categoryRepository.count());
    }

    private void seedCollections() {
        if (collectionRepository.count() > 0) {
            log.info("Collections already exist, skipping...");
            return;
        }

        collectionRepository.save(Collection.builder()
            .name("Living Collection 2025")
            .description("Koleksi terbaru untuk ruang tamu")
            .imageUrl("/hero-living.png")
            .isActive(true)
            .build());

        collectionRepository.save(Collection.builder()
            .name("Sanctuary Series")
            .description("Koleksi untuk kamar tidur")
            .imageUrl("/hero-bedroom.png")
            .isActive(true)
            .build());

        collectionRepository.save(Collection.builder()
            .name("Dining Edition")
            .description("Koleksi untuk ruang makan")
            .imageUrl("/hero-dining.png")
            .isActive(true)
            .build());

        log.info("✅ Seeded {} collections", collectionRepository.count());
    }

    private void seedProducts() {
        if (productRepository.count() > 0) {
            log.info("Products already exist, skipping...");
            return;
        }

        Category ruangTamu = categoryRepository.findByName("Ruang Tamu").orElse(null);
        Category kamarTidur = categoryRepository.findByName("Kamar Tidur").orElse(null);
        Category ruangMakan = categoryRepository.findByName("Ruang Makan").orElse(null);
        Category kursi = categoryRepository.findByName("Kursi").orElse(null);
        Category meja = categoryRepository.findByName("Meja").orElse(null);
        Category lampu = categoryRepository.findByName("Lampu").orElse(null);
        Category dekorasi = categoryRepository.findByName("Dekorasi").orElse(null);

        // Product 1: Bouclé Armchair
        productRepository.save(Product.builder()
            .sku("MSN-CHAIR-001")
            .name("Bouclé Armchair")
            .description("Armchair dengan bahan bouclé premium yang nyaman dan elegan")
            .price(new BigDecimal("6400000"))
            .stock(15)
            .category(kursi)
            .imageUrl("/product-chair.png")
            .isActive(true)
            .isNew(false)
            .build());

        // Product 2: Olive Linen Sofa
        productRepository.save(Product.builder()
            .sku("MSN-SOFA-001")
            .name("Olive Linen Sofa")
            .description("Sofa linen dengan warna olive yang elegan dan nyaman")
            .price(new BigDecimal("12500000"))
            .stock(8)
            .category(ruangTamu)
            .imageUrl("/product-sofa.png")
            .isActive(true)
            .isNew(true)
            .build());

        // Product 3: Travertine Coffee Table
        productRepository.save(Product.builder()
            .sku("MSN-TABLE-001")
            .name("Travertine Coffee Table")
            .description("Meja kopi dengan stone travertine asli yang elegan")
            .price(new BigDecimal("8900000"))
            .stock(5)
            .category(meja)
            .imageUrl("/product-table.png")
            .isActive(true)
            .isNew(true)
            .build());

        // Product 4: Rattan Pendant Lamp
        productRepository.save(Product.builder()
            .sku("MSN-LAMP-001")
            .name("Rattan Pendant Lamp")
            .description("Lampu gantung dengan bahan rattan alami")
            .price(new BigDecimal("2750000"))
            .stock(20)
            .category(lampu)
            .imageUrl("/product-lamp.png")
            .isActive(true)
            .isNew(false)
            .build());

        // Product 5: Ceramic Statement Vase
        productRepository.save(Product.builder()
            .sku("MSN-VASE-001")
            .name("Ceramic Statement Vase")
            .description("Vase keramik handmade untuk dekorasi rumah")
            .price(new BigDecimal("1350000"))
            .stock(25)
            .category(dekorasi)
            .imageUrl("/product-ceramic-vase.png")
            .isActive(true)
            .isNew(true)
            .build());

        // Product 6: Velvet Accent Chair
        productRepository.save(Product.builder()
            .sku("MSN-CHAIR-002")
            .name("Velvet Accent Chair")
            .description("Accent chair dengan velvet premium")
            .price(new BigDecimal("7200000"))
            .stock(12)
            .category(kursi)
            .imageUrl("/product-velvet-chair.png")
            .isActive(true)
            .isNew(false)
            .build());

        // Product 7: Marble Side Table
        productRepository.save(Product.builder()
            .sku("MSN-TABLE-002")
            .name("Marble Side Table")
            .description("Meja samping dengan marble Carrara putih")
            .price(new BigDecimal("4800000"))
            .stock(7)
            .category(meja)
            .imageUrl("/product-marble-table.png")
            .isActive(true)
            .isNew(false)
            .build());

        // Product 8: Rattan Wall Panel
        productRepository.save(Product.builder()
            .sku("MSN-PANEL-001")
            .name("Rattan Wall Panel")
            .description("Panel dinding dari rattan alami untuk dekorasi")
            .price(new BigDecimal("2100000"))
            .stock(18)
            .category(dekorasi)
            .imageUrl("/product-rattan-wall.png")
            .isActive(true)
            .isNew(false)
            .build());

        log.info("✅ Seeded {} products", productRepository.count());
    }

    private void seedAdminUser() {
        if (userRepository.findByEmail("admin@maison.com") != null) {
            log.info("Admin user already exists, skipping...");
            return;
        }

        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(new Role("ROLE_ADMIN"));

        User admin = User.builder()
            .name("Admin Maison")
            .email("admin@maison.com")
            .password(passwordEncoder.encode("admin123"))
            .phone("+6281234567890")
            .tier(UserTier.ADMIN)
            .rewardPoints(0)
            .totalOrders(0)
            .isActive(true)
            .joinDate(Instant.now())
            .roles(adminRoles)
            .build();

        userRepository.save(admin);

        log.info("✅ Seeded admin user");
    }
}