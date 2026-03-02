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
    name VARCHAR(20) UNIQUE NOT NULL
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
    price DECIMAL(10,2) NOT NULL,
    unit VARCHAR(50),
    image_url VARCHAR(500),
    available BOOLEAN DEFAULT true NOT NULL,
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
    item_id BIGINT NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    ingredient_id BIGINT NOT NULL REFERENCES ingredients(id) ON DELETE CASCADE,
    quantity DECIMAL(19,2) NOT NULL,
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
    value_type VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE',
    value DECIMAL(12,2) NOT NULL,
    min_order_amount DECIMAL(12,2),
    max_discount_amount DECIMAL(12,2),
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    usage_limit INTEGER,
    used_count INTEGER DEFAULT 0,
    min_party_size INTEGER,
    max_party_size INTEGER,
    tier_config TEXT,
    applicable_days TEXT,
    apply_to_specific_items BOOLEAN DEFAULT false,
    active BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_discount_value_type CHECK (value_type IN ('PERCENTAGE', 'FIXED_AMOUNT', 'FIXED_PRICE'))
);

-- Add comments to discount columns
COMMENT ON COLUMN discounts.value_type IS 'Type of discount value: PERCENTAGE (%), FIXED_AMOUNT (fixed discount), FIXED_PRICE (set item price)';
COMMENT ON COLUMN discounts.min_party_size IS 'Minimum party size for PARTY_SIZE discount type';
COMMENT ON COLUMN discounts.max_party_size IS 'Maximum party size for PARTY_SIZE discount type';
COMMENT ON COLUMN discounts.tier_config IS 'JSON config for BILL_TIER discount: {"tier1":{"min":200000,"discount":10}}';
COMMENT ON COLUMN discounts.applicable_days IS 'Applicable days for HOLIDAY discount: "MONDAY,FRIDAY" or "2026-01-01,2026-02-14"';
COMMENT ON COLUMN discounts.apply_to_specific_items IS 'Flag to indicate if discount applies to specific items only';

-- Reservations table
CREATE TABLE IF NOT EXISTS reservations (
    id BIGSERIAL PRIMARY KEY,
    customer_name VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    customer_email VARCHAR(255),
    party_size INTEGER NOT NULL,
    reservation_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    note TEXT,
    deposit_required BOOLEAN DEFAULT FALSE,
    deposit_amount DECIMAL(10,2),
    deposit_paid BOOLEAN DEFAULT FALSE,
    grace_period_minutes INTEGER DEFAULT 15,
    arrival_time TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason VARCHAR(255),
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for reservations
CREATE INDEX IF NOT EXISTS idx_reservation_time ON reservations(reservation_time);
CREATE INDEX IF NOT EXISTS idx_status ON reservations(status);
CREATE INDEX IF NOT EXISTS idx_customer_phone ON reservations(customer_phone);

-- Bills table (must be created before orders and payments due to foreign keys)
CREATE TABLE IF NOT EXISTS bills (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT REFERENCES reservations(id),
    total_price DECIMAL(10,2) NOT NULL DEFAULT 0,
    party_size INTEGER,
    discount_id BIGINT REFERENCES discounts(id),
    discount_amount DECIMAL(10,2) DEFAULT 0,
    final_price DECIMAL(10,2) DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    closed_at TIMESTAMP
);

-- Payments table
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    bill_id BIGINT NOT NULL REFERENCES bills(id),
    method VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_id VARCHAR(255) UNIQUE,
    payment_url TEXT,
    momo_order_id VARCHAR(255),
    momo_request_id VARCHAR(255),
    momo_trans_id VARCHAR(255),
    gateway_response TEXT,
    error_message VARCHAR(255),
    paid_at TIMESTAMP,
    refunded_at TIMESTAMP,
    refund_amount DECIMAL(10,2),
    refund_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for payments
CREATE INDEX IF NOT EXISTS idx_payment_transaction_id ON payments(transaction_id);
CREATE INDEX IF NOT EXISTS idx_payment_status ON payments(status);

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    bill_id BIGINT NOT NULL REFERENCES bills(id),
    reservation_id BIGINT REFERENCES reservations(id),
    order_type VARCHAR(20) NOT NULL DEFAULT 'AT_TABLE',
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

-- Reservation Tables (junction table)
CREATE TABLE IF NOT EXISTS reservation_tables (
    reservation_id BIGINT NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
    table_id BIGINT NOT NULL REFERENCES tables(id) ON DELETE CASCADE,
    PRIMARY KEY (reservation_id, table_id)
);

-- Ingredient Batches table
CREATE TABLE IF NOT EXISTS ingredient_batches (
    id BIGSERIAL PRIMARY KEY,
    ingredient_id BIGINT NOT NULL REFERENCES ingredients(id),
    expiry_date TIMESTAMP,
    quantity DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Ingredient Usages table (tracks ingredient consumption per order detail)
CREATE TABLE IF NOT EXISTS ingredient_usages (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL REFERENCES ingredient_batches(id),
    order_detail_id BIGINT NOT NULL REFERENCES order_details(id) ON DELETE CASCADE,
    quantity_used DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Discount Items (junction table)
CREATE TABLE IF NOT EXISTS discount_items (
    discount_id BIGINT NOT NULL REFERENCES discounts(id) ON DELETE CASCADE,
    item_id BIGINT NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    PRIMARY KEY (discount_id, item_id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_discounts_value_type ON discounts(value_type);
CREATE INDEX IF NOT EXISTS idx_discount_items_discount_id ON discount_items(discount_id);
CREATE INDEX IF NOT EXISTS idx_discount_items_item_id ON discount_items(item_id);

-- Grant privileges (only for production database)
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO restaurant_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO restaurant_user;

-- ==========================================
-- INITIAL DATA
-- ==========================================

-- Roles (MUST insert first)
INSERT INTO roles (id, name) VALUES
(1, 'ADMIN'),
(2, 'STAFF'),
(3, 'CHEF'),
(4, 'CUSTOMER'),
(5, 'CASHIER'),
(6, 'MANAGER');

-- Users (password: admin123, admin123, admin123)
-- Hash generated with BCryptPasswordEncoder for "admin123"
INSERT INTO users (id, email, password, full_name, phone, role_id, active, created_at, updated_at) VALUES
(1, 'admin@restaurant.com', '$2a$12$Ep5HCg28eCw3nqOJPlQytuCHfiwBBKYdGg6uni3X0noBQyNLZF81m', 'Administrator', '0901234567', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'staff@restaurant.com', '$2a$12$Ep5HCg28eCw3nqOJPlQytuCHfiwBBKYdGg6uni3X0noBQyNLZF81m', 'Staff Member', '0901234568', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'chef@restaurant.com', '$2a$12$Ep5HCg28eCw3nqOJPlQytuCHfiwBBKYdGg6uni3X0noBQyNLZF81m', 'Head Chef', '0901234569', 3, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'cashier@restaurant.com', '$2a$12$Ep5HCg28eCw3nqOJPlQytuCHfiwBBKYdGg6uni3X0noBQyNLZF81m', 'Cashier', '0901234449', 5, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'manager@restaurant.com', '$2a$12$Ep5HCg28eCw3nqOJPlQytuCHfiwBBKYdGg6uni3X0noBQyNLZF81m', 'Manager', '0901231239', 6, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

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
INSERT INTO items (id, category_id, name, description, price, unit, image_url, available, created_at, updated_at) VALUES
(1, 1, 'Spring Rolls', 'Chả giò giòn rụm với rau sống', 45000, NULL, '/images/spring-rolls.jpg', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, 'Fried Wonton', 'Hoành thánh chiên giòn', 50000, NULL, '/images/fried-wonton.jpg', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 1, 'Caesar Salad', 'Salad rau trộn sốt Caesar', 65000, NULL, '/images/caesar-salad.jpg', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 2, 'Fried Rice with Chicken', 'Cơm chiên gà', 75000, NULL, '/images/chicken-fried-rice.jpg', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 2, 'Grilled Beef Steak', 'Bò bít tết nướng', 185000, NULL, '/images/beef-steak.jpg', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 2, 'Shrimp Pasta', 'Mì Ý tôm sốt kem', 95000, NULL, '/images/shrimp-pasta.jpg', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 2, 'Grilled Chicken', 'Gà nướng mật ong', 120000, NULL, '/images/grilled-chicken.jpg', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 3, 'Vietnamese Coffee', 'Cà phê sữa đá', 35000, NULL, '/images/vietnamese-coffee.jpg', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 3, 'Fresh Orange Juice', 'Nước cam vắt', 30000, NULL, '/images/orange-juice.jpg', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 3, 'Soft Drink', 'Nước ngọt các loại', 20000, NULL, '/images/soft-drink.jpg', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, 3, 'Iced Tea', 'Trà đá chanh', 25000, NULL, '/images/iced-tea.jpg', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, 4, 'Tiramisu', 'Bánh Tiramisu Ý', 55000, NULL, '/images/tiramisu.jpg', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(13, 4, 'Ice Cream', 'Kem tươi các vị', 35000, NULL, '/images/ice-cream.jpg', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(14, 4, 'Flan Caramel', 'Bánh flan caramen', 30000, NULL, '/images/flan.jpg', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(15, 5, 'Tom Yum Soup', 'Súp Tom Yum chua cay', 70000, NULL, '/images/tom-yum.jpg', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(16, 5, 'Chicken Noodle Soup', 'Phở gà', 60000, NULL, '/images/pho-ga.jpg', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Recipes (Item-Ingredient relationships)
INSERT INTO recipes (id, item_id, ingredient_id, quantity, unit) VALUES
-- Spring Rolls
(1, 1, 1, 0.05, NULL),
(2, 1, 6, 0.02, NULL),
(3, 1, 10, 0.05, NULL),
-- Fried Rice with Chicken
(4, 4, 1, 0.15, NULL),
(5, 4, 2, 0.1, NULL),
(6, 4, 6, 0.03, NULL),
(7, 4, 9, 0.02, NULL),
-- Grilled Beef Steak
(8, 5, 3, 0.25, NULL),
(9, 5, 7, 0.01, NULL),
(10, 5, 18, 0.15, NULL),
-- Shrimp Pasta
(11, 6, 4, 0.15, NULL),
(12, 6, 19, 0.1, NULL),
(13, 6, 14, 0.05, NULL),
-- Vietnamese Coffee
(14, 8, 13, 0.02, NULL),
(15, 8, 14, 0.03, NULL),
(16, 8, 11, 0.01, NULL);

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

-- Discounts (discount_type must be: ITEM_SPECIFIC, HOLIDAY, PARTY_SIZE, BILL_TIER)
INSERT INTO discounts (id, code, name, description, discount_type, value_type, value, min_order_amount, max_discount_amount, start_date, end_date, usage_limit, used_count, active, created_at, updated_at) VALUES
(1, 'WELCOME10', 'Welcome Discount', 'Giảm 10% cho khách hàng mới', 'HOLIDAY', 'PERCENTAGE', 10, 100000, 50000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '30 days', 100, 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'FREESHIP', 'Free Shipping', 'Miễn phí vận chuyển đơn từ 200k', 'BILL_TIER', 'FIXED_AMOUNT', 30000, 200000, 30000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '30 days', 200, 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'LUNCH20', 'Lunch Special', 'Giảm 20% giờ vàng (11h-13h)', 'HOLIDAY', 'PERCENTAGE', 20, 150000, 100000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '60 days', NULL, 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'HAPPY50K', 'Happy Hour', 'Giảm 50k cho đơn từ 300k', 'BILL_TIER', 'FIXED_AMOUNT', 50000, 300000, 50000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '15 days', 50, 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'SALE20', 'Flash Sale 20%', 'Giảm 20% tối đa 50k cho đơn từ 100k', 'HOLIDAY', 'PERCENTAGE', 20, 100000, 50000, '2026-01-25 00:00:00', '2026-02-28 23:59:59', 100, 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'PIZZA20', 'Pizza Sale 20%', 'Giảm 20% cho tất cả món Pizza', 'ITEM_SPECIFIC', 'PERCENTAGE', 20, NULL, NULL, '2026-01-25 00:00:00', '2026-02-28 23:59:59', NULL, 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'WEEKENDPARTY', 'Weekend Party 25%', 'Giảm 25% vào thứ 7, chủ nhật', 'HOLIDAY', 'PERCENTAGE', 25, 200000, 100000, '2026-02-01 00:00:00', '2026-02-28 23:59:59', NULL, 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 'TET2026', 'Tet Holiday Sale', 'Giảm 30% dịp Tết Nguyên Đán', 'HOLIDAY', 'PERCENTAGE', 30, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '30 days', NULL, 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 'GROUP4', 'Group Discount 4+ people', 'Giảm 10% cho nhóm 4-6 người', 'PARTY_SIZE', 'PERCENTAGE', 10, 200000, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '60 days', NULL, 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 'GROUP7', 'Group Discount 7+ people', 'Giảm 15% cho nhóm 7-10 người', 'PARTY_SIZE', 'PERCENTAGE', 15, 7, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '60 days', NULL, 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, 'BIGTIER', 'Spend More Save More', 'Giảm theo bậc: 5% (200k+), 10% (500k+), 15% (1M+)', 'BILL_TIER', 'PERCENTAGE', 0, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '90 days', NULL, 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, 'FIXEDTIER', 'Fixed Discount Tiers', 'Giảm cố định: 20k (200k+), 60k (500k+), 150k (1M+)', 'BILL_TIER', 'FIXED_AMOUNT', 0, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '90 days', NULL, 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Update specific discount configurations
UPDATE discounts SET applicable_days = 'SATURDAY,SUNDAY' WHERE code = 'WEEKENDPARTY';
UPDATE discounts SET applicable_days = '2026-01-01,2026-02-10,2026-02-11,2026-02-12' WHERE code = 'TET2026';
UPDATE discounts SET min_party_size = 4, max_party_size = 6 WHERE code = 'GROUP4';
UPDATE discounts SET min_party_size = 7, max_party_size = 10 WHERE code = 'GROUP7';
UPDATE discounts SET tier_config = '{"tier1":{"min":200000,"discount":5},"tier2":{"min":500000,"discount":10},"tier3":{"min":1000000,"discount":15}}' WHERE code = 'BIGTIER';
UPDATE discounts SET tier_config = '{"tier1":{"min":200000,"discount":20000},"tier2":{"min":500000,"discount":60000},"tier3":{"min":1000000,"discount":150000}}' WHERE code = 'FIXEDTIER';
UPDATE discounts SET apply_to_specific_items = true WHERE code = 'PIZZA20';

-- Reset sequences to proper values
SELECT setval('roles_id_seq', (SELECT MAX(id) FROM roles));
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));
SELECT setval('ingredients_id_seq', (SELECT MAX(id) FROM ingredients));
SELECT setval('items_id_seq', (SELECT MAX(id) FROM items));
SELECT setval('recipes_id_seq', (SELECT MAX(id) FROM recipes));
SELECT setval('tables_id_seq', (SELECT MAX(id) FROM tables));
SELECT setval('discounts_id_seq', (SELECT MAX(id) FROM discounts));

-- ==========================================
-- Comments for better documentation
-- ==========================================
COMMENT ON TABLE reservations IS 'Customer table reservations';
COMMENT ON TABLE payments IS 'Payment transactions for bills';
COMMENT ON TABLE bills IS 'Customer bills with discount tracking';
COMMENT ON TABLE orders IS 'Orders linked to bills and reservations';
COMMENT ON TABLE reservation_tables IS 'Junction table linking reservations to tables';

COMMENT ON COLUMN bills.discount_amount IS 'Applied discount amount';
COMMENT ON COLUMN bills.discount_id IS 'Reference to applied discount';
COMMENT ON COLUMN bills.party_size IS 'Number of people in the party';
COMMENT ON COLUMN bills.final_price IS 'Final price after discount';

COMMENT ON COLUMN payments.method IS 'Payment method: CASH, MOMO, BANK_TRANSFER, CREDIT_CARD';
COMMENT ON COLUMN payments.status IS 'Payment status: PENDING, COMPLETED, FAILED, REFUNDED';

COMMENT ON COLUMN reservations.status IS 'Reservation status: PENDING, CONFIRMED, SEATED, COMPLETED, NO_SHOW, CANCELLED';
COMMENT ON COLUMN reservations.grace_period_minutes IS 'Minutes to wait after reservation time before marking as no-show';

COMMENT ON COLUMN orders.order_type IS 'Order type: AT_TABLE, TAKE_AWAY, PRE_ORDER';

-- End of init-db.sql
