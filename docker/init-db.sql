-- Database initialization script for Restaurant QR Order System
-- This script creates the database schema and initial data

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Set default timezone
SET timezone = 'Asia/Ho_Chi_Minh';

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS public;

COMMENT ON DATABASE restaurant_qr_db IS 'Restaurant QR Order Management System Database';

-- ==========================================
-- CREATE TABLES
-- ==========================================

-- Roles table (MUST be created first due to foreign key)
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role_id BIGINT NOT NULL REFERENCES roles(id),
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Categories table
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT
);

-- Items table
CREATE TABLE IF NOT EXISTS items (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT REFERENCES categories(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(12,2) NOT NULL,
    image_url VARCHAR(500),
    available BOOLEAN DEFAULT true NOT NULL,
    preparation_time INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Ingredients table
CREATE TABLE IF NOT EXISTS ingredients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    stock_quantity DECIMAL(10,2) NOT NULL DEFAULT 0,
    unit VARCHAR(50),
    updated_at TIMESTAMP
);

-- Recipes table (Item-Ingredient relationship)
CREATE TABLE IF NOT EXISTS recipes (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT REFERENCES items(id) ON DELETE CASCADE,
    ingredient_id BIGINT REFERENCES ingredients(id) ON DELETE CASCADE,
    quantity DECIMAL(12,3) NOT NULL,
    unit VARCHAR(50)
);

-- Tables table
CREATE TABLE IF NOT EXISTS tables (
    id BIGSERIAL PRIMARY KEY,
    table_number VARCHAR(50) UNIQUE NOT NULL,
    qr_code VARCHAR(500) UNIQUE NOT NULL,
    capacity INTEGER NOT NULL,
    status VARCHAR(50) DEFAULT 'AVAILABLE',
    location VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Discounts table
CREATE TABLE IF NOT EXISTS discounts (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    discount_type VARCHAR(50) NOT NULL,
    value DECIMAL(12,2) NOT NULL,
    min_order_amount DECIMAL(12,2),
    max_discount_amount DECIMAL(12,2),
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    usage_limit INTEGER,
    used_count INTEGER DEFAULT 0,
    active BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bills table (must be created before orders due to foreign key)
CREATE TABLE IF NOT EXISTS bills (
    id BIGSERIAL PRIMARY KEY,
    total_price DECIMAL(10,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    closed_at TIMESTAMP
);

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    bill_id BIGINT NOT NULL REFERENCES bills(id),
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Order Details table
CREATE TABLE IF NOT EXISTS order_details (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    item_id BIGINT NOT NULL REFERENCES items(id),
    quantity INTEGER NOT NULL,
    item_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    note TEXT,
    price DECIMAL(10,2) NOT NULL
);

-- Bill Tables (junction table)
CREATE TABLE IF NOT EXISTS bill_tables (
    bill_id BIGINT NOT NULL REFERENCES bills(id),
    table_id BIGINT NOT NULL REFERENCES tables(id),
    PRIMARY KEY (bill_id, table_id)
);

-- Ingredient Batches table
CREATE TABLE IF NOT EXISTS ingredient_batches (
    id BIGSERIAL PRIMARY KEY,
    ingredient_id BIGINT NOT NULL REFERENCES ingredients(id),
    batch_number VARCHAR(100),
    quantity DECIMAL(12,3) NOT NULL,
    purchase_price DECIMAL(12,2),
    expiry_date DATE,
    supplier VARCHAR(255),
    received_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Ingredient Usages table (tracks ingredient consumption per order detail)
CREATE TABLE IF NOT EXISTS ingredient_usages (
    id BIGSERIAL PRIMARY KEY,
    order_detail_id BIGINT NOT NULL REFERENCES order_details(id) ON DELETE CASCADE,
    batch_id BIGINT NOT NULL REFERENCES ingredient_batches(id),
    quantity_used DECIMAL(12,3) NOT NULL,
    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Discount Items (junction table)
CREATE TABLE IF NOT EXISTS discount_items (
    discount_id BIGINT NOT NULL REFERENCES discounts(id),
    item_id BIGINT NOT NULL REFERENCES items(id),
    PRIMARY KEY (discount_id, item_id)
);

-- Bills table (removed after table structure change)

-- Grant privileges
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO restaurant_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO restaurant_user;

-- ==========================================
-- INITIAL DATA
-- ==========================================

-- Roles (MUST insert first)
INSERT INTO roles (id, name, description, created_at, updated_at) VALUES
(1, 'ADMIN', 'Administrator with full access', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'STAFF', 'Staff member with limited access', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'CHEF', 'Kitchen staff', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'CUSTOMER', 'Customer role', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Users (password: admin123, staff123, chef123)
-- Hash generated with BCryptPasswordEncoder for "admin123"
INSERT INTO users (id, email, password, full_name, phone, role_id, active, created_at, updated_at) VALUES
(1, 'admin@restaurant.com', '$2a$12$Ep5HCg28eCw3nqOJPlQytuCHfiwBBKYdGg6uni3X0noBQyNLZF81m', 'Administrator', '0901234567', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'staff@restaurant.com', '$2a$12$Ep5HCg28eCw3nqOJPlQytuCHfiwBBKYdGg6uni3X0noBQyNLZF81m', 'Staff Member', '0901234568', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'chef@restaurant.com', '$2a$12$Ep5HCg28eCw3nqOJPlQytuCHfiwBBKYdGg6uni3X0noBQyNLZF81m', 'Head Chef', '0901234569', 3, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Categories
INSERT INTO categories (id, name, description) VALUES
(1, 'Appetizers', 'Món khai vị'),
(2, 'Main Course', 'Món chính'),
(3, 'Beverages', 'Đồ uống'),
(4, 'Desserts', 'Món tráng miệng'),
(5, 'Soup', 'Canh/Súp');

-- Ingredients
INSERT INTO ingredients (id, name, unit, stock_quantity) VALUES
(1, 'Rice', 'kg', 500.00),
(2, 'Chicken Breast', 'kg', 100.00),
(3, 'Beef', 'kg', 80.00),
(4, 'Shrimp', 'kg', 50.00),
(5, 'Tomato', 'kg', 30.00),
(6, 'Onion', 'kg', 40.00),
(7, 'Garlic', 'kg', 20.00),
(8, 'Fish Sauce', 'liter', 25.00),
(9, 'Soy Sauce', 'liter', 20.00),
(10, 'Vegetable Oil', 'liter', 30.00),
(11, 'Sugar', 'kg', 50.00),
(12, 'Salt', 'kg', 30.00),
(13, 'Coffee Beans', 'kg', 20.00),
(14, 'Milk', 'liter', 40.00),
(15, 'Cheese', 'kg', 15.00),
(16, 'Lettuce', 'kg', 25.00),
(17, 'Cucumber', 'kg', 20.00),
(18, 'Potato', 'kg', 60.00),
(19, 'Pasta', 'kg', 30.00),
(20, 'Flour', 'kg', 50.00);

-- Items (Menu Items)
INSERT INTO items (id, category_id, name, description, price, image_url, available, preparation_time, created_at, updated_at) VALUES
(1, 1, 'Spring Rolls', 'Chả giò giòn rụm với rau sống', 45000, '/images/spring-rolls.jpg', true, 15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, 'Fried Wonton', 'Hoành thánh chiên giòn', 50000, '/images/fried-wonton.jpg', true, 12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 1, 'Caesar Salad', 'Salad rau trộn sốt Caesar', 65000, '/images/caesar-salad.jpg', true, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 2, 'Fried Rice with Chicken', 'Cơm chiên gà', 75000, '/images/chicken-fried-rice.jpg', true, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 2, 'Grilled Beef Steak', 'Bò bít tết nướng', 185000, '/images/beef-steak.jpg', true, 25, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 2, 'Shrimp Pasta', 'Mì Ý tôm sốt kem', 95000, '/images/shrimp-pasta.jpg', true, 22, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 2, 'Grilled Chicken', 'Gà nướng mật ong', 120000, '/images/grilled-chicken.jpg', true, 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 3, 'Vietnamese Coffee', 'Cà phê sữa đá', 35000, '/images/vietnamese-coffee.jpg', true, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 3, 'Fresh Orange Juice', 'Nước cam vắt', 30000, '/images/orange-juice.jpg', true, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 3, 'Soft Drink', 'Nước ngọt các loại', 20000, '/images/soft-drink.jpg', true, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, 3, 'Iced Tea', 'Trà đá chanh', 25000, '/images/iced-tea.jpg', true, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, 4, 'Tiramisu', 'Bánh Tiramisu Ý', 55000, '/images/tiramisu.jpg', true, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(13, 4, 'Ice Cream', 'Kem tươi các vị', 35000, '/images/ice-cream.jpg', true, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(14, 4, 'Flan Caramel', 'Bánh flan caramen', 30000, '/images/flan.jpg', true, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(15, 5, 'Tom Yum Soup', 'Súp Tom Yum chua cay', 70000, '/images/tom-yum.jpg', true, 18, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(16, 5, 'Chicken Noodle Soup', 'Phở gà', 60000, '/images/pho-ga.jpg', true, 15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Recipes (Item-Ingredient relationships)
INSERT INTO recipes (id, item_id, ingredient_id, quantity) VALUES
-- Spring Rolls
(1, 1, 1, 0.05),
(2, 1, 6, 0.02),
(3, 1, 10, 0.05),
-- Fried Rice with Chicken
(4, 4, 1, 0.15),
(5, 4, 2, 0.1),
(6, 4, 6, 0.03),
(7, 4, 9, 0.02),
-- Grilled Beef Steak
(8, 5, 3, 0.25),
(9, 5, 7, 0.01),
(10, 5, 18, 0.15),
-- Shrimp Pasta
(11, 6, 4, 0.15),
(12, 6, 19, 0.1),
(13, 6, 14, 0.05),
-- Vietnamese Coffee
(14, 8, 13, 0.02),
(15, 8, 14, 0.03),
(16, 8, 11, 0.01);

-- Tables
INSERT INTO tables (id, table_number, qr_code, capacity, status, location, created_at, updated_at) VALUES
(1, '01', 'QR_TABLE_01_' || uuid_generate_v4(), 2, 'AVAILABLE', 'Ground Floor - Window Side', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, '02', 'QR_TABLE_02_' || uuid_generate_v4(), 2, 'AVAILABLE', 'Ground Floor - Window Side', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, '03', 'QR_TABLE_03_' || uuid_generate_v4(), 4, 'AVAILABLE', 'Ground Floor - Center', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, '04', 'QR_TABLE_04_' || uuid_generate_v4(), 4, 'AVAILABLE', 'Ground Floor - Center', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, '05', 'QR_TABLE_05_' || uuid_generate_v4(), 6, 'AVAILABLE', 'Ground Floor - VIP Area', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, '06', 'QR_TABLE_06_' || uuid_generate_v4(), 6, 'AVAILABLE', 'Ground Floor - VIP Area', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, '07', 'QR_TABLE_07_' || uuid_generate_v4(), 4, 'AVAILABLE', '2nd Floor - Garden View', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, '08', 'QR_TABLE_08_' || uuid_generate_v4(), 4, 'AVAILABLE', '2nd Floor - Garden View', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, '09', 'QR_TABLE_09_' || uuid_generate_v4(), 8, 'AVAILABLE', '2nd Floor - Party Room', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, '10', 'QR_TABLE_10_' || uuid_generate_v4(), 2, 'AVAILABLE', '2nd Floor - Balcony', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Discounts
INSERT INTO discounts (id, code, name, description, discount_type, value, min_order_amount, max_discount_amount, start_date, end_date, usage_limit, used_count, active, created_at, updated_at) VALUES
(1, 'WELCOME10', 'Welcome Discount', 'Giảm 10% cho khách hàng mới', 'PERCENTAGE', 10, 100000, 50000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '30 days', 100, 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'FREESHIP', 'Free Shipping', 'Miễn phí vận chuyển đơn từ 200k', 'FIXED_AMOUNT', 30000, 200000, 30000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '30 days', 200, 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'LUNCH20', 'Lunch Special', 'Giảm 20% giờ vàng (11h-13h)', 'PERCENTAGE', 20, 150000, 100000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '60 days', NULL, 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'HAPPY50K', 'Happy Hour', 'Giảm 50k cho đơn từ 300k', 'FIXED_AMOUNT', 50000, 300000, 50000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '15 days', 50, 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Reset sequences to proper values
SELECT setval('roles_id_seq', (SELECT MAX(id) FROM roles));
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));
SELECT setval('ingredients_id_seq', (SELECT MAX(id) FROM ingredients));
SELECT setval('items_id_seq', (SELECT MAX(id) FROM items));
SELECT setval('recipes_id_seq', (SELECT MAX(id) FROM recipes));
SELECT setval('tables_id_seq', (SELECT MAX(id) FROM tables));
SELECT setval('discounts_id_seq', (SELECT MAX(id) FROM discounts));
