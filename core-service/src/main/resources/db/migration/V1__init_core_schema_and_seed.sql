-- ===========================================================
-- 1. Táº O SCHEMA
-- ===========================================================
CREATE SCHEMA IF NOT EXISTS core;

-- ===========================================================
-- 2. TABLE: shops (5 báº£n ghi máº«u - Ä‘á»“ng bá»™ tá»« shop_profiles)
-- ===========================================================
CREATE TABLE IF NOT EXISTS core.shops (
                                             id                    BIGINT PRIMARY KEY,
                                             area_id               BIGINT NOT NULL,
                                             owner_id              BIGINT NOT NULL,
                                             shop_name             VARCHAR(255) NOT NULL,
    shop_description      TEXT,
    logo_url              TEXT,
    cover_image_url       TEXT,
    phone                 VARCHAR(15),
    shop_lat              DOUBLE PRECISION,
    shop_lng              DOUBLE PRECISION,
    location_detail       VARCHAR(255),
    business_hours        JSONB DEFAULT '[]'::jsonb,
    is_open               BOOLEAN DEFAULT FALSE,
    is_accepting_orders   BOOLEAN DEFAULT TRUE,
    avg_rating            NUMERIC(3,2) DEFAULT 0.00,
    total_reviews         INTEGER DEFAULT 0,
    avg_prep_time_minutes SMALLINT DEFAULT 15,
    is_active             BOOLEAN DEFAULT TRUE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_shops_area ON core.shops(area_id, is_active, is_open);
CREATE INDEX IF NOT EXISTS idx_shops_rating ON core.shops(avg_rating DESC);

INSERT INTO core.shops (id, area_id, owner_id, shop_name, shop_description, logo_url, cover_image_url, phone, shop_lat, shop_lng, location_detail, business_hours, is_open, is_accepting_orders, avg_rating, total_reviews, avg_prep_time_minutes, is_active) VALUES
                                                                                                                                                                                                                                                                     (1, 1, 2, 'CÆ¡m Táº¥m PhÃºc Lá»™c Thá»', 'ChuyÃªn cÆ¡m táº¥m sÆ°á»n bÃ¬ cháº£ Ä‘áº­m vá»‹ truyá»n thá»‘ng', 'https://cdn.example.com/logos/comtam.jpg', 'https://cdn.example.com/covers/comtam.jpg', '0902345678', 10.8415, 106.8405, 'Shophouse TÃ²a S1.01', '[{"day": 1, "open": "06:00", "close": "21:00", "is_closed": false}]'::jsonb, TRUE, TRUE, 4.85, 120, 15, TRUE),
                                                                                                                                                                                                                                                                     (2, 1, 3, 'Tea & Coffee Highland Mini', 'Thá»©c uá»‘ng ngon, giao nhanh táº­n cá»­a ná»™i khu', 'https://cdn.example.com/logos/coffee.jpg', 'https://cdn.example.com/covers/coffee.jpg', '0903456789', 10.8420, 106.8410, 'Shophouse TÃ²a S1.02', '[{"day": 1, "open": "07:00", "close": "22:00", "is_closed": false}]'::jsonb, TRUE, TRUE, 4.70, 85, 10, TRUE),
                                                                                                                                                                                                                                                                     (3, 1, 2, 'Phá»Ÿ BÃ² Gia Truyá»n Nam Äá»‹nh', 'Phá»Ÿ nÆ°á»›c dÃ¹ng thanh ngá»t, thá»‹t bÃ² má»m', 'https://cdn.example.com/logos/pho.jpg', 'https://cdn.example.com/covers/pho.jpg', '0902345679', 10.8416, 106.8406, 'Shophouse TÃ²a S1.01', '[{"day": 1, "open": "06:00", "close": "14:00", "is_closed": false}]'::jsonb, FALSE, TRUE, 4.90, 200, 12, TRUE),
                                                                                                                                                                                                                                                                     (4, 2, 3, 'BÃ¡nh MÃ¬ Káº¹p SÃ i GÃ²n', 'GiÃ²n rá»¥m, Ä‘áº§y áº¯p cháº£ lá»¥a thá»‹t nguá»™i', 'https://cdn.example.com/logos/banhmi.jpg', 'https://cdn.example.com/covers/banhmi.jpg', '0903456790', 10.8805, 106.8005, 'Khu KTX Khu A', '[{"day": 1, "open": "05:30", "close": "20:00", "is_closed": false}]'::jsonb, TRUE, TRUE, 4.65, 54, 5, TRUE),
                                                                                                                                                                                                                                                                     (5, 3, 2, 'BÃºn Cháº£ HÃ  Ná»™i XÆ°a', 'HÆ°Æ¡ng vá»‹ chuáº©n Báº¯c giá»¯a lÃ²ng SÃ i GÃ²n', 'https://cdn.example.com/logos/buncha.jpg', 'https://cdn.example.com/covers/buncha.jpg', '0902345680', 10.8125, 106.6295, 'Khu cÃ´ng nghiá»‡p TÃ¢n BÃ¬nh', '[{"day": 1, "open": "10:00", "close": "19:00", "is_closed": false}]'::jsonb, TRUE, TRUE, 4.78, 92, 15, TRUE);


-- ===========================================================
-- 3. TABLE: categories (5 báº£n ghi máº«u)
-- ===========================================================
CREATE TABLE IF NOT EXISTS core.categories (
                                                  id            BIGSERIAL PRIMARY KEY,
                                                  shop_id       BIGINT NOT NULL REFERENCES core.shops(id) ON DELETE CASCADE,
    name          VARCHAR(100) NOT NULL,
    description   TEXT,
    image_url     TEXT,
    sort_order    SMALLINT DEFAULT 0,
    is_active     BOOLEAN DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_categories_shop ON core.categories(shop_id, is_active);

INSERT INTO core.categories (id, shop_id, name, description, image_url, sort_order, is_active) VALUES
                                                                                                      (1, 1, 'CÆ¡m Pháº§n Äáº·c Biá»‡t', 'CÃ¡c mÃ³n cÆ¡m sÆ°á»n, bÃ¬, cháº£ Ä‘áº§y Ä‘áº·n', 'https://cdn.example.com/cats/com.jpg', 1, TRUE),
                                                                                                      (2, 1, 'MÃ³n ThÃªm & Canh', 'Canh khá»• qua, canh chua vÃ  Ä‘á»“ Äƒn kÃ¨m', 'https://cdn.example.com/cats/canh.jpg', 2, TRUE),
                                                                                                      (3, 2, 'CÃ  PhÃª & Espresso', 'CÃ  phÃª Ä‘en, sá»¯a, báº¡c sá»‰u Ä‘áº­m Ä‘Ã ', 'https://cdn.example.com/cats/coffee.jpg', 1, TRUE),
                                                                                                      (4, 2, 'TrÃ  Sá»¯a TrÃ¢n ChÃ¢u', 'TrÃ  sá»¯a thÆ¡m bÃ©o kÃ¨m topping', 'https://cdn.example.com/cats/milktea.jpg', 2, TRUE),
                                                                                                      (5, 3, 'Phá»Ÿ Truyá»n Thá»‘ng', 'Phá»Ÿ tÃ¡i, náº¡m, gÃ¢n, giÃ²n', 'https://cdn.example.com/cats/pho.jpg', 1, TRUE);

SELECT setval('core.categories_id_seq', (SELECT MAX(id) FROM core.categories));


-- ===========================================================
-- 4. TABLE: items (10 báº£n ghi máº«u)
-- ===========================================================
CREATE TABLE IF NOT EXISTS core.items (
                                             id                    BIGSERIAL PRIMARY KEY,
                                             shop_id               BIGINT NOT NULL REFERENCES core.shops(id) ON DELETE CASCADE,
    category_id           BIGINT REFERENCES core.categories(id),
    name                  VARCHAR(255) NOT NULL,
    description           TEXT,
    image_url             TEXT,
    base_price            NUMERIC(12,2) NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    daily_limit           INTEGER,
    daily_sold            INTEGER DEFAULT 0,
    prep_time_minutes     SMALLINT DEFAULT 10,
    tags                  VARCHAR(50)[] DEFAULT ARRAY[]::VARCHAR[],
    avg_rating            NUMERIC(3,2) DEFAULT 0.00,
    total_reviews         INTEGER DEFAULT 0,
    sort_order            SMALLINT DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_items_shop ON core.items(shop_id, status);
CREATE INDEX IF NOT EXISTS idx_items_category ON core.items(category_id);

INSERT INTO core.items (id, shop_id, category_id, name, description, image_url, base_price, status, daily_limit, daily_sold, prep_time_minutes, tags, avg_rating, total_reviews, sort_order) VALUES
                                                                                                                                                                                                    (1, 1, 1, 'CÆ¡m SÆ°á»n BÃ¬ Cháº£', 'SÆ°á»n nÆ°á»›ng thÆ¡m phá»©c káº¿t há»£p bÃ¬ cháº£ nhÃ  lÃ m', 'https://cdn.example.com/items/comsuon.jpg', 45000.00, 'AVAILABLE', 100, 25, 10, ARRAY['hot_deal', 'signature'], 4.90, 80, 1),
                                                                                                                                                                                                    (2, 1, 1, 'CÆ¡m ÄÃ¹i GÃ  NÆ°á»›ng Máº­t MÃ­a', 'ÄÃ¹i gÃ  Ä‘áº­m vá»‹, da giÃ²n thÆ¡m ngá»t', 'https://cdn.example.com/items/comga.jpg', 50000.00, 'AVAILABLE', 80, 15, 12, ARRAY['best_seller'], 4.80, 45, 2),
                                                                                                                                                                                                    (3, 1, 2, 'Canh Khá»• Qua Nhá»“i Thá»‹t', 'Thanh mÃ¡t, giáº£i nhiá»‡t mÃ¹a hÃ¨', 'https://cdn.example.com/items/canhkhoqua.jpg', 15000.00, 'AVAILABLE', 50, 10, 5, ARRAY['healthy'], 4.50, 15, 3),
                                                                                                                                                                                                    (4, 2, 3, 'CÃ  PhÃª Sá»¯a ÄÃ¡ SÃ i GÃ²n', 'Äáº­m Ä‘Ã  hÆ°Æ¡ng vá»‹ nguyÃªn báº£n', 'https://cdn.example.com/items/caphesua.jpg', 25000.00, 'AVAILABLE', NULL, 60, 5, ARRAY['coffee', 'hot_deal'], 4.85, 50, 1),
                                                                                                                                                                                                    (5, 2, 4, 'TrÃ  Sá»¯a TrÃ¢n ChÃ¢u HoÃ ng Gia', 'TrÃ  sá»¯a thÆ¡m ngon kÃ¨m trÃ¢n chÃ¢u dai giÃ²n', 'https://cdn.example.com/items/trasua.jpg', 35000.00, 'AVAILABLE', NULL, 40, 8, ARRAY['sweet'], 4.75, 30, 2),
                                                                                                                                                                                                    (6, 2, 3, 'Báº¡c Sá»‰u ÄÃ¡', 'Nhiá»u sá»¯a Ã­t Ä‘áº¯ng, ngá»t ngÃ o dá»… uá»‘ng', 'https://cdn.example.com/items/bacsiu.jpg', 28000.00, 'AVAILABLE', NULL, 30, 6, ARRAY['coffee'], 4.60, 20, 3),
                                                                                                                                                                                                    (7, 3, 5, 'Phá»Ÿ TÃ¡i BÃ² ViÃªn', 'BÃ¡nh phá»Ÿ má»m, thá»‹t tÃ¡i tÆ°Æ¡i ngon', 'https://cdn.example.com/items/photai.jpg', 55000.00, 'AVAILABLE', 70, 20, 10, ARRAY['signature'], 4.95, 110, 1),
                                                                                                                                                                                                    (8, 3, 5, 'Phá»Ÿ Äáº·c Biá»‡t Äáº§y Äá»§', 'TÃ¡i, náº¡m, gÃ¢n, gáº§u, bÃ² viÃªn ngáº­p trÃ n', 'https://cdn.example.com/items/phodacbiet.jpg', 65000.00, 'AVAILABLE', 50, 18, 12, ARRAY['hot_deal'], 4.98, 90, 2),
                                                                                                                                                                                                    (9, 4, NULL, 'BÃ¡nh MÃ¬ Cháº£ Lá»¥a Äáº·c Biá»‡t', 'Nhiá»u thá»‹t cháº£, bÆ¡ trá»©ng bÃ©o ngáº­y', 'https://cdn.example.com/items/banhmichala.jpg', 20000.00, 'AVAILABLE', 150, 50, 3, ARRAY['breakfast'], 4.70, 40, 1),
                                                                                                                                                                                                    (10, 5, NULL, 'BÃºn Cháº£ HÃ  Ná»™i Truyá»n Thá»‘ng', 'Cháº£ nÆ°á»›ng than hoa thÆ¡m lá»«ng', 'https://cdn.example.com/items/buncha.jpg', 45000.00, 'AVAILABLE', 90, 30, 12, ARRAY['lunch'], 4.88, 60, 1);

SELECT setval('core.items_id_seq', (SELECT MAX(id) FROM core.items));


-- ===========================================================
-- 5. TABLE: item_prices (5 báº£n ghi máº«u cho báº£ng giÃ¡ theo thá»i Ä‘iá»ƒm)
-- ===========================================================
CREATE TABLE IF NOT EXISTS core.item_prices (
                                                   id                BIGSERIAL PRIMARY KEY,
                                                   item_id           BIGINT NOT NULL REFERENCES core.items(id) ON DELETE CASCADE,
    price_name        VARCHAR(100) NOT NULL,
    price             NUMERIC(12,2) NOT NULL,
    price_type        VARCHAR(30) NOT NULL,
    applicable_days   SMALLINT[],
    time_start        TIME,
    time_end          TIME,
    valid_from        DATE,
    valid_until       DATE,
    priority          SMALLINT NOT NULL DEFAULT 0,
    is_active         BOOLEAN DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_item_prices_item ON core.item_prices(item_id, is_active);
CREATE INDEX IF NOT EXISTS idx_item_prices_type ON core.item_prices(price_type, is_active);
CREATE INDEX IF NOT EXISTS idx_item_prices_priority ON core.item_prices(item_id, priority DESC);

INSERT INTO core.item_prices (id, item_id, price_name, price, price_type, applicable_days, time_start, time_end, valid_from, valid_until, priority, is_active) VALUES
                                                                                                                                                                      (1, 1, 'GiÃ¡ Giá» VÃ ng TrÆ°a', 48000.00, 'PEAK_HOUR', ARRAY[1,2,3,4,5]::SMALLINT[], '11:00:00', '13:30:00', NULL, NULL, 10, TRUE),
                                                                                                                                                                      (2, 4, 'Giáº£m GiÃ¡ Happy Hour Chiá»u', 22000.00, 'HAPPY_HOUR', NULL, '14:00:00', '16:00:00', NULL, NULL, 5, TRUE),
                                                                                                                                                                      (3, 7, 'GiÃ¡ Cuá»‘i Tuáº§n', 58000.00, 'WEEKEND', ARRAY[0,6]::SMALLINT[], NULL, NULL, NULL, NULL, 8, TRUE),
                                                                                                                                                                      (4, 2, 'GiÃ¡ Khuyáº¿n MÃ£i Äáº·c Biá»‡t', 45000.00, 'PROMOTION', NULL, NULL, NULL, '2026-04-01', '2026-04-30', 15, TRUE),
                                                                                                                                                                      (5, 5, 'GiÃ¡ Giá» ÄÃªm', 32000.00, 'LATE_NIGHT', NULL, '22:00:00', '23:59:00', NULL, NULL, 6, TRUE);

SELECT setval('core.item_prices_id_seq', (SELECT MAX(id) FROM core.item_prices));


-- ===========================================================
-- 6. TABLE: item_options (5 báº£n ghi máº«u)
-- ===========================================================
CREATE TABLE IF NOT EXISTS core.item_options (
                                                    id            BIGSERIAL PRIMARY KEY,
                                                    item_id       BIGINT NOT NULL REFERENCES core.items(id) ON DELETE CASCADE,
    group_name    VARCHAR(100) NOT NULL,
    option_name   VARCHAR(100) NOT NULL,
    extra_price   NUMERIC(10,2) NOT NULL DEFAULT 0,
    is_required   BOOLEAN DEFAULT FALSE,
    is_multiple   BOOLEAN DEFAULT FALSE,
    max_select    SMALLINT DEFAULT 1,
    is_active     BOOLEAN DEFAULT TRUE,
    sort_order    SMALLINT DEFAULT 0
    );

CREATE INDEX IF NOT EXISTS idx_item_options_item ON core.item_options(item_id, is_active);

INSERT INTO core.item_options (id, item_id, group_name, option_name, extra_price, is_required, is_multiple, max_select, is_active, sort_order) VALUES
                                                                                                                                                      (1, 1, 'Topping ThÃªm', 'ThÃªm trá»©ng á»‘p la', 10000.00, FALSE, TRUE, 2, TRUE, 1),
                                                                                                                                                      (2, 1, 'Topping ThÃªm', 'ThÃªm sÆ°á»n miáº¿ng nhá»', 20000.00, FALSE, TRUE, 1, TRUE, 2),
                                                                                                                                                      (3, 4, 'Äá»™ Ngá»t', 'Ãt Ä‘Æ°á»ng (50%)', 0.00, TRUE, FALSE, 1, TRUE, 1),
                                                                                                                                                      (4, 4, 'Äá»™ Ngá»t', 'KhÃ´ng Ä‘Æ°á»ng', 0.00, TRUE, FALSE, 1, TRUE, 2),
                                                                                                                                                      (5, 5, 'Topping TrÃ  Sá»¯a', 'ThÃªm trÃ¢n chÃ¢u Ä‘en', 5000.00, FALSE, TRUE, 3, TRUE, 1);

SELECT setval('core.item_options_id_seq', (SELECT MAX(id) FROM core.item_options));


-- ===========================================================
-- 7. TABLE: promotions (5 báº£n ghi máº«u)
-- ===========================================================
CREATE TABLE IF NOT EXISTS core.promotions (
                                                  id                    BIGSERIAL PRIMARY KEY,
                                                  code                  VARCHAR(50) NOT NULL UNIQUE,
    promo_type            VARCHAR(20) NOT NULL,
    scope                 VARCHAR(20) NOT NULL,
    shop_id               BIGINT REFERENCES core.shops(id),
    area_id               BIGINT,
    discount_value        NUMERIC(12,2),
    min_order_value       NUMERIC(12,2) DEFAULT 0,
    max_discount_amount   NUMERIC(12,2),
    total_limit           INTEGER,
    per_user_limit        SMALLINT DEFAULT 1,
    used_count            INTEGER DEFAULT 0,
    applicable_to         VARCHAR(20) DEFAULT 'ALL',
    valid_from            TIMESTAMPTZ NOT NULL,
    valid_until           TIMESTAMPTZ NOT NULL,
    approval_status       VARCHAR(20) DEFAULT 'APPROVED',
    is_active             BOOLEAN DEFAULT TRUE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_promotions_code ON core.promotions(code);
CREATE INDEX IF NOT EXISTS idx_promotions_active ON core.promotions(scope, is_active, valid_until);

INSERT INTO core.promotions (id, code, promo_type, scope, shop_id, area_id, discount_value, min_order_value, max_discount_amount, total_limit, per_user_limit, used_count, applicable_to, valid_from, valid_until, approval_status, is_active) VALUES
                                                                                                                                                                                                                                                      (1, 'CHUNGCU10', 'PERCENT', 'AREA', NULL, 1, 10.00, 50000.00, 20000.00, 500, 1, 45, 'RESIDENT', NOW() - INTERVAL '5 days', NOW() + INTERVAL '25 days', 'APPROVED', TRUE),
                                                                                                                                                                                                                                                      (2, 'NEWUSER50K', 'FIXED_AMOUNT', 'PLATFORM', NULL, NULL, 50000.00, 100000.00, NULL, 1000, 1, 120, 'NEW_USER', NOW() - INTERVAL '10 days', NOW() + INTERVAL '50 days', 'APPROVED', TRUE),
                                                                                                                                                                                                                                                      (3, 'FREESHIPVHM', 'FREE_DELIVERY', 'AREA', NULL, 1, 15000.00, 40000.00, NULL, 300, 2, 85, 'ALL', NOW() - INTERVAL '2 days', NOW() + INTERVAL '10 days', 'APPROVED', TRUE),
                                                                                                                                                                                                                                                      (4, 'SHOPCOFFEE15', 'PERCENT', 'SHOP', 2, NULL, 15.00, 30000.00, 15000.00, 200, 1, 30, 'CUSTOMER', NOW() - INTERVAL '1 day', NOW() + INTERVAL '15 days', 'APPROVED', TRUE),
                                                                                                                                                                                                                                                      (5, 'GIAM20K', 'FIXED_AMOUNT', 'PLATFORM', NULL, NULL, 20000.00, 80000.00, NULL, 500, 1, 210, 'ALL', NOW() - INTERVAL '15 days', NOW() + INTERVAL '5 days', 'APPROVED', TRUE);

SELECT setval('core.promotions_id_seq', (SELECT MAX(id) FROM core.promotions));


-- ===========================================================
-- 8. TABLE: promotion_redemptions (5 báº£n ghi máº«u)
-- ===========================================================
CREATE TABLE IF NOT EXISTS core.promotion_redemptions (
                                                             id                  BIGSERIAL PRIMARY KEY,
                                                             promotion_id        BIGINT NOT NULL REFERENCES core.promotions(id),
    user_id             BIGINT,
    guest_session_id    BIGINT,
    order_id            BIGINT,
    discount_applied    NUMERIC(12,2) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'USED',
    idempotency_key     VARCHAR(64) UNIQUE,
    locked_until        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE UNIQUE INDEX IF NOT EXISTS idx_promo_user_unique ON core.promotion_redemptions(promotion_id, user_id)
    WHERE user_id IS NOT NULL AND status = 'USED';
CREATE INDEX IF NOT EXISTS idx_promo_order ON core.promotion_redemptions(order_id);

INSERT INTO core.promotion_redemptions (id, promotion_id, user_id, guest_session_id, order_id, discount_applied, status, idempotency_key, locked_until) VALUES
                                                                                                                                                               (1, 1, 6, NULL, 101, 15000.00, 'USED', 'idem_key_001_abc', NULL),
                                                                                                                                                               (2, 2, 7, NULL, 102, 50000.00, 'USED', 'idem_key_002_abc', NULL),
                                                                                                                                                               (3, 3, NULL, 1, 103, 15000.00, 'USED', 'idem_key_003_abc', NULL),
                                                                                                                                                               (4, 4, 6, NULL, 104, 10000.00, 'USED', 'idem_key_004_abc', NULL),
                                                                                                                                                               (5, 5, 8, NULL, 105, 20000.00, 'LOCKED', 'idem_key_005_abc', NOW() + INTERVAL '15 minutes');

SELECT setval('core.promotion_redemptions_id_seq', (SELECT MAX(id) FROM core.promotion_redemptions));
