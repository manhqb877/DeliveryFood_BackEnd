-- Migration to add multi-shop support columns to orders and order_items
ALTER TABLE orders.orders ADD COLUMN IF NOT EXISTS shop2_id BIGINT;
ALTER TABLE orders.orders ADD COLUMN IF NOT EXISTS shop2_name VARCHAR(255);
ALTER TABLE orders.order_items ADD COLUMN IF NOT EXISTS shop_id BIGINT;
