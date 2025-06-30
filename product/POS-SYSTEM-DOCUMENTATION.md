# POS System - Complete Technical Documentation

## 📋 Table of Contents

1. [System Architecture Overview](#system-architecture-overview)
2. [Database Design & Relationships](#database-design--relationships)
3. [API Layer (Controllers)](#api-layer-controllers)
4. [Business Logic Layer (Services)](#business-logic-layer-services)
5. [Data Access Layer (Repositories)](#data-access-layer-repositories)
6. [Data Models & DTOs](#data-models--dtos)
7. [Data Flow Examples](#data-flow-examples)
8. [Error Handling & Validation](#error-handling--validation)
9. [Transaction Management](#transaction-management)
10. [Security Configuration](#security-configuration)
11. [Testing & Deployment](#testing--deployment)

---

## 🏗️ System Architecture Overview

### Layered Architecture Pattern

```
┌─────────────────────────────────────┐
│           API Layer                 │  ← Controllers (REST endpoints)
├─────────────────────────────────────┤
│        Business Logic Layer         │  ← Services (Business rules)
├─────────────────────────────────────┤
│        Data Access Layer            │  ← Repositories (Database operations)
├─────────────────────────────────────┤
│           Database Layer            │  ← MySQL Database
└─────────────────────────────────────┘
```

### Package Structure

```
com.pos/
├── controller/          # REST API endpoints
├── service/            # Business logic
├── repository/         # Data access
├── model/              # JPA entities
├── dto/                # Data Transfer Objects
├── exception/          # Custom exceptions
└── config/             # Configuration classes
```

---

## 🗄️ Database Design & Relationships

### Entity Relationships

```
Categories (1) ←→ (Many) Products
Products (Many) ←→ (Many) SaleItems
Sales (1) ←→ (Many) SaleItems
```

### Database Schema Details

#### 1. Categories Table

```sql
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,        -- Category name (unique)
    description TEXT,                         -- Optional description
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### 2. Products Table

```sql
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,               -- Product name
    description TEXT,                         -- Product description
    price DECIMAL(10,2) NOT NULL CHECK (price > 0),  -- Price validation
    stock_quantity INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),  -- Stock validation
    category_id BIGINT,                       -- Foreign key to categories
    barcode VARCHAR(50) UNIQUE,               -- Unique barcode
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);
```

#### 3. Sales Table

```sql
CREATE TABLE sales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(200),               -- Customer name
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0,   -- Total sale amount
    payment_method ENUM('CASH', 'CARD', 'MOBILE') DEFAULT 'CASH',
    status ENUM('COMPLETED', 'PENDING', 'CANCELLED') DEFAULT 'COMPLETED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### 4. Sale Items Table

```sql
CREATE TABLE sale_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_id BIGINT NOT NULL,                  -- Foreign key to sales
    product_id BIGINT NOT NULL,               -- Foreign key to products
    quantity INT NOT NULL CHECK (quantity > 0),      -- Quantity validation
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price > 0),  -- Unit price validation
    total_price DECIMAL(10,2) NOT NULL CHECK (total_price > 0), -- Total price validation
    FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
);
```

---

## 🌐 API Layer (Controllers)

### Controller Responsibilities

- **Handle HTTP requests/responses**
- **Validate input data**
- **Delegate to service layer**
- **Return appropriate HTTP status codes**

### Example: ProductController

```java
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    // GET /api/v1/products
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        List<ProductDto> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    // POST /api/v1/products
    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductDto productDto) {
        ProductDto createdProduct = productService.createProduct(productDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }
}
```

### API Endpoints Summary

| Method | Endpoint                             | Description               | Request Body        | Response            |
| ------ | ------------------------------------ | ------------------------- | ------------------- | ------------------- |
| GET    | `/api/v1/categories`                 | Get all categories        | None                | `List<CategoryDto>` |
| GET    | `/api/v1/categories/{id}`            | Get category by ID        | None                | `CategoryDto`       |
| POST   | `/api/v1/categories`                 | Create new category       | `CategoryDto`       | `CategoryDto`       |
| PUT    | `/api/v1/categories/{id}`            | Update category           | `CategoryDto`       | `CategoryDto`       |
| DELETE | `/api/v1/categories/{id}`            | Delete category           | None                | `204 No Content`    |
| GET    | `/api/v1/products`                   | Get all products          | None                | `List<ProductDto>`  |
| GET    | `/api/v1/products/{id}`              | Get product by ID         | None                | `ProductDto`        |
| GET    | `/api/v1/products/barcode/{barcode}` | Get product by barcode    | None                | `ProductDto`        |
| POST   | `/api/v1/products`                   | Create new product        | `ProductDto`        | `ProductDto`        |
| PUT    | `/api/v1/products/{id}`              | Update product            | `ProductDto`        | `ProductDto`        |
| PATCH  | `/api/v1/products/{id}/stock`        | Update stock              | Query param         | `200 OK`            |
| DELETE | `/api/v1/products/{id}`              | Delete product            | None                | `204 No Content`    |
| GET    | `/api/v1/sales`                      | Get all sales (paginated) | Query params        | `Page<SaleDto>`     |
| POST   | `/api/v1/sales`                      | Create new sale           | `CreateSaleRequest` | `SaleDto`           |
| POST   | `/api/v1/sales/{id}/cancel`          | Cancel sale               | None                | `200 OK`            |

---

## ⚙️ Business Logic Layer (Services)

### Service Responsibilities

- **Implement business rules**
- **Handle transactions**
- **Coordinate between repositories**
- **Validate business logic**

### Example: ProductService

```java
@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductDto createProduct(ProductDto productDto) {
        // Business Rule 1: Check if barcode already exists
        if (productDto.getBarcode() != null &&
            productRepository.existsByBarcode(productDto.getBarcode())) {
            throw new RuntimeException("Barcode already exists");
        }

        // Business Rule 2: Validate category exists
        Category category = null;
        if (productDto.getCategoryId() != null) {
            category = categoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", productDto.getCategoryId()));
        }

        // Convert DTO to Entity
        Product product = new Product();
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setPrice(productDto.getPrice());
        product.setStockQuantity(productDto.getStockQuantity());
        product.setBarcode(productDto.getBarcode());
        product.setCategory(category);

        // Save to database
        Product savedProduct = productRepository.save(product);

        // Convert back to DTO and return
        return convertToDto(savedProduct);
    }
}
```

### Key Business Rules

#### Product Management

1. **Barcode Uniqueness**: No two products can have the same barcode
2. **Price Validation**: Product price must be greater than 0
3. **Stock Validation**: Stock quantity cannot be negative
4. **Category Validation**: Product category must exist

#### Sales Processing

1. **Stock Availability**: Cannot sell more than available stock
2. **Transaction Integrity**: All sale items must be processed together
3. **Stock Update**: Stock is automatically reduced when sale is created
4. **Stock Restoration**: Stock is restored when sale is cancelled

---

## 💾 Data Access Layer (Repositories)

### Repository Responsibilities

- **Database operations**
- **Custom queries**
- **Data persistence**

### Example: ProductRepository

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Find product by barcode
    Optional<Product> findByBarcode(String barcode);

    // Find products by category
    List<Product> findByCategoryId(Long categoryId);

    // Search products by name (case-insensitive)
    List<Product> findByNameContainingIgnoreCase(String name);

    // Custom query for low stock products
    @Query("SELECT p FROM Product p WHERE p.stockQuantity <= :threshold")
    List<Product> findLowStockProducts(@Param("threshold") Integer threshold);

    // Check if barcode exists
    boolean existsByBarcode(String barcode);
}
```

### Repository Methods Explained

| Method                                 | SQL Equivalent                                           | Purpose                |
| -------------------------------------- | -------------------------------------------------------- | ---------------------- |
| `findAll()`                            | `SELECT * FROM products`                                 | Get all products       |
| `findById(id)`                         | `SELECT * FROM products WHERE id = ?`                    | Get product by ID      |
| `save(product)`                        | `INSERT/UPDATE products`                                 | Save or update product |
| `deleteById(id)`                       | `DELETE FROM products WHERE id = ?`                      | Delete product         |
| `findByBarcode(barcode)`               | `SELECT * FROM products WHERE barcode = ?`               | Find by barcode        |
| `findByCategoryId(categoryId)`         | `SELECT * FROM products WHERE category_id = ?`           | Find by category       |
| `findByNameContainingIgnoreCase(name)` | `SELECT * FROM products WHERE LOWER(name) LIKE LOWER(?)` | Search by name         |

---

## 📊 Data Models & DTOs

### Entity vs DTO Pattern

#### Entity (Model)

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

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
}
```

#### DTO (Data Transfer Object)

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
}
```

### Why Use DTOs?

1. **Security**: Hide sensitive data
2. **Performance**: Transfer only needed data
3. **Flexibility**: Different DTOs for different use cases
4. **API Stability**: Internal changes don't affect API

---

## 🔄 Data Flow Examples

### Example 1: Creating a Product

```
1. Client Request
   POST /api/v1/products
   {
     "name": "iPhone 15",
     "price": 999.99,
     "categoryId": 1
   }

2. Controller Layer
   ProductController.createProduct(ProductDto productDto)
   ↓
   Validates @Valid annotation
   ↓
   Calls productService.createProduct(productDto)

3. Service Layer
   ProductService.createProduct(ProductDto productDto)
   ↓
   Validates business rules (barcode uniqueness, category exists)
   ↓
   Converts DTO to Entity
   ↓
   Calls productRepository.save(product)

4. Repository Layer
   ProductRepository.save(Product product)
   ↓
   Executes SQL: INSERT INTO products (name, price, category_id) VALUES (?, ?, ?)
   ↓
   Returns saved Product entity

5. Response Flow
   Product entity → ProductService.convertToDto() → ProductDto → Controller → JSON Response
```

### Example 2: Creating a Sale

```
1. Client Request
   POST /api/v1/sales
   {
     "customerName": "John Doe",
     "paymentMethod": "CASH",
     "saleItems": [
       {
         "productId": 1,
         "quantity": 2
       }
     ]
   }

2. Service Layer (Complex Business Logic)
   SaleService.createSale(CreateSaleRequest request)
   ↓
   @Transactional begins
   ↓
   Creates Sale entity
   ↓
   For each sale item:
     - Validates product exists
     - Checks stock availability
     - Creates SaleItem entity
     - Updates product stock
   ↓
   Calculates total amount
   ↓
   Saves sale and items
   ↓
   @Transactional commits

3. Database Operations
   INSERT INTO sales (customer_name, payment_method, total_amount)
   INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, total_price)
   UPDATE products SET stock_quantity = stock_quantity - 2 WHERE id = 1
```

---

## ⚠️ Error Handling & Validation

### Validation Layers

#### 1. Input Validation (Controller Level)

```java
@PostMapping
public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductDto productDto) {
    // @Valid triggers validation annotations
}

public class ProductDto {
    @NotBlank(message = "Product name is required")
    private String name;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;
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
}
```

### Error Response Format

```json
{
  "timestamp": "2024-01-01T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: {name=Product name is required}",
  "path": "/api/v1/products"
}
```

---

## 💰 Transaction Management

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

```
BEGIN TRANSACTION
  INSERT INTO sales (customer_name, total_amount) VALUES ('John', 1999.98)
  INSERT INTO sale_items (sale_id, product_id, quantity) VALUES (1, 1, 2)
  UPDATE products SET stock_quantity = stock_quantity - 2 WHERE id = 1
COMMIT TRANSACTION
```

#### 2. Failed Sale Transaction (Insufficient Stock)

```
BEGIN TRANSACTION
  INSERT INTO sales (customer_name, total_amount) VALUES ('John', 1999.98)
  -- Stock check fails, throws exception
  -- All changes are automatically rolled back
ROLLBACK TRANSACTION
```

---

## 🔒 Security Configuration

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

## 🧪 Testing & Deployment

### Testing Strategy

#### 1. Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProduct_WithValidData_ShouldReturnProduct() {
        // Test implementation
    }
}
```

#### 2. Integration Tests

```java
@SpringBootTest
@AutoConfigureTestDatabase
class ProductControllerIntegrationTest {

    @Test
    void createProduct_ShouldReturnCreatedProduct() {
        // Test with actual database
    }
}
```

### Deployment Configuration

#### Application Properties

```properties
# Production Database
spring.datasource.url=jdbc:mysql://prod-server:3306/pos_system
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# JPA Configuration
spring.jpa.hibernate.ddl-auto=validate  # Don't auto-create tables in production
spring.jpa.show-sql=false  # Disable SQL logging in production

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

This documentation provides a comprehensive understanding of how the POS system works, from API calls to database operations, and everything in between!
