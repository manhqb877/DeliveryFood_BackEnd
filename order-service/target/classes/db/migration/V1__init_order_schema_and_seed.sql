-- ===========================================================
-- 1. Táº O SCHEMA
-- ===========================================================
CREATE SCHEMA IF NOT EXISTS orders;

-- ===========================================================
-- 2. TABLE: carts
-- ===========================================================
CREATE TABLE IF NOT EXISTS orders.carts (
                                              id                    BIGSERIAL PRIMARY KEY,
                                              user_id               BIGINT,
                                              guest_session_id      BIGINT,
                                              shop_id               BIGINT NOT NULL,
                                              area_id               BIGINT NOT NULL,
                                              subtotal              NUMERIC(12,2) DEFAULT 0,
    discount_amount       NUMERIC(12,2) DEFAULT 0,
    delivery_address      JSONB,
    promotion_code        VARCHAR(50),
    expires_at            TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cart_user_shop UNIQUE NULLS NOT DISTINCT (user_id, shop_id),
    CONSTRAINT uq_cart_guest_shop UNIQUE NULLS NOT DISTINCT (guest_session_id, shop_id)
    );

INSERT INTO orders.carts (id, user_id, guest_session_id, shop_id, area_id, subtotal, discount_amount, delivery_address, promotion_code, expires_at) VALUES
                                                                                                                                                          (1, 6, NULL, 1, 1, 95000.00, 10000.00, '{"building": "TÃ²a A", "unit": "A1201", "lat": 10.7769, "lng": 106.7009}'::jsonb, 'CHUNGCU10', NOW() + INTERVAL '24 hours'),
                                                                                                                                                          (2, NULL, 1, 2, 1, 35000.00, 0.00, '{"building": "TÃ²a C", "unit": "C101", "lat": 10.7790, "lng": 106.7030}'::jsonb, NULL, NOW() + INTERVAL '24 hours'),
                                                                                                                                                          (3, 7, NULL, 2, 1, 60000.00, 15000.00, '{"building": "TÃ²a B", "unit": "B0802", "lat": 10.7780, "lng": 106.7020}'::jsonb, 'SHOPCOFFEE15', NOW() + INTERVAL '24 hours');

SELECT setval('orders.carts_id_seq', (SELECT MAX(id) FROM orders.carts));


-- ===========================================================
-- 3. TABLE: cart_items
-- ===========================================================
CREATE TABLE IF NOT EXISTS orders.cart_items (
                                                   id                    BIGSERIAL PRIMARY KEY,
                                                   cart_id               BIGINT NOT NULL REFERENCES orders.carts(id) ON DELETE CASCADE,
    item_id               BIGINT NOT NULL,
    item_name             VARCHAR(255) NOT NULL,
    unit_price            NUMERIC(12,2) NOT NULL,
    quantity              SMALLINT NOT NULL DEFAULT 1,
    selected_options      JSONB DEFAULT '[]'::jsonb,
    item_note             TEXT,
    total_price           NUMERIC(12,2) NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_cart_items_cart ON orders.cart_items(cart_id);

INSERT INTO orders.cart_items (id, cart_id, item_id, item_name, unit_price, quantity, selected_options, item_note, total_price) VALUES
                                                                                                                                      (1, 1, 1, 'CÆ¡m SÆ°á»n BÃ¬ Cháº£', 45000.00, 2, '[{"group": "Topping", "option": "ThÃªm trá»©ng", "extra_price": 5000}]'::jsonb, 'Ãt cÆ¡m', 100000.00),
                                                                                                                                      (2, 2, 5, 'TrÃ  Sá»¯a TrÃ¢n ChÃ¢u HoÃ ng Gia', 35000.00, 1, '[]'::jsonb, 'ÄÃ¡ riÃªng', 35000.00),
                                                                                                                                      (3, 3, 4, 'CÃ  PhÃª Sá»¯a ÄÃ¡ SÃ i GÃ²n', 25000.00, 2, '[{"group": "Äá»™ Ngá»t", "option": "Ãt Ä‘Æ°á»ng", "extra_price": 0}]'::jsonb, NULL, 50000.00);

SELECT setval('orders.cart_items_id_seq', (SELECT MAX(id) FROM orders.cart_items));


-- ===========================================================
-- 4. TABLE: orders
-- ===========================================================
CREATE TABLE IF NOT EXISTS orders.orders (
                                               id                    BIGSERIAL PRIMARY KEY,
                                               order_code            VARCHAR(20) NOT NULL UNIQUE,
    user_id               BIGINT,
    guest_session_id      BIGINT,
    shop_id               BIGINT NOT NULL,
    shop_name             VARCHAR(255) NOT NULL,
    area_id               BIGINT NOT NULL,
    delivery_address      JSONB NOT NULL,
    subtotal              NUMERIC(12,2) NOT NULL,
    discount_amount       NUMERIC(12,2) NOT NULL DEFAULT 0,
    delivery_fee          NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_amount          NUMERIC(12,2) NOT NULL,
    promotion_id          BIGINT,
    promotion_code        VARCHAR(50),
    payment_method        VARCHAR(20) NOT NULL,
    payment_status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    order_status          VARCHAR(30) NOT NULL DEFAULT 'PLACED',
    cancel_reason         TEXT,
    cancelled_by          VARCHAR(20),
    order_note            TEXT,
    idempotency_key       VARCHAR(64) UNIQUE,
    placed_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    confirmed_at          TIMESTAMPTZ,
    ready_at              TIMESTAMPTZ,
    picked_up_at          TIMESTAMPTZ,
    delivered_at          TIMESTAMPTZ,
    completed_at          TIMESTAMPTZ,
    cancelled_at          TIMESTAMPTZ,
    saga_status           VARCHAR(20) DEFAULT 'IN_PROGRESS',
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_orders_user ON orders.orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_guest ON orders.orders(guest_session_id);
CREATE INDEX IF NOT EXISTS idx_orders_shop ON orders.orders(shop_id, order_status);
CREATE INDEX IF NOT EXISTS idx_orders_code ON orders.orders(order_code);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders.orders(order_status, placed_at DESC);

INSERT INTO orders.orders (id, order_code, user_id, guest_session_id, shop_id, shop_name, area_id, delivery_address, subtotal, discount_amount, delivery_fee, total_amount, promotion_id, promotion_code, payment_method, payment_status, order_status, idempotency_key, placed_at, confirmed_at, saga_status) VALUES
                                                                                                                                                                                                                                                                                                                     (101, 'ORD-20260917-A1B2', 6, NULL, 1, 'CÆ¡m Táº¥m PhÃºc Lá»™c Thá»', 1, '{"building": "TÃ²a A", "unit": "A1201", "lat": 10.7769, "lng": 106.7009, "recipient_name": "Nguyá»…n VÄƒn KhÃ¡ch", "recipient_phone": "0906789012"}'::jsonb, 90000.00, 10000.00, 15000.00, 95000.00, 1, 'CHUNGCU10', 'ONLINE', 'PAID', 'CONFIRMED', 'idem_key_order_001', NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '25 minutes', 'COMPLETED'),
                                                                                                                                                                                                                                                                                                                     (102, 'ORD-20260917-C3D4', NULL, 1, 2, 'Tea & Coffee Highland Mini', 1, '{"building": "TÃ²a C", "unit": "C101", "lat": 10.7790, "lng": 106.7030, "recipient_name": "KhÃ¡ch VÃ£ng Lai", "recipient_phone": "0911111111"}'::jsonb, 35000.00, 0.00, 10000.00, 45000.00, NULL, NULL, 'COD', 'COD_PENDING', 'PLACED', 'idem_key_order_002', NOW() - INTERVAL '10 minutes', NULL, 'IN_PROGRESS');

SELECT setval('orders.orders_id_seq', (SELECT MAX(id) FROM orders.orders));


-- ===========================================================
-- 5. TABLE: order_items
-- ===========================================================
CREATE TABLE IF NOT EXISTS orders.order_items (
                                                    id                    BIGSERIAL PRIMARY KEY,
                                                    order_id              BIGINT NOT NULL REFERENCES orders.orders(id) ON DELETE CASCADE,
    item_id               BIGINT NOT NULL,
    item_name             VARCHAR(255) NOT NULL,
    item_image_url        TEXT,
    unit_price            NUMERIC(12,2) NOT NULL,
    quantity              SMALLINT NOT NULL,
    selected_options      JSONB DEFAULT '[]'::jsonb,
    item_note             TEXT,
    total_price           NUMERIC(12,2) NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_order_items_order ON orders.order_items(order_id);

INSERT INTO orders.order_items (id, order_id, item_id, item_name, item_image_url, unit_price, quantity, selected_options, item_note, total_price) VALUES
                                                                                                                                                        (1, 101, 1, 'CÆ¡m SÆ°á»n BÃ¬ Cháº£', 'https://cdn.example.com/items/comsuon.jpg', 45000.00, 2, '[]'::jsonb, NULL, 90000.00),
                                                                                                                                                        (2, 102, 5, 'TrÃ  Sá»¯a TrÃ¢n ChÃ¢u HoÃ ng Gia', 'https://cdn.example.com/items/trasua.jpg', 35000.00, 1, '[]'::jsonb, 'Ãt ngá»t', 35000.00);

SELECT setval('orders.order_items_id_seq', (SELECT MAX(id) FROM orders.order_items));


-- ===========================================================
-- 6. TABLE: order_status_history
-- ===========================================================
CREATE TABLE IF NOT EXISTS orders.order_status_history (
                                                             id            BIGSERIAL PRIMARY KEY,
                                                             order_id      BIGINT NOT NULL REFERENCES orders.orders(id),
    old_status    VARCHAR(30),
    new_status    VARCHAR(30) NOT NULL,
    actor_type    VARCHAR(20) NOT NULL,
    actor_id      BIGINT,
    note          TEXT,
    metadata      JSONB,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_order_history_order ON orders.order_status_history(order_id, created_at);

INSERT INTO orders.order_status_history (id, order_id, old_status, new_status, actor_type, actor_id, note, created_at) VALUES
                                                                                                                             (1, 101, NULL, 'PLACED', 'CUSTOMER', 6, 'KhÃ¡ch Ä‘áº·t Ä‘Æ¡n hÃ ng má»›i', NOW() - INTERVAL '30 minutes'),
                                                                                                                             (2, 101, 'PLACED', 'CONFIRMED', 'SHOP_MANAGER', 2, 'Shop xÃ¡c nháº­n chuáº©n bá»‹ Ä‘Æ¡n', NOW() - INTERVAL '25 minutes'),
                                                                                                                             (3, 102, NULL, 'PLACED', 'GUEST', NULL, 'Guest Ä‘áº·t Ä‘Æ¡n hÃ ng táº¡m thá»i', NOW() - INTERVAL '10 minutes');

SELECT setval('orders.order_status_history_id_seq', (SELECT MAX(id) FROM orders.order_status_history));


-- ===========================================================
-- 7. TABLE: outbox_events
-- ===========================================================
CREATE TABLE IF NOT EXISTS orders.outbox_events (
                                                      id                BIGSERIAL PRIMARY KEY,
                                                      aggregate_type    VARCHAR(50) NOT NULL,
    aggregate_id      BIGINT NOT NULL,
    event_type        VARCHAR(100) NOT NULL,
    payload           JSONB NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count       SMALLINT DEFAULT 0,
    published_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_outbox_pending ON orders.outbox_events(status, created_at) WHERE status = 'PENDING';

INSERT INTO orders.outbox_events (id, aggregate_type, aggregate_id, event_type, payload, status, retry_count, published_at) VALUES
                                                                                                                                  (1, 'ORDER', 101, 'orders.placed', '{"orderId": 101, "shopId": 1, "totalAmount": 95000.00}'::jsonb, 'PUBLISHED', 0, NOW() - INTERVAL '29 minutes'),
                                                                                                                                  (2, 'ORDER', 102, 'orders.placed', '{"orderId": 102, "shopId": 2, "totalAmount": 45000.00}'::jsonb, 'PENDING', 0, NULL);

SELECT setval('orders.outbox_events_id_seq', (SELECT MAX(id) FROM orders.outbox_events));


-- ===========================================================
-- 8. TABLE: reviews
-- ===========================================================
CREATE TABLE IF NOT EXISTS orders.reviews (
                                                id                  BIGSERIAL PRIMARY KEY,
                                                order_id            BIGINT NOT NULL UNIQUE REFERENCES orders.orders(id),
    user_id             BIGINT,
    guest_session_id    BIGINT,
    shop_id             BIGINT NOT NULL,
    shop_rating         SMALLINT NOT NULL CHECK (shop_rating BETWEEN 1 AND 5),
    shop_comment        TEXT,
    delivery_id         BIGINT,
    shipper_rating      SMALLINT CHECK (shipper_rating BETWEEN 1 AND 5),
    shipper_comment     TEXT,
    image_urls          TEXT[] DEFAULT ARRAY[]::TEXT[],
    shop_reply          TEXT,
    shop_replied_at     TIMESTAMPTZ,
    is_anonymous        BOOLEAN DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_reviews_shop ON orders.reviews(shop_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_reviews_order ON orders.reviews(order_id);

INSERT INTO orders.reviews (id, order_id, user_id, shop_id, shop_rating, shop_comment, delivery_id, shipper_rating, shipper_comment, is_anonymous, created_at) VALUES
    (1, 101, 6, 1, 5, 'CÆ¡m ráº¥t ngon, sÆ°á»n má»m vá»«a miá»‡ng!', 10, 5, 'Shipper giao nhanh, nhiá»‡t tÃ¬nh.', FALSE, NOW() - INTERVAL '1 hour');

SELECT setval('orders.reviews_id_seq', (SELECT MAX(id) FROM orders.reviews));


-- ===========================================================
-- 9. TABLE: complaints
-- ===========================================================
CREATE TABLE IF NOT EXISTS orders.complaints (
                                                   id                      BIGSERIAL PRIMARY KEY,
                                                   order_id                BIGINT NOT NULL REFERENCES orders.orders(id),
    complainant_user_id     BIGINT,
    complainant_guest_id    BIGINT,
    reason_type             VARCHAR(50) NOT NULL,
    description             TEXT NOT NULL,
    image_urls              TEXT[] DEFAULT ARRAY[]::TEXT[],
    status                  VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    resolution              TEXT,
    resolved_by             BIGINT,
    resolved_at             TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

INSERT INTO orders.complaints (id, order_id, complainant_user_id, reason_type, description, status, created_at) VALUES
    (1, 101, 6, 'MISSING_ITEM', 'Thiáº¿u canh khá»• qua Ä‘Ã£ Ä‘áº·t kÃ¨m.', 'OPEN', NOW() - INTERVAL '10 minutes');

SELECT setval('orders.complaints_id_seq', (SELECT MAX(id) FROM orders.complaints));
