# Point of Sale (POS) System Backend

A complete Spring Boot backend application for a Point of Sale system with RESTful APIs, MySQL database, and layered architecture.

## 🚀 Features

- **Product Management**: CRUD operations for products with categories
- **Inventory Management**: Stock tracking and low stock alerts
- **Sales Processing**: Complete sales workflow with stock validation
- **Category Management**: Organize products by categories
- **RESTful APIs**: Clean, consistent API design
- **Data Validation**: Comprehensive input validation
- **Error Handling**: Global exception handling with consistent error responses
- **Transaction Management**: ACID-compliant sales transactions

## 🛠️ Technology Stack

- **Java 17**
- **Spring Boot 3.5.3**
- **Spring Data JPA**
- **Spring Security**
- **Spring Validation**
- **MySQL Database**
- **Maven**
- **Lombok**

## 📋 Prerequisites

- Java 17 or higher
- MySQL 8.0 or higher
- Maven 3.6 or higher

## 🗄️ Database Design

### Tables Structure

#### 1. Categories Table

```sql
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### 2. Products Table

```sql
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL CHECK (price > 0),
    stock_quantity INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    category_id BIGINT,
    barcode VARCHAR(50) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);
```

#### 3. Sales Table

```sql
CREATE TABLE sales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(200),
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
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
    sale_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price > 0),
    total_price DECIMAL(10,2) NOT NULL CHECK (total_price > 0),
    FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
);
```

## ⚙️ Configuration

### Database Configuration

Update `application.properties` with your MySQL credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/pos_system?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## 🚀 Running the Application

1. **Clone the repository**

   ```bash
   git clone <repository-url>
   cd product
   ```

2. **Configure database**

   - Update `application.properties` with your MySQL credentials
   - Ensure MySQL is running

3. **Build the project**

   ```bash
   mvn clean install
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080`

## 📚 API Documentation

### Base URL: `http://localhost:8080/api/v1`

### Categories API

#### Get All Categories

```http
GET /categories
```

#### Get Category by ID

```http
GET /categories/{id}
```

#### Create Category

```http
POST /categories
Content-Type: application/json

{
    "name": "Electronics",
    "description": "Electronic devices and accessories"
}
```

#### Update Category

```http
PUT /categories/{id}
Content-Type: application/json

{
    "name": "Electronics",
    "description": "Updated description"
}
```

#### Delete Category

```http
DELETE /categories/{id}
```

### Products API

#### Get All Products

```http
GET /products
```

#### Get Product by ID

```http
GET /products/{id}
```

#### Get Product by Barcode

```http
GET /products/barcode/{barcode}
```

#### Get Products by Category

```http
GET /products/category/{categoryId}
```

#### Search Products

```http
GET /products/search?name=phone
```

#### Get Low Stock Products

```http
GET /products/low-stock?threshold=10
```

#### Create Product

```http
POST /products
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

#### Update Product

```http
PUT /products/{id}
Content-Type: application/json

{
    "name": "iPhone 15 Pro",
    "description": "Updated description",
    "price": 1099.99,
    "stockQuantity": 45,
    "categoryId": 1,
    "barcode": "1234567890124"
}
```

#### Update Stock

```http
PATCH /products/{id}/stock?quantity=10
```

#### Delete Product

```http
DELETE /products/{id}
```

### Sales API

#### Get All Sales (Paginated)

```http
GET /sales?page=0&size=10
```

#### Get Sale by ID

```http
GET /sales/{id}
```

#### Get Sales by Customer Name

```http
GET /sales/customer?customerName=John
```

#### Get Sales by Date Range

```http
GET /sales/date-range?startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59
```

#### Get Sales by Payment Method

```http
GET /sales/payment-method?paymentMethod=CASH
```

#### Create Sale

```http
POST /sales
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

#### Cancel Sale

```http
POST /sales/{id}/cancel
```

## 🏗️ Project Structure

```
src/main/java/com/pos/
├── PosApplication.java
├── controller/
│   ├── CategoryController.java
│   ├── ProductController.java
│   └── SaleController.java
├── service/
│   ├── CategoryService.java
│   ├── ProductService.java
│   └── SaleService.java
├── repository/
│   ├── CategoryRepository.java
│   ├── ProductRepository.java
│   ├── SaleRepository.java
│   └── SaleItemRepository.java
├── model/
│   ├── Category.java
│   ├── Product.java
│   ├── Sale.java
│   └── SaleItem.java
├── dto/
│   ├── CategoryDto.java
│   ├── ProductDto.java
│   ├── SaleDto.java
│   ├── SaleItemDto.java
│   └── CreateSaleRequest.java
├── exception/
│   ├── ResourceNotFoundException.java
│   ├── InsufficientStockException.java
│   └── GlobalExceptionHandler.java
└── config/
    └── SecurityConfig.java
```

## 🔧 Business Logic

### Sales Processing

1. **Stock Validation**: Check if sufficient stock is available
2. **Transaction Management**: All operations are wrapped in transactions
3. **Stock Update**: Automatically reduce stock when sale is created
4. **Stock Restoration**: Restore stock when sale is cancelled

### Validation Rules

- Product price must be greater than 0
- Stock quantity must be non-negative
- Sale quantity must be greater than 0
- Product name and category name are required

## 🚨 Error Handling

The application provides consistent error responses:

```json
{
  "timestamp": "2024-01-01T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: {fieldName=errorMessage}",
  "path": "/api/v1/products"
}
```

## 🔒 Security

- CSRF protection is disabled for API endpoints
- All `/api/v1/**` endpoints are publicly accessible
- Spring Security is configured for basic protection

## 📝 Testing

Run tests with:

```bash
mvn test
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 🆘 Support

For support and questions, please create an issue in the repository.
