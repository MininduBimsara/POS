-- Sample data for POS System

-- Insert sample categories
INSERT INTO categories (name, description) VALUES
('Electronics', 'Electronic devices and accessories'),
('Clothing', 'Apparel and fashion items'),
('Books', 'Books and publications'),
('Food & Beverages', 'Food and drink items'),
('Home & Garden', 'Home improvement and garden supplies');

-- Insert sample products
INSERT INTO products (name, description, price, stock_quantity, category_id, barcode) VALUES
('iPhone 15', 'Latest iPhone model with advanced features', 999.99, 50, 1, '1234567890123'),
('Samsung Galaxy S24', 'Premium Android smartphone', 899.99, 45, 1, '1234567890124'),
('MacBook Pro', 'Professional laptop for developers', 1999.99, 25, 1, '1234567890125'),
('Nike Air Max', 'Comfortable running shoes', 129.99, 100, 2, '1234567890126'),
('Adidas T-Shirt', 'Cotton sports t-shirt', 29.99, 200, 2, '1234567890127'),
('Java Programming Book', 'Complete guide to Java development', 49.99, 75, 3, '1234567890128'),
('Python Cookbook', 'Advanced Python programming techniques', 39.99, 60, 3, '1234567890129'),
('Coffee Beans', 'Premium Arabica coffee beans', 15.99, 150, 4, '1234567890130'),
('Organic Tea', 'Green tea with natural ingredients', 8.99, 120, 4, '1234567890131'),
('Garden Hose', 'Heavy-duty garden watering hose', 45.99, 30, 5, '1234567890132'); 