package com.backend.demo.config;

import com.backend.demo.model.Category;
import com.backend.demo.model.Collection;
import com.backend.demo.model.Product;
import com.backend.demo.model.Role;
import com.backend.demo.model.User;
import com.backend.demo.model.UserTier;
import com.backend.demo.model.Voucher;
import com.backend.demo.model.VoucherType;
import java.util.HashSet;
import java.util.Set;
import com.backend.demo.repository.CategoryRepository;
import com.backend.demo.repository.CollectionRepository;
import com.backend.demo.repository.ProductRepository;
import com.backend.demo.repository.UserRepository;
import com.backend.demo.repository.VoucherRepository;
import com.backend.demo.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeedDataRunner implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final CollectionRepository collectionRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final VoucherRepository voucherRepository;
    private final AppSettingService appSettingService;
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

            // Seed additional Kamar Tidur + Ruang Makan products (idempotent by SKU)
            seedBedroomAndDiningProducts();

            // Ensure newer categories exist (Penyimpanan) — idempotent
            ensureExtraCategories();

            // Seed additional catalog products for filter categories (idempotent by SKU)
            seedExtraCatalogProducts();

            // Seed Admin User
            seedAdminUser();

            seedVouchers();

            seedSettings();
            
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

    /**
     * Seeds additional products for "Kamar Tidur" and "Ruang Makan" categories.
     * Idempotent — only inserts products whose SKU does not yet exist.
     * Safe to run on fresh and existing databases.
     */
    private void seedBedroomAndDiningProducts() {
        Category kamarTidur = categoryRepository.findByName("Kamar Tidur").orElse(null);
        Category ruangMakan = categoryRepository.findByName("Ruang Makan").orElse(null);
        Collection sanctuarySeries = collectionRepository.findByName("Sanctuary Series").orElse(null);
        Collection diningEdition = collectionRepository.findByName("Dining Edition").orElse(null);

        if (kamarTidur == null || ruangMakan == null) {
            log.warn("⚠️  Cannot seed bedroom/dining products: required categories missing");
            return;
        }

        int inserted = 0;

        // ============ KAMAR TIDUR (Bedroom) ============
        inserted += saveIfMissing(Product.builder()
            .sku("MSN-BED-001")
            .name("King Size Upholstered Bed")
            .description("Tempat tidur king size dengan headboard upholstery velvet premium. Frame kayu solid oak, cocok untuk kamar utama yang mengutamakan kenyamanan dan estetika mewah.")
            .price(new BigDecimal("18500000"))
            .salePrice(new BigDecimal("16650000"))
            .stock(6)
            .category(kamarTidur)
            .collection(sanctuarySeries)
            .imageUrl("/product-velvet-chair.png")
            .material("Solid Oak + Velvet")
            .dimensions("200 x 200 x 120 cm")
            .weightKg(new BigDecimal("85.00"))
            .assemblyRequired(true)
            .warrantyMonths(24)
            .rating(new BigDecimal("4.85"))
            .isActive(true)
            .isNew(true)
            .build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-NIGHT-001")
            .name("Walnut Nightstand 2-Drawer")
            .description("Meja samping tempat tidur dengan 2 laci dari kayu walnut. Handle brass matte, permukaan tahan gores, ideal untuk lampu tidur dan buku.")
            .price(new BigDecimal("3200000"))
            .stock(14)
            .category(kamarTidur)
            .collection(sanctuarySeries)
            .imageUrl("/product-marble-table.png")
            .material("Solid Walnut + Brass")
            .dimensions("55 x 40 x 60 cm")
            .weightKg(new BigDecimal("18.50"))
            .assemblyRequired(false)
            .warrantyMonths(12)
            .rating(new BigDecimal("4.70"))
            .isActive(true)
            .isNew(false)
            .build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-DRESSER-001")
            .name("Oak 6-Drawer Dresser")
            .description("Dresser dengan 6 laci berdimensi besar untuk penyimpanan pakaian maksimal. Slow-close drawer system, kayu oak alami tanpa finishing berlebihan.")
            .price(new BigDecimal("7800000"))
            .stock(8)
            .category(kamarTidur)
            .collection(sanctuarySeries)
            .imageUrl("/product-table.png")
            .material("Solid Oak")
            .dimensions("160 x 50 x 85 cm")
            .weightKg(new BigDecimal("42.00"))
            .assemblyRequired(true)
            .warrantyMonths(18)
            .rating(new BigDecimal("4.78"))
            .isActive(true)
            .isNew(true)
            .build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-LAMP-BED-001")
            .name("Bedside Reading Lamp")
            .description("Lampu baca bedside dengan arm fleksibel dan dimmer sentuh. Cahaya hangat 2700K yang tidak mengganggu tidur, hemat energi dengan LED.")
            .price(new BigDecimal("1450000"))
            .stock(22)
            .category(kamarTidur)
            .collection(sanctuarySeries)
            .imageUrl("/product-lamp.png")
            .material("Brushed Brass + Linen Shade")
            .dimensions("20 x 35 x 55 cm")
            .weightKg(new BigDecimal("2.40"))
            .assemblyRequired(false)
            .warrantyMonths(12)
            .rating(new BigDecimal("4.82"))
            .isActive(true)
            .isNew(false)
            .build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-MIRROR-001")
            .name("Oval Floor Mirror")
            .description("Cermin lantai berbentuk oval dengan frame kayu walnut yang elegan. Ideal untuk sudut kamar tidur, memantulkan cahaya natural dan memperluas ruangan secara visual.")
            .price(new BigDecimal("4200000"))
            .stock(10)
            .category(kamarTidur)
            .collection(sanctuarySeries)
            .imageUrl("/product-rattan-wall.png")
            .material("Walnut Frame + Clear Glass")
            .dimensions("70 x 5 x 170 cm")
            .weightKg(new BigDecimal("14.00"))
            .assemblyRequired(false)
            .warrantyMonths(12)
            .rating(new BigDecimal("4.90"))
            .isActive(true)
            .isNew(true)
            .build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-VASE-BED-001")
            .name("Ceramic Bedside Diffuser")
            .description("Diffuser keramik handmade untuk aroma kamar tidur yang menenangkan. Ultrasonic mist, timer otomatis, dan LED ambient lembut.")
            .price(new BigDecimal("950000"))
            .stock(28)
            .category(kamarTidur)
            .collection(sanctuarySeries)
            .imageUrl("/product-ceramic-vase.png")
            .material("Stoneware Ceramic")
            .dimensions("14 x 14 x 22 cm")
            .weightKg(new BigDecimal("1.20"))
            .assemblyRequired(false)
            .warrantyMonths(6)
            .rating(new BigDecimal("4.75"))
            .isActive(true)
            .isNew(true)
            .build());

        // ============ RUANG MAKAN (Dining Room) ============
        inserted += saveIfMissing(Product.builder()
            .sku("MSN-DINING-001")
            .name("Oak Dining Table 6-Seater")
            .description("Meja makan kayu oak solid untuk 6 orang. Permukaan lebar dengan finish natural oil yang menonjolkan grain kayu, cocok untuk makan malam keluarga.")
            .price(new BigDecimal("14800000"))
            .stock(5)
            .category(ruangMakan)
            .collection(diningEdition)
            .imageUrl("/product-table.png")
            .material("Solid Oak")
            .dimensions("180 x 90 x 75 cm")
            .weightKg(new BigDecimal("55.00"))
            .assemblyRequired(true)
            .warrantyMonths(24)
            .rating(new BigDecimal("4.88"))
            .isActive(true)
            .isNew(true)
            .build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-DINING-CHAIR-001")
            .name("Linen Dining Chair (Set of 2)")
            .description("Set 2 kursi makan dengan dudukan linen premium dan kaki kayu oak. Didesain ergonomis untuk kenyamanan makan panjang, cocok dipadukan dengan meja kayu apapun.")
            .price(new BigDecimal("3600000"))
            .salePrice(new BigDecimal("3240000"))
            .stock(20)
            .category(ruangMakan)
            .collection(diningEdition)
            .imageUrl("/product-chair.png")
            .material("Oak + Belgian Linen")
            .dimensions("45 x 50 x 90 cm (per chair)")
            .weightKg(new BigDecimal("8.50"))
            .assemblyRequired(true)
            .warrantyMonths(12)
            .rating(new BigDecimal("4.80"))
            .isActive(true)
            .isNew(false)
            .build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-DINING-BENCH-001")
            .name("Wooden Dining Bench")
            .description("Bangku makan kayu solid untuk sisi panjang meja makan. Desain minimalis, kapasitas hingga 3 orang, mudah digeser di bawah meja saat tidak digunakan.")
            .price(new BigDecimal("4500000"))
            .stock(9)
            .category(ruangMakan)
            .collection(diningEdition)
            .imageUrl("/product-sofa.png")
            .material("Solid Teak")
            .dimensions("160 x 38 x 45 cm")
            .weightKg(new BigDecimal("16.00"))
            .assemblyRequired(false)
            .warrantyMonths(18)
            .rating(new BigDecimal("4.72"))
            .isActive(true)
            .isNew(false)
            .build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-DINING-LAMP-001")
            .name("Linear Pendant Light")
            .description("Lampu gantung linear untuk di atas meja makan. Panjang 120cm dengan 5 titik lampu LED dimmable, menciptakan pencahayaan sempurna saat jamuan makan.")
            .price(new BigDecimal("3400000"))
            .stock(12)
            .category(ruangMakan)
            .collection(diningEdition)
            .imageUrl("/product-lamp.png")
            .material("Brushed Brass + Opal Glass")
            .dimensions("120 x 10 x 25 cm")
            .weightKg(new BigDecimal("4.80"))
            .assemblyRequired(true)
            .warrantyMonths(24)
            .rating(new BigDecimal("4.85"))
            .isActive(true)
            .isNew(true)
            .build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-DINING-TABLE-002")
            .name("Marble Round Dining Table")
            .description("Meja makan bundar dengan permukaan marble Carrara asli, kaki kayu oak berbentuk pedestal. Muat 4 orang, cocok untuk ruang makan modern yang elegan.")
            .price(new BigDecimal("16900000"))
            .stock(4)
            .category(ruangMakan)
            .collection(diningEdition)
            .imageUrl("/product-marble-table.png")
            .material("Carrara Marble + Solid Oak")
            .dimensions("120 x 120 x 75 cm")
            .weightKg(new BigDecimal("78.00"))
            .assemblyRequired(true)
            .warrantyMonths(24)
            .rating(new BigDecimal("4.92"))
            .isActive(true)
            .isNew(true)
            .build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-DINING-VASE-001")
            .name("Centerpiece Ceramic Bowl")
            .description("Mangkuk keramik artistik sebagai centerpiece meja makan. Bentuk organik handmade, cocok untuk buah segar atau sebagai dekorasi statement.")
            .price(new BigDecimal("1180000"))
            .stock(18)
            .category(ruangMakan)
            .collection(diningEdition)
            .imageUrl("/product-ceramic-vase.png")
            .material("Stoneware Ceramic")
            .dimensions("38 x 38 x 14 cm")
            .weightKg(new BigDecimal("2.80"))
            .assemblyRequired(false)
            .warrantyMonths(6)
            .rating(new BigDecimal("4.78"))
            .isActive(true)
            .isNew(false)
            .build());

        if (inserted > 0) {
            log.info("✅ Seeded {} additional bedroom/dining products", inserted);
        } else {
            log.info("Bedroom/dining products already present, skipping additional seeds");
        }
    }

    /**
     * Ensures that newer categories (e.g. "Penyimpanan") exist in the database.
     * Idempotent — only creates categories whose name does not already exist.
     * Safe to run on fresh and existing databases.
     */
    private void ensureExtraCategories() {
        ensureCategory("Penyimpanan", "/product-bookshelf.svg", "Rak, lemari, konsol TV, dan solusi penyimpanan rumah");
    }

    private void ensureCategory(String name, String imageUrl, String description) {
        if (categoryRepository.findByName(name).isPresent()) return;
        categoryRepository.save(Category.builder()
            .name(name)
            .imageUrl(imageUrl)
            .description(description)
            .isActive(true)
            .build());
        log.info("✅ Added missing category: {}", name);
    }

    /**
     * Seeds a large batch of additional products for the catalog filter categories
     * on the koleksi page: Kursi (& Sofa), Meja, Lampu, Dekorasi, Penyimpanan.
     * Idempotent by SKU — safe to run repeatedly.
     */
    private void seedExtraCatalogProducts() {
        Category kursi      = categoryRepository.findByName("Kursi").orElse(null);
        Category meja       = categoryRepository.findByName("Meja").orElse(null);
        Category lampu      = categoryRepository.findByName("Lampu").orElse(null);
        Category dekorasi   = categoryRepository.findByName("Dekorasi").orElse(null);
        Category penyimpanan = categoryRepository.findByName("Penyimpanan").orElse(null);

        Collection livingCollection    = collectionRepository.findByName("Living Collection 2025").orElse(null);
        Collection sanctuarySeries     = collectionRepository.findByName("Sanctuary Series").orElse(null);
        Collection diningEdition       = collectionRepository.findByName("Dining Edition").orElse(null);

        if (kursi == null || meja == null || lampu == null || dekorasi == null || penyimpanan == null) {
            log.warn("⚠️  Cannot seed extra catalog products: required categories missing");
            return;
        }

        int inserted = 0;

        // ============ KURSI & SOFA ============
        inserted += saveIfMissing(Product.builder()
            .sku("MSN-SOFA-002")
            .name("Linen 3-Seater Sofa")
            .description("Sofa 3 dudukan dengan upholstery linen premium warna oat milk. Frame kayu oak solid, cushion high-density foam dengan feather topper untuk kenyamanan maksimal.")
            .price(new BigDecimal("14800000"))
            .salePrice(new BigDecimal("13320000"))
            .stock(6)
            .category(kursi)
            .collection(livingCollection)
            .imageUrl("/product-sofa.png")
            .material("Belgian Linen + Solid Oak")
            .dimensions("220 x 95 x 85 cm")
            .weightKg(new BigDecimal("58.00"))
            .assemblyRequired(false)
            .warrantyMonths(24)
            .rating(new BigDecimal("4.85"))
            .isActive(true).isNew(true).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-SOFA-003")
            .name("Modular Sectional Sofa")
            .description("Sofa modular L-shape dengan chaise kanan. Dapat direkonfigurasi sesuai kebutuhan ruangan, cocok untuk keluarga besar atau area entertainment.")
            .price(new BigDecimal("24500000"))
            .stock(3)
            .category(kursi)
            .collection(livingCollection)
            .imageUrl("/product-sectional.svg")
            .material("Performance Fabric + Engineered Hardwood")
            .dimensions("290 x 180 x 85 cm")
            .weightKg(new BigDecimal("92.00"))
            .assemblyRequired(true)
            .warrantyMonths(24)
            .rating(new BigDecimal("4.92"))
            .isActive(true).isNew(true).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-CHAIR-003")
            .name("Leather Lounge Chair")
            .description("Kursi lounge dengan kulit full-grain premium dan frame kayu walnut. Ergonomis dengan sudut recline alami, cocok untuk ruang baca.")
            .price(new BigDecimal("9800000"))
            .stock(8)
            .category(kursi)
            .collection(livingCollection)
            .imageUrl("/product-lounge-chair.svg")
            .material("Full-Grain Leather + Walnut")
            .dimensions("80 x 85 x 95 cm")
            .weightKg(new BigDecimal("22.00"))
            .assemblyRequired(false)
            .warrantyMonths(24)
            .rating(new BigDecimal("4.88"))
            .isActive(true).isNew(false).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-CHAIR-004")
            .name("Scandinavian Oak Dining Chair")
            .description("Kursi makan dengan desain Scandinavian klasik. Kaki oak solid dengan dudukan anyaman rotan alami, cocok untuk meja makan apapun.")
            .price(new BigDecimal("2200000"))
            .stock(24)
            .category(kursi)
            .collection(diningEdition)
            .imageUrl("/product-chair.png")
            .material("Solid Oak + Natural Rattan")
            .dimensions("45 x 50 x 85 cm")
            .weightKg(new BigDecimal("5.50"))
            .assemblyRequired(true)
            .warrantyMonths(12)
            .rating(new BigDecimal("4.72"))
            .isActive(true).isNew(false).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-CHAIR-005")
            .name("Wingback Accent Chair")
            .description("Wingback chair klasik dengan upholstery velvet sage green. Frame kayu solid, cocok sebagai statement piece di ruang tamu atau kamar tidur utama.")
            .price(new BigDecimal("6800000"))
            .stock(10)
            .category(kursi)
            .collection(livingCollection)
            .imageUrl("/product-velvet-chair.png")
            .material("Velvet + Solid Beech")
            .dimensions("75 x 80 x 110 cm")
            .weightKg(new BigDecimal("18.00"))
            .assemblyRequired(false)
            .warrantyMonths(18)
            .rating(new BigDecimal("4.80"))
            .isActive(true).isNew(true).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-STOOL-001")
            .name("Velvet Bar Stool (Set of 2)")
            .description("Set 2 bar stool dengan dudukan velvet mustard dan kaki logam brushed brass. Tinggi counter-height 65cm, cocok untuk kitchen island atau bar.")
            .price(new BigDecimal("3200000"))
            .stock(15)
            .category(kursi)
            .imageUrl("/product-bar-stool.svg")
            .material("Velvet + Brushed Brass")
            .dimensions("40 x 40 x 75 cm (per stool)")
            .weightKg(new BigDecimal("6.00"))
            .assemblyRequired(true)
            .warrantyMonths(12)
            .rating(new BigDecimal("4.78"))
            .isActive(true).isNew(true).build());

        // ============ MEJA ============
        inserted += saveIfMissing(Product.builder()
            .sku("MSN-TABLE-003")
            .name("Mid-Century Round Coffee Table")
            .description("Meja kopi bundar gaya mid-century dengan top kayu walnut dan kaki tapered brass. Diameter 90cm, ideal untuk ruang tamu compact.")
            .price(new BigDecimal("5400000"))
            .stock(11)
            .category(meja)
            .collection(livingCollection)
            .imageUrl("/product-table.png")
            .material("Walnut + Brushed Brass")
            .dimensions("90 x 90 x 42 cm")
            .weightKg(new BigDecimal("18.00"))
            .assemblyRequired(true)
            .warrantyMonths(18)
            .rating(new BigDecimal("4.82"))
            .isActive(true).isNew(true).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-TABLE-004")
            .name("Nesting Side Tables Set (3 pcs)")
            .description("Set 3 meja samping nesting dengan top kayu oak dan rangka logam matte black. Dapat disimpan bertumpuk untuk menghemat ruang.")
            .price(new BigDecimal("3900000"))
            .salePrice(new BigDecimal("3510000"))
            .stock(14)
            .category(meja)
            .imageUrl("/product-nesting-tables.svg")
            .material("Oak + Matte Black Steel")
            .dimensions("45/55/65 cm diameter")
            .weightKg(new BigDecimal("12.00"))
            .assemblyRequired(true)
            .warrantyMonths(12)
            .rating(new BigDecimal("4.76"))
            .isActive(true).isNew(false).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-DESK-001")
            .name("Writing Desk with Drawer")
            .description("Meja kerja dengan 1 laci penyimpanan dan cable management. Top kayu oak dengan finish matte, cocok untuk home office minimalis.")
            .price(new BigDecimal("4900000"))
            .stock(12)
            .category(meja)
            .imageUrl("/product-writing-desk.svg")
            .material("Solid Oak + Steel Frame")
            .dimensions("140 x 60 x 75 cm")
            .weightKg(new BigDecimal("24.00"))
            .assemblyRequired(true)
            .warrantyMonths(18)
            .rating(new BigDecimal("4.84"))
            .isActive(true).isNew(true).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-DESK-002")
            .name("Executive Walnut Desk")
            .description("Meja kerja executive dengan 3 laci samping, top kulit PU embossed, dan finish walnut mewah. Dirancang untuk kantor profesional atau CEO room.")
            .price(new BigDecimal("12800000"))
            .stock(5)
            .category(meja)
            .imageUrl("/product-writing-desk.svg")
            .material("American Walnut + PU Leather")
            .dimensions("180 x 80 x 76 cm")
            .weightKg(new BigDecimal("48.00"))
            .assemblyRequired(true)
            .warrantyMonths(24)
            .rating(new BigDecimal("4.90"))
            .isActive(true).isNew(false).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-TABLE-006")
            .name("Glass Console Table")
            .description("Console table dengan top kaca tempered dan rangka brass tipis. Elegan untuk entryway atau di belakang sofa, tidak memakan banyak ruang visual.")
            .price(new BigDecimal("4200000"))
            .stock(8)
            .category(meja)
            .imageUrl("/product-console-table.svg")
            .material("Tempered Glass + Brass")
            .dimensions("120 x 35 x 80 cm")
            .weightKg(new BigDecimal("14.00"))
            .assemblyRequired(true)
            .warrantyMonths(12)
            .rating(new BigDecimal("4.70"))
            .isActive(true).isNew(false).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-TABLE-007")
            .name("Travertine Round Dining Table")
            .description("Meja makan bundar dengan top travertine asli Italia dan pedestal oak solid. Muat 4-6 orang, statement piece untuk ruang makan mewah.")
            .price(new BigDecimal("19500000"))
            .stock(3)
            .category(meja)
            .collection(diningEdition)
            .imageUrl("/product-marble-table.png")
            .material("Italian Travertine + Solid Oak")
            .dimensions("130 x 130 x 76 cm")
            .weightKg(new BigDecimal("85.00"))
            .assemblyRequired(true)
            .warrantyMonths(24)
            .rating(new BigDecimal("4.95"))
            .isActive(true).isNew(true).build());

        // ============ LAMPU ============
        inserted += saveIfMissing(Product.builder()
            .sku("MSN-LAMP-002")
            .name("Brass Arc Floor Lamp")
            .description("Lampu lantai dengan arc brass panjang 180cm dan shade linen drum. Memberikan pencahayaan overhead tanpa perlu instalasi plafon.")
            .price(new BigDecimal("3800000"))
            .stock(10)
            .category(lampu)
            .imageUrl("/product-floor-lamp.svg")
            .material("Brushed Brass + Linen Shade")
            .dimensions("180 x 60 x 200 cm")
            .weightKg(new BigDecimal("16.00"))
            .assemblyRequired(true)
            .warrantyMonths(18)
            .rating(new BigDecimal("4.82"))
            .isActive(true).isNew(true).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-LAMP-003")
            .name("Ceramic Table Lamp")
            .description("Lampu meja dengan body keramik handmade warna sage dan shade linen oat. Cahaya hangat 2700K, cocok untuk side table atau nightstand.")
            .price(new BigDecimal("1650000"))
            .stock(20)
            .category(lampu)
            .imageUrl("/product-lamp.png")
            .material("Stoneware Ceramic + Linen")
            .dimensions("32 x 32 x 55 cm")
            .weightKg(new BigDecimal("3.80"))
            .assemblyRequired(false)
            .warrantyMonths(12)
            .rating(new BigDecimal("4.78"))
            .isActive(true).isNew(false).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-LAMP-004")
            .name("Paper Lantern Pendant")
            .description("Lampu gantung dengan shade washi paper handmade dari Jepang. Memberikan cahaya diffuse yang lembut, cocok untuk gaya japandi.")
            .price(new BigDecimal("1950000"))
            .stock(16)
            .category(lampu)
            .imageUrl("/product-lamp.png")
            .material("Washi Paper + Bamboo Frame")
            .dimensions("45 x 45 x 50 cm")
            .weightKg(new BigDecimal("1.20"))
            .assemblyRequired(true)
            .warrantyMonths(6)
            .rating(new BigDecimal("4.72"))
            .isActive(true).isNew(true).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-LAMP-005")
            .name("Wall Sconce Pair")
            .description("Sepasang lampu dinding dengan swing arm brass dan shade linen. Hardwired, cocok untuk di samping tempat tidur atau di kedua sisi cermin.")
            .price(new BigDecimal("2400000"))
            .stock(12)
            .category(lampu)
            .collection(sanctuarySeries)
            .imageUrl("/product-lamp.png")
            .material("Brushed Brass + Linen")
            .dimensions("30 x 50 x 25 cm (per sconce)")
            .weightKg(new BigDecimal("2.40"))
            .assemblyRequired(true)
            .warrantyMonths(12)
            .rating(new BigDecimal("4.80"))
            .isActive(true).isNew(false).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-LAMP-006")
            .name("Crystal Chandelier 8-Arm")
            .description("Chandelier mewah dengan 8 arm brass dan kristal cut murano. Ideal untuk foyer atau dining room dengan langit-langit tinggi.")
            .price(new BigDecimal("18500000"))
            .stock(3)
            .category(lampu)
            .collection(diningEdition)
            .imageUrl("/product-chandelier.svg")
            .material("Brass + Murano Crystal")
            .dimensions("90 x 90 x 85 cm")
            .weightKg(new BigDecimal("22.00"))
            .assemblyRequired(true)
            .warrantyMonths(24)
            .rating(new BigDecimal("4.94"))
            .isActive(true).isNew(true).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-LAMP-007")
            .name("Minimalist LED Desk Lamp")
            .description("Lampu meja LED dengan dimmer sentuh, 3 tingkat warna (warm/neutral/cool), dan USB charging port. Body aluminium matte black.")
            .price(new BigDecimal("890000"))
            .salePrice(new BigDecimal("801000"))
            .stock(28)
            .category(lampu)
            .imageUrl("/product-lamp.png")
            .material("Anodized Aluminum")
            .dimensions("15 x 15 x 48 cm")
            .weightKg(new BigDecimal("1.80"))
            .assemblyRequired(false)
            .warrantyMonths(24)
            .rating(new BigDecimal("4.76"))
            .isActive(true).isNew(false).build());

        // ============ DEKORASI ============
        inserted += saveIfMissing(Product.builder()
            .sku("MSN-VASE-002")
            .name("Terracotta Decorative Vase")
            .description("Vase terracotta handmade dengan tekstur organik dan finish unglazed alami. Cocok untuk dried pampas atau sebagai statement object.")
            .price(new BigDecimal("1180000"))
            .stock(22)
            .category(dekorasi)
            .imageUrl("/product-ceramic-vase.png")
            .material("Terracotta Clay")
            .dimensions("28 x 28 x 45 cm")
            .weightKg(new BigDecimal("3.60"))
            .assemblyRequired(false)
            .warrantyMonths(6)
            .rating(new BigDecimal("4.82"))
            .isActive(true).isNew(true).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-MIRROR-002")
            .name("Round Brass Wall Mirror")
            .description("Cermin dinding bundar dengan frame brass tebal 4cm. Diameter 80cm, memantulkan cahaya natural dan memberikan kesan lapang.")
            .price(new BigDecimal("2800000"))
            .stock(14)
            .category(dekorasi)
            .imageUrl("/product-round-mirror.svg")
            .material("Brass Frame + Clear Glass")
            .dimensions("80 x 4 x 80 cm")
            .weightKg(new BigDecimal("6.80"))
            .assemblyRequired(false)
            .warrantyMonths(12)
            .rating(new BigDecimal("4.88"))
            .isActive(true).isNew(true).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-FRAME-001")
            .name("Gallery Frame Set (5 pcs)")
            .description("Set 5 frame oak solid dengan ukuran variatif untuk gallery wall. Termasuk hanging template dan hardware, tanpa print.")
            .price(new BigDecimal("1450000"))
            .stock(30)
            .category(dekorasi)
            .imageUrl("/product-gallery-frames.svg")
            .material("Solid Oak + Acrylic Glass")
            .dimensions("A4, A5, 20x25, 30x40, 40x50 cm")
            .weightKg(new BigDecimal("4.20"))
            .assemblyRequired(false)
            .warrantyMonths(6)
            .rating(new BigDecimal("4.74"))
            .isActive(true).isNew(false).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-CANDLE-001")
            .name("Scented Soy Candle Trio")
            .description("Set 3 lilin aromatherapy soy wax dengan aroma Sandalwood, Fig & Olive, dan Cedarwood. Burn time 40 jam per lilin, glass jar reusable.")
            .price(new BigDecimal("680000"))
            .stock(45)
            .category(dekorasi)
            .imageUrl("/product-candles.svg")
            .material("Soy Wax + Glass Jar")
            .dimensions("8 x 8 x 10 cm (per candle)")
            .weightKg(new BigDecimal("1.50"))
            .assemblyRequired(false)
            .warrantyMonths(0)
            .rating(new BigDecimal("4.86"))
            .isActive(true).isNew(true).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-THROW-001")
            .name("Merino Wool Throw Blanket")
            .description("Selimut throw dari 100% merino wool Selandia Baru. Warna caramel hangat, cocok untuk sofa atau ujung tempat tidur.")
            .price(new BigDecimal("980000"))
            .stock(26)
            .category(dekorasi)
            .imageUrl("/product-throw.svg")
            .material("100% Merino Wool")
            .dimensions("130 x 180 cm")
            .weightKg(new BigDecimal("1.40"))
            .assemblyRequired(false)
            .warrantyMonths(0)
            .rating(new BigDecimal("4.90"))
            .isActive(true).isNew(false).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-RUG-001")
            .name("Handwoven Jute Rug")
            .description("Karpet jute handwoven dengan tekstur alami. Warna natural honey, menahan suara dan memberikan kehangatan pada lantai keramik atau kayu.")
            .price(new BigDecimal("2600000"))
            .stock(12)
            .category(dekorasi)
            .imageUrl("/product-jute-rug.svg")
            .material("100% Natural Jute")
            .dimensions("200 x 300 cm")
            .weightKg(new BigDecimal("9.80"))
            .assemblyRequired(false)
            .warrantyMonths(6)
            .rating(new BigDecimal("4.78"))
            .isActive(true).isNew(true).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-PANEL-002")
            .name("Abstract Canvas Wall Art")
            .description("Lukisan kanvas abstrak tone earthy dengan frame oak minimalis. Ukuran besar 100x140cm, cocok sebagai focal point ruang tamu.")
            .price(new BigDecimal("3200000"))
            .stock(8)
            .category(dekorasi)
            .imageUrl("/product-rattan-wall.png")
            .material("Canvas + Oak Frame")
            .dimensions("100 x 4 x 140 cm")
            .weightKg(new BigDecimal("6.00"))
            .assemblyRequired(false)
            .warrantyMonths(6)
            .rating(new BigDecimal("4.80"))
            .isActive(true).isNew(false).build());

        // ============ PENYIMPANAN ============
        inserted += saveIfMissing(Product.builder()
            .sku("MSN-STORE-001")
            .name("Oak Bookshelf 5-Tier")
            .description("Rak buku 5 tingkat dari kayu oak solid dengan back panel terbuka. Kapasitas besar untuk buku, tanaman, dan dekorasi.")
            .price(new BigDecimal("6800000"))
            .stock(8)
            .category(penyimpanan)
            .imageUrl("/product-bookshelf.svg")
            .material("Solid Oak")
            .dimensions("90 x 35 x 200 cm")
            .weightKg(new BigDecimal("38.00"))
            .assemblyRequired(true)
            .warrantyMonths(18)
            .rating(new BigDecimal("4.84"))
            .isActive(true).isNew(true).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-STORE-002")
            .name("Walnut Sideboard 4-Door")
            .description("Sideboard mewah dengan 4 pintu push-to-open dan handle brass. Interior dengan 2 adjustable shelves, cocok untuk ruang makan atau entryway.")
            .price(new BigDecimal("11500000"))
            .salePrice(new BigDecimal("10350000"))
            .stock(5)
            .category(penyimpanan)
            .imageUrl("/product-sideboard.svg")
            .material("American Walnut + Brass")
            .dimensions("180 x 45 x 85 cm")
            .weightKg(new BigDecimal("62.00"))
            .assemblyRequired(true)
            .warrantyMonths(24)
            .rating(new BigDecimal("4.90"))
            .isActive(true).isNew(true).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-STORE-003")
            .name("Rattan Storage Basket (Set of 3)")
            .description("Set 3 keranjang penyimpanan rattan handwoven dengan ukuran S/M/L. Lining fabric untuk melindungi isi, cocok untuk linen, mainan, atau laundry.")
            .price(new BigDecimal("890000"))
            .stock(32)
            .category(penyimpanan)
            .imageUrl("/product-storage-basket.svg")
            .material("Natural Rattan + Cotton Lining")
            .dimensions("25/35/45 cm diameter")
            .weightKg(new BigDecimal("2.80"))
            .assemblyRequired(false)
            .warrantyMonths(6)
            .rating(new BigDecimal("4.76"))
            .isActive(true).isNew(false).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-STORE-004")
            .name("Linen Wardrobe 3-Door")
            .description("Lemari pakaian 3 pintu dengan hanging rod, 4 adjustable shelves, dan 2 laci bawah. Finish linen-textured melamine yang tahan gores.")
            .price(new BigDecimal("9800000"))
            .stock(6)
            .category(penyimpanan)
            .collection(sanctuarySeries)
            .imageUrl("/product-wardrobe.svg")
            .material("Engineered Wood + Linen-Textured Laminate")
            .dimensions("160 x 60 x 210 cm")
            .weightKg(new BigDecimal("85.00"))
            .assemblyRequired(true)
            .warrantyMonths(24)
            .rating(new BigDecimal("4.82"))
            .isActive(true).isNew(true).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-STORE-005")
            .name("Industrial TV Console")
            .description("TV console industrial dengan top kayu reclaimed dan frame steel matte black. Cable management terintegrasi, muat TV hingga 75 inch.")
            .price(new BigDecimal("5800000"))
            .stock(10)
            .category(penyimpanan)
            .collection(livingCollection)
            .imageUrl("/product-console-tv.svg")
            .material("Reclaimed Pine + Powder-Coated Steel")
            .dimensions("180 x 40 x 55 cm")
            .weightKg(new BigDecimal("32.00"))
            .assemblyRequired(true)
            .warrantyMonths(18)
            .rating(new BigDecimal("4.80"))
            .isActive(true).isNew(false).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-STORE-006")
            .name("Slim Shoe Cabinet")
            .description("Rak sepatu slim dengan 4 tilt-out compartment, muat hingga 24 pasang sepatu. Kedalaman hanya 28cm — cocok untuk entryway sempit.")
            .price(new BigDecimal("2900000"))
            .stock(14)
            .category(penyimpanan)
            .imageUrl("/product-shoe-cabinet.svg")
            .material("Engineered Wood + Metal Hinges")
            .dimensions("80 x 28 x 180 cm")
            .weightKg(new BigDecimal("26.00"))
            .assemblyRequired(true)
            .warrantyMonths(12)
            .rating(new BigDecimal("4.72"))
            .isActive(true).isNew(true).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-STORE-007")
            .name("Open Cube Storage (9-Cube)")
            .description("Rak cube 3x3 terbuka dari kayu oak untuk display dan storage fleksibel. Kompatibel dengan fabric bin standar, cocok untuk ruang anak atau home office.")
            .price(new BigDecimal("4200000"))
            .stock(12)
            .category(penyimpanan)
            .imageUrl("/product-bookshelf.svg")
            .material("Solid Oak")
            .dimensions("115 x 35 x 115 cm")
            .weightKg(new BigDecimal("28.00"))
            .assemblyRequired(true)
            .warrantyMonths(18)
            .rating(new BigDecimal("4.74"))
            .isActive(true).isNew(false).build());

        inserted += saveIfMissing(Product.builder()
            .sku("MSN-STORE-008")
            .name("Bedroom Chest of Drawers")
            .description("Lemari laci kamar tidur dengan 5 laci besar dan slow-close rails. Cocok untuk penyimpanan pakaian dalam dan linen, finish oak natural.")
            .price(new BigDecimal("6400000"))
            .stock(9)
            .category(penyimpanan)
            .collection(sanctuarySeries)
            .imageUrl("/product-table.png")
            .material("Solid Oak")
            .dimensions("100 x 50 x 110 cm")
            .weightKg(new BigDecimal("42.00"))
            .assemblyRequired(true)
            .warrantyMonths(18)
            .rating(new BigDecimal("4.82"))
            .isActive(true).isNew(false).build());

        if (inserted > 0) {
            log.info("✅ Seeded {} additional catalog products (Kursi/Meja/Lampu/Dekorasi/Penyimpanan)", inserted);
        } else {
            log.info("Extra catalog products already present, skipping");
        }
    }

    /**
     * Saves a product only if its SKU does not already exist.
     * Returns 1 if inserted, 0 if skipped.
     */
    private int saveIfMissing(Product product) {
        if (productRepository.existsBySku(product.getSku())) {
            return 0;
        }
        productRepository.save(product);
        return 1;
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

    private void seedVouchers() {
        if (voucherRepository.count() > 0) {
            backfillVoucherPoints();
            log.info("Vouchers already exist, skipping...");
            return;
        }

        voucherRepository.save(Voucher.builder()
            .code("MAISON10")
            .type(VoucherType.PERSEN)
            .value(new BigDecimal("10"))
            .pointsCost(1000)
            .minOrderValue(new BigDecimal("500000"))
            .usageLimit(500)
            .validUntil(LocalDate.now().plusYears(1))
            .isActive(true)
            .build());

        voucherRepository.save(Voucher.builder()
            .code("WELCOME15")
            .type(VoucherType.PERSEN)
            .value(new BigDecimal("15"))
            .pointsCost(1500)
            .minOrderValue(BigDecimal.ZERO)
            .usageLimit(500)
            .validUntil(LocalDate.now().plusYears(1))
            .isActive(true)
            .build());

        voucherRepository.save(Voucher.builder()
            .code("ONGKIR50")
            .type(VoucherType.NOMINAL)
            .value(new BigDecimal("50000"))
            .pointsCost(500)
            .minOrderValue(new BigDecimal("300000"))
            .usageLimit(300)
            .validUntil(LocalDate.now().plusYears(1))
            .isActive(true)
            .build());

        log.info("✅ Seeded {} vouchers", voucherRepository.count());
    }

    private void backfillVoucherPoints() {
        setVoucherPoints("MAISON10", 1000);
        setVoucherPoints("WELCOME15", 1500);
        setVoucherPoints("ONGKIR50", 500);
    }

    private void setVoucherPoints(String code, int pointsCost) {
        voucherRepository.findByCodeIgnoreCase(code).ifPresent(voucher -> {
            if (voucher.getPointsCost() == null || voucher.getPointsCost() == 0) {
                voucher.setPointsCost(pointsCost);
                voucherRepository.save(voucher);
            }
        });
    }

    private void seedSettings() {
        appSettingService.putIfAbsent("store", "storeName", "Maison Furniture");
        appSettingService.putIfAbsent("store", "storeEmail", "hello@maison.id");
        appSettingService.putIfAbsent("store", "storePhone", "+6281234567890");
        appSettingService.putIfAbsent("store", "storeAddress", "Jl. Dago No. 123, Bandung, Jawa Barat");
        appSettingService.putIfAbsent("store", "shippingFreeMin", 5000000);
        appSettingService.putIfAbsent("store", "shippingCost", 150000);
        appSettingService.putIfAbsent("store", "taxRate", new BigDecimal("0.11"));
        appSettingService.putIfAbsent("store", "whatsappNumber", "+6281234567890");
        appSettingService.putIfAbsent("store", "instagramUrl", "https://instagram.com/maison");
        appSettingService.putIfAbsent("store", "facebookUrl", "https://facebook.com/maison");
        appSettingService.putIfAbsent("store", "tokopediaUrl", "https://tokopedia.com/maison");
        appSettingService.putIfAbsent("store", "shopeeUrl", "https://shopee.com/maison");

        appSettingService.putIfAbsent("rewards", "pointsPerRupiah", 1);
        appSettingService.putIfAbsent("rewards", "pointsToRupiahRatio", 10);
        appSettingService.putIfAbsent("rewards", "welcomePoints", 100);
        appSettingService.putIfAbsent("rewards", "birthdayPoints", 500);
        appSettingService.putIfAbsent("rewards", "referralPoints", 200);
    }
}
