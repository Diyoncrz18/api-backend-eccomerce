# Maison Interior — Backend API

> E-commerce backend untuk toko furnitur & dekorasi rumah dengan **AI chatbot RAG** (Retrieval-Augmented Generation) bertenaga Gemini.

---

## Daftar Isi

1. [Ringkasan & Tech Stack](#1-ringkasan--tech-stack)
2. [Quick Start](#2-quick-start)
3. [Fitur 1 — Integrasi MySQL](#fitur-1--integrasi-mysql)
4. [Fitur 2 — Integrasi Gemini AI](#fitur-2--integrasi-gemini-ai)
5. [Fitur 3 — AI Recommendation Engine](#fitur-3--ai-recommendation-engine)
6. [Fitur 4 — RAG Pipeline](#fitur-4--rag-pipeline)
7. [Fitur 5a — Chat DB-Grounded (Produk)](#fitur-5a--chat-db-grounded-produk)
8. [Fitur 5b — Chat Per-User Data](#fitur-5b--chat-per-user-data)
9. [Fitur 6a — Dashboard Analytics](#fitur-6a--dashboard-analytics)
10. [Referensi API Endpoints](#referensi-api-endpoints)
11. [Verifikasi & Demo](#verifikasi--demo)

---

## 1. Ringkasan & Tech Stack

**Stack**

| Layer | Teknologi |
|---|---|
| Bahasa | Java 21 |
| Framework | Spring Boot 3.5.11 |
| Database | MySQL 8 + HikariCP + JPA/Hibernate |
| Security | Spring Security + JWT (jjwt 0.11.5) |
| AI / LLM | Google Gemini API (`gemini-2.5-flash-lite` + `gemini-embedding-001`) |
| HTTP Client | Spring WebClient (reactive, untuk Gemini) |
| Build | Maven 3 + Lombok |

**Arsitektur**

```
┌─────────────────────────────────────────────────────────────┐
│  Frontend Next.js (final-backend-eccomerce)                 │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTP/JSON
┌───────────────────────────▼─────────────────────────────────┐
│  Spring Boot REST API (port 8081)                           │
│  ┌─────────────┬──────────────┬────────────┬─────────────┐  │
│  │ Controllers │ Services     │ AI/RAG     │ Security    │  │
│  │ (REST API)  │ (Business)   │ (Gemini)   │ (JWT)       │  │
│  └─────────────┴──────────────┴────────────┴─────────────┘  │
└───────┬───────────────────────────────────┬─────────────────┘
        │ JPA                               │ HTTPS
┌───────▼─────────┐                ┌────────▼──────────────┐
│  MySQL 8        │                │  Google Gemini API    │
│  - 9 entities   │                │  - generateContent    │
│  - 53 produk    │                │  - embedContent       │
└─────────────────┘                └───────────────────────┘
```

---

## 2. Quick Start

```pwsh
# 1. Setup .env (copy dari template)
cp .env.example .env
# Edit .env: set DB_PASSWORD, GEMINI_API_KEY (dari https://aistudio.google.com/app/apikey)

# 2. Pastikan MySQL jalan & database sudah dibuat
mysql -u root -p -e "CREATE DATABASE backend_final"

# 3. Run
.\mvnw.cmd spring-boot:run
# atau
.\start-backend.ps1   # auto-cleanup port + load .env

# 4. Verify
curl http://localhost:8081/api/v1/chat/health
```

Default port: **8081**. API base: `http://localhost:8081/api/v1`.

---

## Fitur 1 — Integrasi MySQL

> Bagaimana data persisted di MySQL via Spring Data JPA + Hibernate.

### Step 1: Konfigurasi Connection

File: `src/main/resources/application.properties`

```properties
spring.datasource.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:backend_final}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

Credentials dibaca dari `.env` (gitignored) lewat:
```properties
spring.config.import=optional:file:.env[.properties]
```

### Step 2: Connection Pool (HikariCP)

```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=300000
```

HikariCP me-pool 5–10 koneksi sehingga request HTTP tidak buka/tutup koneksi DB tiap kali.

### Step 3: ORM Mapping (JPA + Hibernate)

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
```

`ddl-auto=update` artinya schema MySQL **otomatis sinkron** dari Java entity tiap startup — tidak perlu jalankan SQL manual.

### Step 4: Entity → Repository → Service → Controller

Pola berlapis untuk tiap domain object:

```java
// 1. Entity (model/Product.java)
@Entity
@Table(name = "products")
public class Product {
    @Id @GeneratedValue private Long id;
    @Column(nullable = false) private String name;
    private Integer stock;
    private BigDecimal price;
    @ManyToOne private Category category;
    // ...
}

// 2. Repository (repository/ProductRepository.java)
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByIsActiveTrue(Pageable p);
    @Query("SELECT p FROM Product p WHERE p.stock > 0 ...")
    Page<Product> findDashboardRecommendations(Pageable p);
}

// 3. Service (service/ProductService.java) — @Transactional logic
@Service @Transactional(readOnly = true)
public class ProductService { ... }

// 4. Controller (controller/ProductController.java) — REST API
@RestController @RequestMapping("${api.prefix}/products")
public class ProductController { ... }
```

### Step 5: Seed Data Otomatis

File: `src/main/java/com/backend/demo/config/SeedDataRunner.java`

Implements `CommandLineRunner` → dijalankan otomatis tiap startup. Insert:
- 7 kategori (Kursi, Meja, Lampu, Dekorasi, Penyimpanan, dst)
- 3 koleksi (Sanctuary Series, Dining Edition, dll)
- **53 produk** dengan stok, harga, dimensi, material, deskripsi
- 1 admin user (`admin@maison.com` / `admin123`)
- 3 voucher seed (MAISON10, WELCOME15, ONGKIR50)
- App settings default (currency, shipping fee, dll)

### Entities (9 total)

| Entity | Purpose |
|---|---|
| `User` | Akun + roles + tier (REGULAR/GOLD/PLATINUM/ADMIN) + reward points |
| `Product` | Katalog produk + stok + sale price + dimensi |
| `Category` | Kategori produk (Kursi, Meja, dll) |
| `Collection` | Koleksi tema (Sanctuary, Dining Edition) |
| `Order` + `OrderItem` | Pesanan + line items + status |
| `CartItem` | Keranjang per user |
| `Review` | Rating produk |
| `Voucher` | Kode diskon (PERSEN/NOMINAL) |
| `AppSetting` | Key/value config global |
| `ProductEmbedding` | Vector cache untuk RAG (Phase 2) |

### Verifikasi

```pwsh
curl http://localhost:8081/api/v1/products?limit=5
# Dapat 53 produk total. JSON includes: id, name, stock, price, salePrice, material, dimensions, category, collection.
```

---

## Fitur 2 — Integrasi Gemini AI

> Cara Spring Boot panggil Google Gemini API untuk LLM + embeddings.

### Step 1: API Key Setup

1. Daftar gratis di https://aistudio.google.com/app/apikey
2. Simpan di `.env`:
   ```
   GEMINI_API_KEY=AIzaSy...
   ```
3. `application.properties` baca:
   ```properties
   rag.gemini.api-key=${GEMINI_API_KEY:}
   rag.gemini.model=${GEMINI_MODEL:gemini-2.5-flash-lite}
   ```

### Step 2: WebClient Setup

File: `src/main/java/com/backend/demo/service/ai/GeminiClient.java`

Pakai Spring WebClient (reactive, non-blocking) dari `spring-boot-starter-webflux`:

```java
@PostConstruct
void init() {
    this.webClient = WebClient.builder()
        .baseUrl(baseUrl)  // https://generativelanguage.googleapis.com/v1beta
        .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
        .build();
}
```

### Step 3: Build & Send Request

Endpoint pattern:
```
POST {baseUrl}/models/{model}:generateContent?key={apiKey}
```

Request body (JSON):
```json
{
  "contents": [{ "parts": [{ "text": "<full prompt>" }] }],
  "generationConfig": {
    "temperature": 0.7,
    "maxOutputTokens": 500,
    "topP": 0.95,
    "topK": 40,
    "thinkingConfig": { "thinkingBudget": 0 }
  }
}
```

> `thinkingBudget=0` mematikan "thinking" mode Gemini 2.5 sehingga semua output tokens dipakai untuk jawaban final (lebih cepat untuk chat).

### Step 4: Retry Policy + Timeout

Production-grade error handling:

```java
.timeout(Duration.ofMillis(timeoutMs))  // 20 detik
.retryWhen(Retry.backoff(retryMaxAttempts - 1, Duration.ofMillis(retryInitialBackoffMs))
    .filter(GeminiClient::isTransient))
```

Retry exponential backoff (3 attempts, 500ms → 1s → 2s) untuk:
- HTTP 408 Timeout
- HTTP 425 Too Early
- HTTP 429 Rate Limit
- HTTP 5xx (500/502/503/504)
- Network timeout / connection reset

Non-transient errors (400, 401, 403) gagal langsung — tidak ada gunanya retry.

### Step 5: Graceful Fallback

Kalau Gemini benar-benar down atau API key tidak set:

```java
if (!geminiClient.isConfigured()) {
    return heuristicResponse(userMessage, retrieved);  // pakai keyword retriever saja
}

try {
    llm = geminiClient.generate(prompt);
} catch (GeminiException e) {
    return heuristicResponse(userMessage, retrieved);  // fallback dengan flag success=false
}
```

→ **Chat tidak pernah crash**. User selalu dapat jawaban (mungkin static jika Gemini down).

### Dua Endpoint Gemini

| Endpoint | Model | Pemakaian |
|---|---|---|
| `generateContent` | `gemini-2.5-flash-lite` | Chat answer (~1000 RPD free) |
| `embedContent` | `gemini-embedding-001` | Vector embedding untuk semantic search (768 dim) |

File client kedua: `service/ai/EmbeddingClient.java` (struktur mirip GeminiClient).

---

## Fitur 3 — AI Recommendation Engine

> Cara sistem memilih produk yang relevan untuk direkomendasikan.

### Strategi: Hybrid Retrieval (Semantic + Keyword)

Dua retriever bekerja paralel, hasilnya di-fusion dengan bobot.

```
                    User Query
                        │
            ┌───────────┴───────────┐
            ▼                       ▼
  ┌─────────────────┐    ┌─────────────────┐
  │ SemanticRetriever│    │ ProductRetriever│
  │ (cosine 65%)    │    │ (keyword 35%)   │
  └─────────────────┘    └─────────────────┘
            │                       │
            └───────────┬───────────┘
                        ▼
            ┌─────────────────────┐
            │  HybridRetriever    │
            │  rank fusion → top-K│
            └─────────────────────┘
                        │
                        ▼
                  6 produk relevan
```

### Step 1: Semantic Retrieval

File: `src/main/java/com/backend/demo/service/ai/SemanticRetriever.java`

1. **Embed query**: kirim teks user ke Gemini Embedding API → dapat vector 768 dim
2. **L2-normalize** vector (supaya cosine = dot product)
3. **Cosine similarity** vs setiap product vector di cache (53 vektor)
4. Return top-K dengan skor

```java
float[] queryVec = embeddingService.embedQueryNormalized(query);
for (Map.Entry<Long, float[]> e : embeddingService.getAllVectors().entrySet()) {
    float sim = EmbeddingService.cosine(queryVec, e.getValue());
    // ... sort, pick top-K
}
```

### Step 2: Keyword Retrieval

File: `src/main/java/com/backend/demo/service/ai/ProductRetriever.java`

1. **Normalize query**: lowercase, hapus punctuation, hapus stopword bahasa Indonesia
2. **Detect category hint**: "sofa" → kategori Kursi, "lampu" → Lampu, dst
3. **Detect price ceiling**: "5 juta" → maxPrice = 5_000_000, "500rb" → 500_000
4. **Detect style hint**: "minimalis" → "japandi", "skandinavia" → "natural"
5. **Score per produk**:
   - +10 jika nama match keyword
   - +5 jika kategori match
   - +3 jika material match
   - −∞ kalau price > maxPrice
   - +1 untuk rating bonus
6. Return top-K sorted by score, tie-break by rating desc

### Step 3: Rank Fusion

File: `src/main/java/com/backend/demo/service/ai/HybridRetriever.java`

Final score:
```
final = 0.65 × cosine_normalized + 0.35 × reciprocal_keyword_rank
```

- `cosine_normalized = (cosine + 1) / 2` → range [0, 1]
- `reciprocal_keyword_rank = 1 / (1 + rank)` → 1.0, 0.63, 0.5, 0.4, ...

Bobot bisa diatur via `application.properties`:
```properties
rag.retrieval.semantic-weight=0.65
rag.retrieval.keyword-weight=0.35
```

### Step 4: Anti-Hallucination

Setelah LLM kasih `PRODUCT_IDS: [5, 12]`, kita validate:

```java
// File: service/ai/RagService.java
List<Product> finalProducts = new ArrayList<>();
for (Long id : idsFromLLM) {
    Product p = byId.get(id);  // byId = produk hasil retrieval
    if (p != null) finalProducts.add(p);
}
```

→ ID yang **tidak ada** di pool retrieval di-drop. LLM tidak bisa "ngarang" produk.

### Tipe Rekomendasi yang Di-support

| Tipe Query | Contoh | Mekanisme |
|---|---|---|
| Intent + kategori | "sofa untuk ruang tamu" | semantic + keyword "sofa" → kursi |
| Budget | "lampu di bawah 2 juta" | keyword parser maxPrice=2000000 |
| Style | "gaya skandinavia kayu natural" | semantic embedding match |
| Use-case | "wadah pajang buku" | semantic understanding |
| Multi-fitur | "sofa empuk untuk nonton film" | hybrid (semantic dominant) |

---

## Fitur 4 — RAG Pipeline

> Bagaimana retrieval-augmented generation menghasilkan jawaban grounded.

### Overview: 5-Step RAG Flow

```
Step 1: INDEXING (one-time, di startup)
        53 produk → embed via Gemini → simpan ke product_embeddings + cache memory

Step 2: RETRIEVAL (per query)
        User query → embed → cosine vs cache → top-K=6 produk

Step 3: AUGMENTATION (build prompt)
        System rules + catalog block + history + user message → 1 prompt panjang

Step 4: GENERATION (LLM call)
        Kirim prompt ke Gemini → text response dengan marker

Step 5: PARSING (post-process)
        Extract INTENT, PRODUCT_IDS, validate IDs, strip markers, resolve ke DTO
```

### Step 1: Bootstrap Embedding (Indexing)

File: `src/main/java/com/backend/demo/service/ai/EmbeddingBootstrap.java`

Dijalankan otomatis di `@PostConstruct`:

1. Load semua produk active dari DB
2. Untuk tiap produk, build "document text":
   ```
   Produk: Linen 3-Seater Sofa.
   Kategori: Kursi.
   Koleksi: Sanctuary Series.
   Material: Belgian Linen + Solid Oak.
   Dimensi: 220 x 95 x 85 cm.
   Deskripsi: Sofa 3-seater dengan...
   ```
3. Hitung **SHA-256 hash** dari document text → simpan di `product_embeddings.source_hash`
4. **Skip kalau hash tidak berubah** → tidak buang quota Gemini saat restart
5. Batch 25 produk → 1 call ke Gemini Embedding API
6. Simpan vector JSON di table `product_embeddings`
7. Load ke memory cache `Map<Long, float[]>` (L2-normalized)

> Result: cache size = 53 vektor × 768 dim float ≈ 160 KB di memory. Lookup O(N) tapi cepat untuk 53 items.

### Step 2: Retrieval (Query Time)

File: `src/main/java/com/backend/demo/service/ai/HybridRetriever.java`

Lihat [Fitur 3](#fitur-3--ai-recommendation-engine) untuk detail. Ringkasnya:
- Semantic retrieval (cosine) + keyword retrieval (score-based)
- Rank fusion → top-K = 6 produk

### Step 3: Augmentation (Prompt Building)

File: `src/main/java/com/backend/demo/service/ai/PromptBuilder.java`

Prompt assembled dari 4 blok:

```
1. SYSTEM INSTRUCTIONS — rules: pakai Bahasa Indonesia, format harga,
   marker output INTENT + PRODUCT_IDS, anti-halusinasi.

2. KATALOG PRODUK YANG TERSEDIA — 6 produk hasil retrieval dengan
   stok, harga, sale price, material, dimensi, deskripsi.

3. PERCAKAPAN SEBELUMNYA — max 6 message terakhir untuk multi-turn.

4. PESAN SAAT INI — query user → "Maison AI:" (LLM completes here).
```

Format produk individu (line per produk):
```
[id:5] Linen 3-Seater Sofa | Kursi | Rp 14.800.000 | Bahan: Belgian Linen+Oak | Stok: 6
   Desc: Sofa 3-seater Belgian Linen dengan frame Solid Oak...
```

### Step 4: Generation (LLM Call)

```
POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=...
```

Gemini balas, contoh output mentah:
```
Linen 3-Seater Sofa cocok untuk ruang tamu Anda dengan harga Rp 14.800.000.
Material Belgian Linen-nya lembut dan stoknya masih ada 6 unit.

INTENT: product_recommendation
PRODUCT_IDS: [5]
```

### Step 5: Parsing & Validation

File: `src/main/java/com/backend/demo/service/ai/RagService.java`

```java
List<Long> idsFromLLM = extractProductIds(rawText);     // regex \[([^\]]*)\]
String intent = extractIntent(rawText);                  // regex INTENT:\s*([a-z_]+)
String cleanText = stripMarkers(rawText);                // hapus baris marker

// Validate IDs against retrieved pool (anti-hallucination)
List<Product> finalProducts = new ArrayList<>();
for (Long id : idsFromLLM) {
    Product p = byId.get(id);
    if (p != null) finalProducts.add(p);
}

// Resolve ke ProductResponse DTO (full object dengan gambar, dll)
return ChatResponse.builder()
    .text(cleanText)
    .intent(intent)
    .productIds(...)
    .products(finalProducts.stream().map(ProductResponse::from).toList())
    .usage(usage)
    .success(true)
    .build();
```

User akhirnya dapat response JSON terstruktur:
```json
{
  "text": "Linen 3-Seater Sofa cocok untuk ruang tamu Anda...",
  "intent": "product_recommendation",
  "productIds": [5],
  "products": [{ "id": 5, "name": "Linen 3-Seater Sofa", "price": 14800000, ... }],
  "usage": { "promptTokens": 1240, "responseTokens": 87, "totalTokens": 1327 },
  "success": true
}
```

---

## Fitur 5a — Chat DB-Grounded (Produk)

> Chat tidak generic — semua jawaban tentang produk berasal dari DB real.

### Bukti: Stok, Harga, Material masuk ke prompt

File: `src/main/java/com/backend/demo/service/ai/PromptBuilder.java` — method `formatProduct()`

```java
sb.append("[id:").append(p.getId()).append("] ");
sb.append(p.getName());
if (p.getCategory() != null)  sb.append(" | ").append(p.getCategory().getName());
sb.append(" | ").append(formatRp(p.getPrice()));
if (p.getSalePrice() != null) sb.append(" (diskon ").append(formatRp(p.getSalePrice())).append(")");
if (p.getMaterial() != null)  sb.append(" | Bahan: ").append(p.getMaterial());
if (p.getStock() != null)     sb.append(" | Stok: ").append(p.getStock());
if (Boolean.TRUE.equals(p.getIsNew())) sb.append(" | BARU");
```

→ Setiap produk yang masuk prompt selalu disertakan **stok real-time** dari column `products.stock`.

### Walkthrough Lengkap: "stok produk Linen Sofa berapa?"

1. **Frontend kirim**:
   ```
   POST /api/v1/chat/query
   { "message": "stok Linen Sofa berapa?", "history": [] }
   ```

2. **Backend retrieval**:
   - `HybridRetriever` → semantic match "linen sofa" + keyword tokens
   - Ambil 6 produk teratas, termasuk `[id=5] Linen 3-Seater Sofa`

3. **Backend query DB** (otomatis lewat JPA):
   ```sql
   SELECT id, name, stock, price, sale_price, material, ...
   FROM products WHERE is_active = true ORDER BY id
   ```

4. **Build prompt** dengan stok real-time:
   ```
   === KATALOG PRODUK YANG TERSEDIA ===
   [id:5] Linen 3-Seater Sofa | Kursi | Rp 14.800.000 | Bahan: Belgian Linen+Oak | Stok: 6
   [id:21] Olive Linen Sofa   | Kursi | Rp 12.500.000 | Bahan: Linen+Engineered Wood | Stok: 7
   ...
   ```

5. **Kirim ke Gemini** → LLM jawab:
   > "Linen 3-Seater Sofa stoknya masih 6 unit. Material Belgian Linen-nya lembut..."

6. **Frontend tampilkan** + product card.

### Demo Hasil Test

| Query | Jawaban Maison AI | Verifikasi DB |
|---|---|---|
| `Stok Crystal Chandelier 8-Arm berapa?` | "3 unit yang tersedia" | `stock=3` ✓ |
| `Minimalist LED Desk Lamp ada diskon?` | "diskon menjadi Rp 801.000" | `salePrice=801000` ✓ |
| `Round Brass Wall Mirror bahannya?` | "frame brass dan clear glass" | material match ✓ |
| `Lampu di bawah 2 juta?` | 3 lampu (1.65jt, 890rb, 1.45jt) | semua < 2jt ✓ |

### Data yang Selalu Grounded ke DB

✅ Nama produk · Kategori · Koleksi · Harga normal · Sale price · Material · Dimensi · **Stok** · Status BARU · Deskripsi

---

## Fitur 5b — Chat Per-User Data

> Chat juga tahu data akun user (orders, vouchers, points) kalau user login.

### Arsitektur

```
User (login) ──[JWT in header]──► /chat/query
                                       │
                                       ▼
                              ┌─────────────────┐
                              │ ChatController  │
                              │ resolveEmail()  │
                              └────────┬────────┘
                                       │ email
                                       ▼
                              ┌─────────────────────┐
                              │ UserContextProvider │
                              │ buildFor(email)     │
                              └────────┬────────────┘
                                       │ UserContext snapshot
                                       ▼
                              ┌─────────────────────┐
                              │ PromptBuilder       │
                              │ blok DATA AKUN ANDA │
                              └─────────────────────┘
```

### Step 1: Optional Auth di /chat/query

File: `src/main/java/com/backend/demo/controller/ChatController.java`

`/chat/query` tetap **public** (tidak wajib login), tapi kalau JWT ada → di-parse otomatis oleh `AuthTokenFilter`.

```java
@PostMapping("/query")
public ResponseEntity<ChatResponse> query(@Valid @RequestBody ChatRequest request,
                                          Authentication authentication) {
    String email = resolveEmail(authentication);  // null kalau anonim
    ChatResponse response = ragService.query(request, email);
    return ResponseEntity.ok(response);
}

private static String resolveEmail(Authentication auth) {
    if (auth == null) return null;
    if (auth instanceof AnonymousAuthenticationToken) return null;
    if (!auth.isAuthenticated()) return null;
    return auth.getName();  // email dari JWT subject
}
```

### Step 2: Build User Context Snapshot

File: `src/main/java/com/backend/demo/service/ai/UserContextProvider.java`

```java
@Transactional(readOnly = true)
public Optional<UserContext> buildFor(String email) {
    if (email == null) return Optional.empty();
    try {
        return userRepository.findWithWishlistByEmail(email).map(this::snapshot);
    } catch (Exception e) {
        return Optional.empty();  // defensive: chat tidak crash
    }
}
```

`snapshot(user)` mengumpulkan **5 jenis data** dari DB:

| Data | Source | Query |
|---|---|---|
| Identitas + tier + points | `User` | `findWithWishlistByEmail()` |
| Order stats | `OrderRepository` | `countByUserId`, `countByUserIdAndStatusIn` |
| 3 pesanan terbaru | `OrderRepository` | `findByUserId(uid, top3 DESC by createdAt)` |
| Cart count | `CartItemRepository` | `countByUserId` |
| Wishlist count | `User.wishlist` | sudah ter-fetch via `@EntityGraph` |
| Active vouchers | `VoucherRepository` | `findByIsActiveTrueOrderByValidUntilAsc()` filter kadaluarsa |

### Step 3: Render ke Prompt

File: `PromptBuilder.formatUserContext()` menghasilkan blok:

```
=== DATA AKUN ANDA (USER YANG SEDANG LOGIN) ===
Nama: Admin Maison
Tier: ADMIN | Reward Points: 0 | Total Belanja: Rp 0

Statistik Pesanan:
- Total pesanan: 0
- Pesanan aktif (sedang diproses/dikirim): 0
- Pesanan selesai: 0

Cart aktif: 0 item
Wishlist: 0 item

Voucher tersedia (aktif & belum kadaluarsa):
- MAISON10: Diskon 10% (min belanja Rp 500.000) — berlaku sampai 24 Apr 2027
- WELCOME15: Diskon 15% (min belanja Rp 750.000) — berlaku sampai 24 Apr 2027
- ONGKIR50: Diskon Rp 50.000 (min belanja Rp 300.000) — berlaku sampai 24 Apr 2027
```

### Step 4: System Instructions Adaptif

System rule berubah berdasarkan login state:

**Logged-in mode**:
```
8. Untuk pertanyaan tentang akun ("pesanan saya", "voucher saya", "poin saya"),
   gunakan EKSKLUSIF data dari blok DATA AKUN ANDA. Jangan mengarang.
   INTENT untuk pertanyaan akun: account_info dengan PRODUCT_IDS: [].
```

**Anonymous mode**:
```
8. Jika user bertanya tentang data pribadi, arahkan dengan sopan untuk login.
   INTENT: fallback, PRODUCT_IDS: [].
```

### Demo Test (Verified)

```pwsh
# Login dulu
$login = Invoke-RestMethod -Uri http://localhost:8081/api/v1/auth/login `
  -Method Post `
  -Body (@{email='admin@maison.com'; password='admin123'} | ConvertTo-Json) `
  -ContentType 'application/json'
$token = $login.payload.accessToken

# Query authed
$body = @{ message='Voucher apa yang aktif untuk saya?'; history=@() } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:8081/api/v1/chat/query `
  -Method Post -Body $body `
  -Headers @{ Authorization = "Bearer $token"; 'Content-Type' = 'application/json' }
```

| Query | Mode | Intent | Jawaban Maison AI |
|---|---|---|---|
| Berapa pesanan saya? | authed | `account_info` | "Anda memiliki 0 pesanan aktif dan 0 pesanan selesai." |
| Voucher apa yang aktif? | authed | `account_info` | "MAISON10 (diskon 10% min belanja Rp 500.000), WELCOME15 (diskon 15%), ONGKIR50 (diskon Rp 50.000 min Rp 300.000)..." |
| Reward poin saya? | authed | `account_info` | "Saat ini Anda memiliki 0 Reward Points." |
| Sofa Linen ada stoknya? | authed | `product_recommendation` | "Linen 3-Seater Sofa Rp 13.320.000, Olive Linen Sofa Rp 12.500.000. Keduanya tersedia dalam stok yang cukup." |
| Berapa pesanan saya? | unauth | `fallback` | "Mohon maaf, untuk mengecek informasi pesanan Anda, silakan login terlebih dahulu..." |

### Rekap Intent

| Intent | Kapan dipakai |
|---|---|
| `greeting` | "halo", "hai" |
| `product_recommendation` | Pertanyaan tentang produk |
| `account_info` | Data pribadi (logged-in only) |
| `off_topic` | Di luar topik furnitur |
| `fallback` | Tidak masuk kategori lain / minta login |

---

## Fitur 6a — Dashboard Analytics

> KPI dashboard untuk user (personal) dan admin (toko).

### a. User Dashboard (`GET /api/v1/dashboard/me`)

File:
- `src/main/java/com/backend/demo/controller/DashboardController.java`
- `src/main/java/com/backend/demo/service/DashboardService.java`

Auth: required (JWT).

**Step-by-step**:
1. Extract email dari JWT → fetch `User` (with wishlist eager via `@EntityGraph`)
2. Hitung statistik pesanan via `OrderRepository.countByUserId`, `countByUserIdAndStatusIn(ACTIVE_STATUSES)`
3. Ambil 5 pesanan terbaru → map ke `OrderSummary`
4. Wishlist top-4 → `ProductSummary` (dengan flag `inWishlist=true`)
5. Recommendations 6 produk via `productRepository.findDashboardRecommendations(...)`
6. Hitung `pointsNext` (next tier threshold: 1000 untuk REGULAR→GOLD, 10000 untuk GOLD→PLATINUM)

**Response sample**:
```json
{
  "user": { "name": "Budi", "tier": "GOLD", "points": 2450, "pointsNext": 10000, "totalSpent": 18000000 },
  "stats": { "totalOrders": 5, "activeOrders": 2, "wishlistCount": 4, "rewardPoints": 2450 },
  "recentOrders": [ { "orderNumber": "ORD-2026-001", "status": "DIKIRIM", "total": 14800000, ... } ],
  "wishlist": [ /* 4 ProductSummary */ ],
  "recommendations": [ /* 6 ProductSummary */ ]
}
```

### b. Admin Dashboard (Frontend `/admin` page)

File: `final-backend-eccomerce/app/admin/page.tsx`

**Step-by-step**:
1. Frontend fetch 3 endpoint paralel:
   - `fetchAdminOrders(0, 100)` → list pesanan
   - `fetchAdminUsers(0, 100)` → list user
   - `fetchProducts(0, 100)` → list produk
2. **Compute KPI client-side**:
   ```typescript
   const totalRevenue = orders.filter(o => o.status !== 'dibatalkan')
                              .reduce((sum, o) => sum + o.total, 0);
   const pendingOrders = orders.filter(o => o.status === 'menunggu').length;
   const activeMembers = users.filter(u => u.isActive && !u.roles.includes('ROLE_ADMIN')).length;
   const lowStock = products.filter(p => p.stock <= 5).slice(0, 5);
   ```
3. **Build sales trend** chart (per minggu) via `buildSalesTrend(orders)`
4. **Tier distribution** chart (REGULAR/GOLD/PLATINUM) via `buildTierDistribution(activeMembers)`

**KPI yang ditampilkan**:
- 💰 Total Revenue (sum order non-cancelled)
- ⏳ Pending Orders (status="menunggu")
- 👥 Active Members (non-admin, isActive)
- 📦 Active Products + Low Stock list
- 📈 Sales Trend chart
- 🏆 Tier Distribution chart
- 🕐 Recent Orders (5 terbaru)

### Backend Aggregation Helper

File: `src/main/java/com/backend/demo/repository/OrderRepository.java`

```java
@Query("SELECT SUM(o.total) FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate AND o.status = 'SELESAI'")
BigDecimal getRevenueByDateRange(@Param("startDate") Instant startDate,
                                 @Param("endDate") Instant endDate);

@Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
long countByStatus(@Param("status") OrderStatus status);
```

File: `src/main/java/com/backend/demo/controller/AdminUserController.java`

Endpoint `/api/v1/admin/users/stats`:
```java
long totalUsers = userRepository.count();
long activeUsers = userRepository.countByIsActive(true);
// ... etc, return as Map<String, Object>
```

---

## Referensi API Endpoints

### Public (tanpa auth)

| Method | Endpoint | Deskripsi |
|---|---|---|
| POST | `/api/v1/auth/login` | Login → access token + refresh cookie |
| POST | `/api/v1/auth/register` | Register user baru |
| POST | `/api/v1/auth/refresh-token` | Tukar refresh cookie → access token baru |
| GET | `/api/v1/products` | List produk (paginated) |
| GET | `/api/v1/products/{id}` | Detail produk |
| GET | `/api/v1/categories` | List kategori |
| GET | `/api/v1/collections` | List koleksi |
| GET | `/api/v1/reviews/product/{id}` | List review per produk |
| POST | `/api/v1/vouchers/validate` | Cek voucher (code + min belanja) |
| **POST** | **`/api/v1/chat/query`** | **Chatbot RAG (optional auth untuk per-user mode)** |
| GET | `/api/v1/chat/health` | Health check chatbot |

### Authenticated (JWT required)

| Method | Endpoint | Deskripsi |
|---|---|---|
| GET | `/api/v1/dashboard/me` | User dashboard |
| GET/POST/PUT/DELETE | `/api/v1/cart` | CRUD cart |
| GET/POST | `/api/v1/orders` | List + place order |
| GET/POST/DELETE | `/api/v1/wishlist` | Wishlist |
| POST/DELETE | `/api/v1/reviews` | Post/delete review |
| GET/PUT | `/api/v1/users/me` | Profile |
| POST | `/api/v1/auth/logout` | Logout |

### Admin Only (`ROLE_ADMIN`)

| Method | Endpoint | Deskripsi |
|---|---|---|
| GET/POST/PUT/DELETE | `/api/v1/admin/users/**` | Manage users |
| GET/POST/PUT/DELETE | `/api/v1/admin/vouchers/**` | Manage vouchers |
| GET/PUT | `/api/v1/admin/settings/**` | App settings |
| GET/POST | `/api/v1/admin/rewards/**` | Reward management |
| POST | `/api/v1/chat/reindex` | Re-embed semua produk |

---

## Verifikasi & Demo

### Quick health check

```pwsh
curl http://localhost:8081/api/v1/chat/health
# Expected: { "status": "ok", "embeddingReady": true, "embeddingCacheSize": 53 }
```

### Demo chat produk (anonymous)

```pwsh
$body = @{ message = 'Stok Crystal Chandelier berapa?'; history = @() } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:8081/api/v1/chat/query `
  -Method Post -Body $body -ContentType 'application/json'
```

### Demo chat per-user (authenticated)

```pwsh
# 1. Login
$login = Invoke-RestMethod -Uri http://localhost:8081/api/v1/auth/login `
  -Method Post `
  -Body (@{email='admin@maison.com'; password='admin123'} | ConvertTo-Json) `
  -ContentType 'application/json'
$token = $login.payload.accessToken

# 2. Query dengan token
$body = @{ message = 'Voucher apa saja yang aktif untuk saya?'; history = @() } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:8081/api/v1/chat/query `
  -Method Post -Body $body `
  -Headers @{ Authorization = "Bearer $token"; 'Content-Type' = 'application/json' }
```

### Test embedding bootstrap (admin only)

```pwsh
Invoke-RestMethod -Uri http://localhost:8081/api/v1/chat/reindex `
  -Method Post -Headers @{ Authorization = "Bearer $token" }
# Expected: { "totalProducts": 53, "embedded": 0, "skipped": 53, "elapsed": "..." }
# (skipped 53 = semua sudah ter-embed dari sesi sebelumnya, tidak buang quota)
```

---

## Status Implementasi

| # | Fitur | Status | Bukti |
|---|---|---|---|
| 1 | MySQL integration | ✅ | application.properties + JPA + 9 entities + 53 seed |
| 2 | Gemini API integration | ✅ | GeminiClient + EmbeddingClient + retry + fallback |
| 3 | AI Recommendation | ✅ | HybridRetriever (semantic 65% + keyword 35%) |
| 4 | RAG pipeline | ✅ | 5-step: index → retrieve → augment → generate → parse |
| 5a | Chat DB-grounded (produk) | ✅ | PromptBuilder.formatProduct injects stock/price/material |
| 5b | Chat per-user data | ✅ | UserContextProvider + DATA AKUN ANDA block |
| 6a | Dashboard analytics | ✅ | User dashboard + admin dashboard (revenue/orders/low stock/charts) |

**Skor**: 7/7 fitur utama implemented. Semua diverifikasi end-to-end dengan curl test.

---

## Lisensi & Kontribusi

Project ini dibuat untuk tugas akhir kelas Backend. Repo:
- Backend: https://github.com/Diyoncrz18/api-backend-eccomerce
- Frontend: https://github.com/Diyoncrz18/Final-Backend-Eccomerce
