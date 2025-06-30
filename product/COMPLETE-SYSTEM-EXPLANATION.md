# 🏪 POS System - Complete System Explanation

## 📖 Overview

This document explains **everything** about the Spring Boot POS (Point of Sale) system - from how API calls work to how data flows through different layers, and all the technical details in between.

---

## 🏗️ System Architecture

### The Big Picture

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENT (Postman/Browser)                 │
├─────────────────────────────────────────────────────────────┤
│                    API LAYER (Controllers)                  │
│              • CategoryController                           │
│              • ProductController                            │
│              • SaleController                               │
├─────────────────────────────────────────────────────────────┤
│                BUSINESS LOGIC LAYER (Services)              │
│              • CategoryService                              │
│              • ProductService                               │
│              • SaleService                                  │
├─────────────────────────────────────────────────────────────┤
│                 DATA ACCESS LAYER (Repositories)            │
│              • CategoryRepository                           │
│              • ProductRepository                            │
│              • SaleRepository                               │
│              • SaleItemRepository                           │
├─────────────────────────────────────────────────────────────┤
│                    DATABASE LAYER (MySQL)                   │
│              • categories table                             │
│              • products table                               │
│              • sales table                                  │
│              • sale_items table                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔄 How Data Flows Through the System

### Example 1: Creating a Product

#### Step-by-Step Flow:

**1. Client Request**

```http
POST http://localhost:8080/api/v1/products
Content-Type: application/json

{
    "name": "iPhone 15",
    "description": "Latest iPhone model",
    "price": 999.99,
    "stockQuantity": 50,
    "categoryId": 1,
    "barcode": "1234567890123"
}
```

**2. Controller Layer (ProductController)**

```java
@PostMapping
public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductDto productDto) {
    // @Valid triggers validation
    ProductDto createdProduct = productService.createProduct(productDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
}
```

**3. Service Layer (ProductService)**

```java
@Transactional
public ProductDto createProduct(ProductDto productDto) {
    // Business Rule 1: Check barcode uniqueness
    if (productRepository.existsByBarcode(productDto.getBarcode())) {
        throw new RuntimeException("Barcode already exists");
    }

    // Business Rule 2: Validate category exists
    Category category = categoryRepository.findById(productDto.getCategoryId())
        .orElseThrow(() -> new ResourceNotFoundException("Category", "id", productDto.getCategoryId()));

    // Convert DTO to Entity
    Product product = new Product();
    product.setName(productDto.getName());
    product.setPrice(productDto.getPrice());
    product.setCategory(category);

    // Save to database
    Product savedProduct = productRepository.save(product);

    // Convert back to DTO
    return convertToDto(savedProduct);
}
```

**4. Repository Layer (ProductRepository)**

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByBarcode(String barcode);
}
```

**5. Database Operation**

```sql
-- Check if barcode exists
SELECT COUNT(*) FROM products WHERE barcode = '1234567890123';

-- Insert new product
INSERT INTO products (name, description, price, stock_quantity, category_id, barcode)
VALUES ('iPhone 15', 'Latest iPhone model', 999.99, 50, 1, '1234567890123');
```

**6. Response Flow**

```
Product Entity → ProductService.convertToDto() → ProductDto → Controller → JSON Response
```

---

### Example 2: Creating a Sale (Complex Transaction)

#### Step-by-Step Flow:

**1. Client Request**

```http
POST http://localhost:8080/api/v1/sales
Content-Type: application/json

{
    "customerName": "John Doe",
    "paymentMethod": "CASH",
    "saleItems": [
        {
            "productId": 1,
            "quantity": 2,
            "unitPrice": 999.99,
            "totalPrice": 1999.98
        }
    ]
}
```

**2. Service Layer (Complex Business Logic)**

```java
@Transactional
public SaleDto createSale(CreateSaleRequest request) {
    // @Transactional begins here

    // Create the sale
    Sale sale = new Sale();
    sale.setCustomerName(request.getCustomerName());
    sale.setPaymentMethod(request.getPaymentMethod());
    sale.setStatus(Sale.SaleStatus.COMPLETED);

    Sale savedSale = saleRepository.save(sale);

    BigDecimal totalAmount = BigDecimal.ZERO;

    // Process each sale item
    for (SaleItemDto itemDto : request.getSaleItems()) {
        // Get product
        Product product = productRepository.findById(itemDto.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemDto.getProductId()));

        // Check stock availability
        if (product.getStockQuantity() < itemDto.getQuantity()) {
            throw new InsufficientStockException(product.getName(), itemDto.getQuantity(), product.getStockQuantity());
        }

        // Create sale item
        SaleItem saleItem = new SaleItem();
        saleItem.setSale(savedSale);
        saleItem.setProduct(product);
        saleItem.setQuantity(itemDto.getQuantity());
        saleItem.setUnitPrice(product.getPrice());
        saleItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())));

        saleItemRepository.save(saleItem);

        // Update stock (critical business rule)
        productService.updateStock(product.getId(), -itemDto.getQuantity());

        totalAmount = totalAmount.add(saleItem.getTotalPrice());
    }

    // Update sale total
    savedSale.setTotalAmount(totalAmount);
    saleRepository.save(savedSale);

    // @Transactional commits here (or rolls back if any error)
    return convertToDto(savedSale);
}
```

**3. Database Operations (All in One Transaction)**

```sql
-- Begin transaction
START TRANSACTION;

-- Create sale
INSERT INTO sales (customer_name, payment_method, status, total_amount)
VALUES ('John Doe', 'CASH', 'COMPLETED', 0);

-- Create sale item
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, total_price)
VALUES (1, 1, 2, 999.99, 1999.98);

-- Update product stock
UPDATE products SET stock_quantity = stock_quantity - 2 WHERE id = 1;

-- Update sale total
UPDATE sales SET total_amount = 1999.98 WHERE id = 1;

-- Commit transaction
COMMIT;
```

---

## 🗄️ Database Design Explained

### Table Relationships

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  categories │    │   products  │    │    sales    │
├─────────────┤    ├─────────────┤    ├─────────────┤
│ id (PK)     │◄───┤ category_id │    │ id (PK)     │
│ name        │    │ id (PK)     │    │ customer_   │
│ description │    │ name        │    │   name      │
│ created_at  │    │ price       │    │ total_amount│
│ updated_at  │    │ stock_qty   │    │ payment_    │
└─────────────┘    │ barcode     │    │   method    │
                   │ created_at  │    │ status      │
                   │ updated_at  │    │ created_at  │
                   └─────────────┘    │ updated_at  │
                                      └─────────────┘
                                              │
                                              │
                                              ▼
                                      ┌─────────────┐
                                      │ sale_items  │
                                      ├─────────────┤
                                      │ id (PK)     │
                                      │ sale_id     │
                                      │ product_id  │
                                      │ quantity    │
                                      │ unit_price  │
                                      │ total_price │
                                      └─────────────┘
```

### Key Design Decisions

1. **Foreign Key Relationships**:

   - `products.category_id` → `categories.id` (Many-to-One)
   - `sale_items.sale_id` → `sales.id` (Many-to-One)
   - `sale_items.product_id` → `products.id` (Many-to-One)

2. **Cascade Rules**:

   - Delete category → Set product category to NULL
   - Delete sale → Delete all sale items (CASCADE)
   - Delete product → Prevent if used in sales (RESTRICT)

3. **Data Validation**:
   - Price must be > 0
   - Stock quantity must be ≥ 0
   - Sale quantity must be > 0

---

## 🌐 API Layer Deep Dive

### Controller Responsibilities

#### 1. Request Handling

```java
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    // GET /api/v1/products
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        List<ProductDto> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    // GET /api/v1/products/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        ProductDto product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    // POST /api/v1/products
    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductDto productDto) {
        ProductDto createdProduct = productService.createProduct(productDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }
}
```

#### 2. HTTP Status Codes

- `200 OK`: Successful GET, PUT operations
- `201 Created`: Successful POST operations
- `204 No Content`: Successful DELETE operations
- `400 Bad Request`: Validation errors
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server errors

#### 3. Request Validation

```java
public class ProductDto {
    @NotBlank(message = "Product name is required")
    private String name;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity must be non-negative")
    private Integer stockQuantity;
}
```

---

## ⚙️ Business Logic Layer Deep Dive

### Service Layer Responsibilities

#### 1. Business Rules Implementation

```java
@Service
@Transactional
public class ProductService {

    public ProductDto createProduct(ProductDto productDto) {
        // Business Rule 1: Barcode uniqueness
        if (productDto.getBarcode() != null &&
            productRepository.existsByBarcode(productDto.getBarcode())) {
            throw new RuntimeException("Barcode already exists");
        }

        // Business Rule 2: Category validation
        Category category = null;
        if (productDto.getCategoryId() != null) {
            category = categoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", productDto.getCategoryId()));
        }

        // Business Rule 3: Price validation
        if (productDto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Price must be greater than 0");
        }

        // Convert and save
        Product product = convertToEntity(productDto, category);
        Product savedProduct = productRepository.save(product);
        return convertToDto(savedProduct);
    }
}
```

#### 2. Transaction Management

```java
@Transactional
public SaleDto createSale(CreateSaleRequest request) {
    // All operations in this method are in one transaction

    // If any operation fails, ALL changes are rolled back
    // This ensures data consistency

    Sale sale = createSaleEntity(request);
    Sale savedSale = saleRepository.save(sale);

    for (SaleItemDto itemDto : request.getSaleItems()) {
        processSaleItem(savedSale, itemDto);
    }

    return convertToDto(savedSale);
}
```

#### 3. Data Conversion

```java
private ProductDto convertToDto(Product product) {
    ProductDto dto = new ProductDto();
    dto.setId(product.getId());
    dto.setName(product.getName());
    dto.setDescription(product.getDescription());
    dto.setPrice(product.getPrice());
    dto.setStockQuantity(product.getStockQuantity());
    dto.setBarcode(product.getBarcode());

    if (product.getCategory() != null) {
        dto.setCategoryId(product.getCategory().getId());
        dto.setCategoryName(product.getCategory().getName());
    }

    return dto;
}
```

---

## 💾 Data Access Layer Deep Dive

### Repository Pattern

#### 1. Basic CRUD Operations

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // Inherited methods from JpaRepository:
    // - save(Product product)
    // - findById(Long id)
    // - findAll()
    // - deleteById(Long id)
    // - existsById(Long id)
}
```

#### 2. Custom Query Methods

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Method name query
    Optional<Product> findByBarcode(String barcode);

    // Method name query with conditions
    List<Product> findByCategoryId(Long categoryId);

    // Method name query with LIKE
    List<Product> findByNameContainingIgnoreCase(String name);

    // Method name query with exists
    boolean existsByBarcode(String barcode);
}
```

#### 3. Custom JPQL Queries

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Custom JPQL query
    @Query("SELECT p FROM Product p WHERE p.stockQuantity <= :threshold")
    List<Product> findLowStockProducts(@Param("threshold") Integer threshold);

    // Custom JPQL query with JOIN
    @Query("SELECT p FROM Product p JOIN p.category c WHERE c.name = :categoryName")
    List<Product> findProductsByCategoryName(@Param("categoryName") String categoryName);
}
```

#### 4. Generated SQL Examples

```sql
-- findByBarcode("1234567890123")
SELECT * FROM products WHERE barcode = '1234567890123';

-- findByCategoryId(1)
SELECT * FROM products WHERE category_id = 1;

-- findByNameContainingIgnoreCase("phone")
SELECT * FROM products WHERE LOWER(name) LIKE LOWER('%phone%');

-- findLowStockProducts(10)
SELECT * FROM products WHERE stock_quantity <= 10;
```

---

## 📊 Data Models & DTOs Deep Dive

### Entity vs DTO Pattern

#### Entity (Database Model)

```java
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity must be non-negative")
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "barcode", unique = true, length = 50)
    private String barcode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

#### DTO (API Model)

```java
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private Long categoryId;        // Only ID, not full object
    private String categoryName;    // Denormalized for convenience
    private String barcode;
    // Note: No timestamps in DTO (internal data)
}
```

### Why Use DTOs?

1. **Security**: Hide sensitive internal data
2. **Performance**: Transfer only needed data
3. **API Stability**: Internal changes don't affect API
4. **Flexibility**: Different DTOs for different use cases

---

## ⚠️ Error Handling & Validation Deep Dive

### Validation Layers

#### 1. Input Validation (Controller Level)

```java
@PostMapping
public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductDto productDto) {
    // @Valid triggers validation annotations in ProductDto
    ProductDto createdProduct = productService.createProduct(productDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
}
```

#### 2. Business Rule Validation (Service Level)

```java
public ProductDto createProduct(ProductDto productDto) {
    // Business rule validation
    if (productDto.getBarcode() != null &&
        productRepository.existsByBarcode(productDto.getBarcode())) {
        throw new RuntimeException("Barcode already exists");
    }

    // More business rules...
}
```

#### 3. Global Exception Handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getDescription(false)
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation Error",
            "Validation failed: " + errors.toString(),
            request.getDescription(false)
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
```

### Error Response Format

```json
{
  "timestamp": "2024-01-01T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: {name=Product name is required, price=Price must be greater than 0}",
  "path": "/api/v1/products"
}
```

---

## 💰 Transaction Management Deep Dive

### Transaction Annotations

#### Service Level Transactions

```java
@Service
@Transactional  // All methods in this service are transactional
public class SaleService {

    @Transactional  // Can override at method level
    public SaleDto createSale(CreateSaleRequest request) {
        // All database operations in this method are in one transaction
        // If any operation fails, all changes are rolled back
    }
}
```

### Transaction Scenarios

#### 1. Successful Sale Transaction

```sql
BEGIN TRANSACTION;
  INSERT INTO sales (customer_name, payment_method, total_amount)
  VALUES ('John Doe', 'CASH', 0);

  INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, total_price)
  VALUES (1, 1, 2, 999.99, 1999.98);

  UPDATE products SET stock_quantity = stock_quantity - 2 WHERE id = 1;

  UPDATE sales SET total_amount = 1999.98 WHERE id = 1;
COMMIT TRANSACTION;
```

#### 2. Failed Sale Transaction (Insufficient Stock)

```sql
BEGIN TRANSACTION;
  INSERT INTO sales (customer_name, payment_method, total_amount)
  VALUES ('John Doe', 'CASH', 0);

  -- Stock check fails, throws exception
  -- All changes are automatically rolled back
ROLLBACK TRANSACTION;
```

### Transaction Properties

- **Atomicity**: All operations succeed or all fail
- **Consistency**: Database remains in valid state
- **Isolation**: Transactions don't interfere with each other
- **Durability**: Committed changes are permanent

---

## 🔒 Security Configuration Deep Dive

### Spring Security Setup

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)  // Disable CSRF for API
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/v1/**").permitAll()  // Allow all API access
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
```

### Security Features

1. **CSRF Protection**: Disabled for API endpoints
2. **CORS**: Configured for cross-origin requests
3. **Authentication**: Basic security setup (can be extended with JWT)

---

## 🧪 Testing Strategy

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProduct_WithValidData_ShouldReturnProduct() {
        // Given
        ProductDto productDto = new ProductDto();
        productDto.setName("Test Product");
        productDto.setPrice(new BigDecimal("99.99"));

        Category category = new Category();
        category.setId(1L);
        category.setName("Electronics");

        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setPrice(new BigDecimal("99.99"));
        product.setCategory(category);

        // When
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductDto result = productService.createProduct(productDto);

        // Then
        assertThat(result.getName()).isEqualTo("Test Product");
        assertThat(result.getPrice()).isEqualTo(new BigDecimal("99.99"));
    }
}
```

### Integration Tests

```java
@SpringBootTest
@AutoConfigureTestDatabase
class ProductControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createProduct_ShouldReturnCreatedProduct() {
        // Given
        ProductDto productDto = new ProductDto();
        productDto.setName("Integration Test Product");
        productDto.setPrice(new BigDecimal("199.99"));

        // When
        ResponseEntity<ProductDto> response = restTemplate.postForEntity(
            "/api/v1/products",
            productDto,
            ProductDto.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getName()).isEqualTo("Integration Test Product");
    }
}
```

---

## 🚀 Deployment & Configuration

### Application Properties

```properties
# Application Configuration
spring.application.name=pos-system
server.port=8080

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/pos_system?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Data Initialization
spring.jpa.defer-datasource-initialization=true
spring.sql.init.mode=always

# Logging Configuration
logging.level.com.pos=DEBUG
logging.level.org.springframework.security=DEBUG

# Jackson Configuration
spring.jackson.default-property-inclusion=non_null
spring.jackson.serialization.write-dates-as-timestamps=false
```

### Production Configuration

```properties
# Production Database
spring.datasource.url=jdbc:mysql://prod-server:3306/pos_system
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# JPA Configuration
spring.jpa.hibernate.ddl-auto=validate  # Don't auto-create tables
spring.jpa.show-sql=false  # Disable SQL logging

# Logging
logging.level.com.pos=INFO
logging.level.org.springframework.security=WARN
```

---

## 📈 Performance Considerations

### Database Optimization

1. **Indexes**: Add indexes on frequently queried columns
2. **Lazy Loading**: Use `FetchType.LAZY` for relationships
3. **Pagination**: Implement pagination for large datasets

### Caching Strategy

```java
@Cacheable("products")
public List<ProductDto> getAllProducts() {
    return productRepository.findAll().stream()
        .map(this::convertToDto)
        .collect(Collectors.toList());
}
```

### Connection Pooling

```properties
# HikariCP Configuration
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

---

## 🔧 Monitoring & Logging

### Logging Configuration

```properties
# Application Logging
logging.level.com.pos=DEBUG
logging.level.org.springframework.web=INFO
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

### Health Checks

```java
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("POS System is running");
    }
}
```

---

## 🚀 Future Enhancements

### Potential Improvements

1. **Authentication & Authorization**: JWT-based authentication
2. **Audit Logging**: Track all changes to data
3. **Reporting**: Sales reports and analytics
4. **Inventory Alerts**: Low stock notifications
5. **Multi-tenancy**: Support multiple stores
6. **API Documentation**: Swagger/OpenAPI integration
7. **Caching**: Redis for better performance
8. **Message Queue**: Async processing for large operations

---

## 📚 Additional Resources

### Useful Commands

```bash
# Build the project
mvn clean install

# Run tests
mvn test

# Run application
mvn spring-boot:run

# Generate JAR file
mvn package
```

### Database Commands

```sql
-- Check table structure
DESCRIBE products;

-- View sample data
SELECT * FROM products LIMIT 5;

-- Check foreign key relationships
SELECT
    TABLE_NAME,
    COLUMN_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_SCHEMA = 'pos_system';
```

---

## 🎯 Summary

This POS system demonstrates:

1. **Layered Architecture**: Clear separation of concerns
2. **RESTful APIs**: Standard HTTP methods and status codes
3. **Data Validation**: Multiple layers of validation
4. **Transaction Management**: ACID-compliant operations
5. **Error Handling**: Consistent error responses
6. **Security**: Basic security configuration
7. **Testing**: Unit and integration test strategies
8. **Documentation**: Comprehensive API documentation

The system is production-ready and follows Spring Boot best practices!
