-- ===========================================================
-- 1. Táº O SCHEMA
-- ===========================================================
CREATE SCHEMA IF NOT EXISTS auth;

-- ===========================================================
-- 2. TABLE: users (10 báº£n ghi máº«u)
-- ===========================================================
CREATE TABLE IF NOT EXISTS auth.users (
                                                  id                    BIGSERIAL PRIMARY KEY,
                                                  phone                 VARCHAR(15) NOT NULL UNIQUE,
    email                 VARCHAR(255) UNIQUE,
    full_name             VARCHAR(255),
    avatar_url            TEXT,
    password_hash         VARCHAR(255),
    role                  VARCHAR(30) NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    area_id               BIGINT,
    is_area_verified      BOOLEAN DEFAULT FALSE,
    default_address       JSONB,
    saved_addresses       JSONB DEFAULT '[]'::jsonb,
    google_id             VARCHAR(100) UNIQUE,
    facebook_id           VARCHAR(100) UNIQUE,
    refresh_token_hash    VARCHAR(255),
    last_login_at         TIMESTAMPTZ,
    failed_login_count    SMALLINT DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_users_phone ON auth.users(phone);
CREATE INDEX IF NOT EXISTS idx_users_role ON auth.users(role);
CREATE INDEX IF NOT EXISTS idx_users_area_id ON auth.users(area_id);

-- Seed 10 Users (Admin, Shop Manager, Shipper, Customer)
INSERT INTO auth.users (id, phone, email, full_name, avatar_url, password_hash, role, status, area_id, is_area_verified, default_address, saved_addresses, google_id, facebook_id) VALUES
                                                                                                                                                                                               (1, '0901234567', 'admin@platform.vn', 'Nguyá»…n Quáº£n Trá»‹', 'https://cdn.example.com/avatars/admin.jpg', '$2a$10$X...hash', 'ADMIN', 'ACTIVE', 1, TRUE, '{"label": "VÄƒn phÃ²ng", "building": "Trá»¥ sá»Ÿ chÃ­nh", "lat": 10.7769, "lng": 106.7009}'::jsonb, '[]'::jsonb, 'google_admin_123', NULL),
                                                                                                                                                                                               (2, '0902345678', 'shop1@gmail.com', 'Tráº§n VÄƒn QuÃ¡n', 'https://cdn.example.com/avatars/shop1.jpg', '$2a$10$X...hash', 'SHOP_MANAGER', 'ACTIVE', 1, TRUE, '{"label": "QuÃ¡n", "building": "TÃ²a A", "unit": "Shophouse 01", "lat": 10.7770, "lng": 106.7010}'::jsonb, '[]'::jsonb, NULL, NULL),
                                                                                                                                                                                               (3, '0903456789', 'shop2@gmail.com', 'LÃª Thá»‹ Báº¿p', 'https://cdn.example.com/avatars/shop2.jpg', '$2a$10$X...hash', 'SHOP_MANAGER', 'ACTIVE', 1, TRUE, '{"label": "QuÃ¡n", "building": "TÃ²a B", "unit": "Shophouse 05", "lat": 10.7780, "lng": 106.7020}'::jsonb, '[]'::jsonb, NULL, NULL),
                                                                                                                                                                                               (4, '0904567890', 'shipper1@gmail.com', 'Pháº¡m Giao HÃ ng', 'https://cdn.example.com/avatars/ship1.jpg', '$2a$10$X...hash', 'SHIPPER', 'ACTIVE', 1, TRUE, '{"label": "NhÃ  trá»", "building": "Khu dÃ¢n cÆ°", "lat": 10.7750, "lng": 106.6990}'::jsonb, '[]'::jsonb, NULL, NULL),
                                                                                                                                                                                               (5, '0905678901', 'shipper2@gmail.com', 'HoÃ ng Tá»‘c HÃ nh', 'https://cdn.example.com/avatars/ship2.jpg', '$2a$10$X...hash', 'SHIPPER', 'ACTIVE', 1, TRUE, '{"label": "NhÃ  trá»", "building": "Khu dÃ¢n cÆ°", "lat": 10.7755, "lng": 106.6995}'::jsonb, '[]'::jsonb, NULL, NULL),
                                                                                                                                                                                               (6, '0906789012', 'cust1@gmail.com', 'Nguyá»…n VÄƒn KhÃ¡ch', 'https://cdn.example.com/avatars/cust1.jpg', '$2a$10$X...hash', 'CUSTOMER', 'ACTIVE', 1, TRUE, '{"label": "NhÃ ", "building": "TÃ²a A", "unit": "A1201", "lat": 10.7769, "lng": 106.7009}'::jsonb, '[{"label": "CÃ´ng ty", "building": "TÃ²a Viettel", "unit": "P.502"}]'::jsonb, 'google_cust_1', 'fb_cust_1'),
                                                                                                                                                                                               (7, '0907890123', 'cust2@gmail.com', 'Tráº§n Thá»‹ Mua', 'https://cdn.example.com/avatars/cust2.jpg', '$2a$10$X...hash', 'CUSTOMER', 'ACTIVE', 1, TRUE, '{"label": "NhÃ ", "building": "TÃ²a B", "unit": "B0802", "lat": 10.7780, "lng": 106.7020}'::jsonb, '[]'::jsonb, NULL, NULL),
                                                                                                                                                                                               (8, '0908901234', 'cust3@gmail.com', 'LÃª HoÃ ng Sáº¯m', 'https://cdn.example.com/avatars/cust3.jpg', '$2a$10$X...hash', 'CUSTOMER', 'ACTIVE', 2, TRUE, '{"label": "NhÃ ", "building": "KÃ½ tÃºc xÃ¡ khu A", "unit": "P.304", "lat": 10.8800, "lng": 106.8000}'::jsonb, '[]'::jsonb, NULL, NULL),
                                                                                                                                                                                               (9, '0909012345', 'locked_user@gmail.com', 'VÅ© Vi Pháº¡m', NULL, '$2a$10$X...hash', 'CUSTOMER', 'LOCKED', 1, FALSE, NULL, '[]'::jsonb, NULL, NULL),
                                                                                                                                                                                               (10, '0900123456', 'pending_user@gmail.com', 'Äá»— Chá» XÃ¡c Thá»±c', NULL, '$2a$10$X...hash', 'CUSTOMER', 'PENDING', NULL, FALSE, NULL, '[]'::jsonb, NULL, NULL);

SELECT setval('auth.users_id_seq', (SELECT MAX(id) FROM auth.users));


-- ===========================================================
-- 3. TABLE: guest_sessions (10 báº£n ghi máº«u)
-- ===========================================================
CREATE TABLE IF NOT EXISTS auth.guest_sessions (
                                                           id                    BIGSERIAL PRIMARY KEY,
                                                           session_token         VARCHAR(64) NOT NULL UNIQUE,
    phone                 VARCHAR(15),
    is_phone_verified     BOOLEAN DEFAULT FALSE,
    order_count_today     SMALLINT DEFAULT 0,
    order_value_today     NUMERIC(12,2) DEFAULT 0,
    delivery_address      JSONB,
    upgraded_to_user_id   BIGINT,
    device_fingerprint    VARCHAR(255),
    ip_address            VARCHAR(45),
    expires_at            TIMESTAMPTZ NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_guest_sessions_token ON auth.guest_sessions(session_token);
CREATE INDEX IF NOT EXISTS idx_guest_sessions_phone ON auth.guest_sessions(phone);
CREATE INDEX IF NOT EXISTS idx_guest_sessions_expires ON auth.guest_sessions(expires_at);

INSERT INTO auth.guest_sessions (id, session_token, phone, is_phone_verified, order_count_today, order_value_today, delivery_address, upgraded_to_user_id, device_fingerprint, ip_address, expires_at) VALUES
                                                                                                                                                                                                                   (1, 'tok_guest_001_abcxyz', '0911111111', TRUE, 1, 45000.00, '{"building": "TÃ²a C", "unit": "C101", "lat": 10.7790, "lng": 106.7030}'::jsonb, NULL, 'fp_device_01', '192.168.1.10', NOW() + INTERVAL '24 hours'),
                                                                                                                                                                                                                   (2, 'tok_guest_002_abcxyz', '0922222222', FALSE, 0, 0.00, '{"building": "TÃ²a C", "unit": "C202", "lat": 10.7791, "lng": 106.7031}'::jsonb, NULL, 'fp_device_02', '192.168.1.11', NOW() + INTERVAL '24 hours'),
                                                                                                                                                                                                                   (3, 'tok_guest_003_abcxyz', '0933333333', TRUE, 2, 120000.00, '{"building": "TÃ²a A", "unit": "A303", "lat": 10.7769, "lng": 106.7009}'::jsonb, 6, 'fp_device_03', '192.168.1.12', NOW() - INTERVAL '2 hours'), -- ÄÃ£ upgrade sang user id 6
                                                                                                                                                                                                                   (4, 'tok_guest_004_abcxyz', NULL, FALSE, 0, 0.00, '{"building": "TÃ²a B", "unit": "B404", "lat": 10.7780, "lng": 106.7020}'::jsonb, NULL, 'fp_device_04', '192.168.1.13', NOW() + INTERVAL '24 hours'),
                                                                                                                                                                                                                   (5, 'tok_guest_005_abcxyz', '0944444444', TRUE, 1, 60000.00, '{"building": "Khu dÃ¢n cÆ°", "unit": "Kiosk 2", "lat": 10.7750, "lng": 106.6990}'::jsonb, NULL, 'fp_device_05', '192.168.1.14', NOW() + INTERVAL '24 hours'),
                                                                                                                                                                                                                   (6, 'tok_guest_006_abcxyz', NULL, FALSE, 0, 0.00, NULL, NULL, 'fp_device_06', '192.168.1.15', NOW() + INTERVAL '24 hours'),
                                                                                                                                                                                                                   (7, 'tok_guest_007_abcxyz', '0955555555', TRUE, 3, 210000.00, '{"building": "TÃ²a A", "unit": "A909", "lat": 10.7769, "lng": 106.7009}'::jsonb, NULL, 'fp_device_07', '192.168.1.16', NOW() + INTERVAL '24 hours'),
                                                                                                                                                                                                                   (8, 'tok_guest_008_abcxyz', NULL, FALSE, 0, 0.00, '{"building": "TÃ²a B", "unit": "B102", "lat": 10.7780, "lng": 106.7020}'::jsonb, NULL, 'fp_device_08', '192.168.1.17', NOW() + INTERVAL '24 hours'),
                                                                                                                                                                                                                   (9, 'tok_guest_009_abcxyz', '0966666666', TRUE, 1, 35000.00, '{"building": "TÃ²a C", "unit": "C505", "lat": 10.7790, "lng": 106.7030}'::jsonb, NULL, 'fp_device_09', '192.168.1.18', NOW() + INTERVAL '24 hours'),
                                                                                                                                                                                                                   (10, 'tok_guest_010_abcxyz', NULL, FALSE, 0, 0.00, NULL, NULL, 'fp_device_10', '192.168.1.19', NOW() + INTERVAL '24 hours');

SELECT setval('auth.guest_sessions_id_seq', (SELECT MAX(id) FROM auth.guest_sessions));


-- ===========================================================
-- 4. TABLE: otp_logs (10 báº£n ghi máº«u)
-- ===========================================================
CREATE TABLE IF NOT EXISTS auth.otp_logs (
                                                     id            BIGSERIAL PRIMARY KEY,
                                                     phone         VARCHAR(15) NOT NULL,
    otp_hash      VARCHAR(255) NOT NULL,
    purpose       VARCHAR(30) NOT NULL,
    is_used       BOOLEAN DEFAULT FALSE,
    attempts      SMALLINT DEFAULT 0,
    expires_at    TIMESTAMPTZ NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_otp_phone_purpose ON auth.otp_logs(phone, purpose);

INSERT INTO auth.otp_logs (id, phone, otp_hash, purpose, is_used, attempts, expires_at) VALUES
                                                                                                    (1, '0906789012', '$2a$10$hash_otp_1', 'LOGIN', TRUE, 0, NOW() + INTERVAL '5 minutes'),
                                                                                                    (2, '0907890123', '$2a$10$hash_otp_2', 'REGISTER', TRUE, 1, NOW() + INTERVAL '5 minutes'),
                                                                                                    (3, '0908901234', '$2a$10$hash_otp_3', 'RESET_PASSWORD', FALSE, 0, NOW() + INTERVAL '5 minutes'),
                                                                                                    (4, '0911111111', '$2a$10$hash_otp_4', 'GUEST_VERIFY', TRUE, 0, NOW() + INTERVAL '5 minutes'),
                                                                                                    (5, '0901234567', '$2a$10$hash_otp_5', 'LOGIN', FALSE, 2, NOW() + INTERVAL '2 minutes'),
                                                                                                    (6, '0904567890', '$2a$10$hash_otp_6', 'DELIVERY_CONFIRM', TRUE, 0, NOW() + INTERVAL '10 minutes'),
                                                                                                    (7, '0905678901', '$2a$10$hash_otp_7', 'DELIVERY_CONFIRM', FALSE, 0, NOW() + INTERVAL '10 minutes'),
                                                                                                    (8, '0900123456', '$2a$10$hash_otp_8', 'REGISTER', FALSE, 0, NOW() + INTERVAL '5 minutes'),
                                                                                                    (9, '0922222222', '$2a$10$hash_otp_9', 'GUEST_VERIFY', FALSE, 1, NOW() + INTERVAL '3 minutes'),
                                                                                                    (10, '0903456789', '$2a$10$hash_otp_10', 'LOGIN', TRUE, 0, NOW() + INTERVAL '5 minutes');

SELECT setval('auth.otp_logs_id_seq', (SELECT MAX(id) FROM auth.otp_logs));


-- ===========================================================
-- 5. TABLE: areas (5 báº£n ghi máº«u)
-- ===========================================================
CREATE TABLE IF NOT EXISTS auth.areas (
                                                  id                    BIGSERIAL PRIMARY KEY,
                                                  area_code             VARCHAR(30) NOT NULL UNIQUE,
    area_name             VARCHAR(255) NOT NULL,
    area_type             VARCHAR(20) NOT NULL,
    city                  VARCHAR(100),
    district              VARCHAR(100),
    address               TEXT,
    center_lat            DOUBLE PRECISION NOT NULL,
    center_lng            DOUBLE PRECISION NOT NULL,
    boundary_geojson      JSONB,
    radius_meters         INTEGER DEFAULT 500,
    auth_code             VARCHAR(20),
    shipper_model         VARCHAR(20) NOT NULL DEFAULT 'PLATFORM',
    is_active             BOOLEAN DEFAULT TRUE,
    created_by            BIGINT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

INSERT INTO auth.areas (id, area_code, area_name, area_type, city, district, address, center_lat, center_lng, boundary_geojson, radius_meters, auth_code, shipper_model, is_active, created_by) VALUES
                                                                                                                                                                                                            (1, 'CC_VINHOMES_Q9', 'Chung cÆ° Vinhomes Grand Park', 'CHUNG_CU', 'TP. Há»“ ChÃ­ Minh', 'ThÃ nh phá»‘ Thá»§ Äá»©c', 'Nguyen Xien, Long Thanh My', 10.8411, 106.8402, '{"type":"Polygon","coordinates":[[[106.83,10.83],[106.85,10.83],[106.85,10.85],[106.83,10.85],[106.83,10.83]]]}'::jsonb, 1000, 'VHM9', 'PLATFORM', TRUE, 1),
                                                                                                                                                                                                            (2, 'KTX_KHOA_HOC', 'KÃ½ tÃºc xÃ¡ ÄH Quá»‘c Gia Khu A', 'KY_TUC_XA', 'TP. Há»“ ChÃ­ Minh', 'ThÃ nh phá»‘ Thá»§ Äá»©c', 'ÄÃ´ng HÃ²a, DÄ© An', 10.8800, 106.8000, '{"type":"Polygon","coordinates":[[[106.79,10.87],[106.81,10.87],[106.81,10.89],[106.79,10.89],[106.79,10.87]]]}'::jsonb, 800, 'KTXA', 'HYBRID', TRUE, 1),
                                                                                                                                                                                                            (3, 'KCN_TAN_BINH', 'Khu cÃ´ng nghiá»‡p TÃ¢n BÃ¬nh', 'KHU_CN', 'TP. Há»“ ChÃ­ Minh', 'Quáº­n TÃ¢n PhÃº', 'TÃ¢y Tháº¡nh, TÃ¢n PhÃº', 10.8123, 106.6291, '{"type":"Polygon","coordinates":[[[106.62,10.80],[106.64,10.80],[106.64,10.82],[106.62,10.82],[106.62,10.80]]]}'::jsonb, 1500, 'TBIC', 'SHOP_OWN', TRUE, 1),
                                                                                                                                                                                                            (4, 'CC_MASTERI_AN_PHU', 'Chung cÆ° Masteri An PhÃº', 'CHUNG_CU', 'TP. Há»“ ChÃ­ Minh', 'ThÃ nh phá»‘ Thá»§ Äá»©c', 'Xa lá»™ HÃ  Ná»™i, An PhÃº', 10.8005, 106.7456, '{"type":"Polygon","coordinates":[[[106.74,10.79],[106.75,10.79],[106.75,10.81],[106.74,10.81],[106.74,10.79]]]}'::jsonb, 500, 'MSA1', 'PLATFORM', TRUE, 1),
                                                                                                                                                                                                            (5, 'VP_ESTELLA_HEIGHTS', 'TÃ²a nhÃ  VÄƒn phÃ²ng Estella', 'VAN_PHONG', 'TP. Há»“ ChÃ­ Minh', 'ThÃ nh phá»‘ Thá»§ Äá»©c', 'Song hÃ nh, An PhÃº', 10.7980, 106.7430, '{"type":"Polygon","coordinates":[[[106.73,10.79],[106.75,10.79],[106.75,10.80],[106.73,10.80],[106.73,10.79]]]}'::jsonb, 400, 'EST9', 'PLATFORM', TRUE, 1);

SELECT setval('auth.areas_id_seq', (SELECT MAX(id) FROM auth.areas));


-- ===========================================================
-- 6. TABLE: intra_zone_maps (10 báº£n ghi máº«u)
-- ===========================================================
CREATE TABLE IF NOT EXISTS auth.intra_zone_maps (
                                                            id            BIGSERIAL PRIMARY KEY,
                                                            area_id       BIGINT NOT NULL REFERENCES auth.areas(id) ON DELETE CASCADE,
    node_type     VARCHAR(20) NOT NULL,
    node_code     VARCHAR(50) NOT NULL,
    node_label    VARCHAR(255) NOT NULL,
    parent_id     BIGINT REFERENCES auth.intra_zone_maps(id),
    entry_lat     DOUBLE PRECISION,
    entry_lng     DOUBLE PRECISION,
    sort_order    SMALLINT DEFAULT 0,
    is_active     BOOLEAN DEFAULT TRUE,
    UNIQUE(area_id, node_code)
    );

CREATE INDEX IF NOT EXISTS idx_intra_zone_area ON auth.intra_zone_maps(area_id);
CREATE INDEX IF NOT EXISTS idx_intra_zone_parent ON auth.intra_zone_maps(parent_id);

INSERT INTO auth.intra_zone_maps (id, area_id, node_type, node_code, node_label, parent_id, entry_lat, entry_lng, sort_order, is_active) VALUES
                                                                                                                                                     (1, 1, 'BUILDING', 'TOA_A', 'TÃ²a S1.01', NULL, 10.8415, 106.8405, 1, TRUE),
                                                                                                                                                     (2, 1, 'FLOOR', 'TOA_A_TANG_15', 'Táº§ng 15', 1, 10.8415, 106.8405, 15, TRUE),
                                                                                                                                                     (3, 1, 'UNIT', 'TOA_A_P_1502', 'CÄƒn há»™ 15.02', 2, 10.8415, 106.8405, 2, TRUE),
                                                                                                                                                     (4, 1, 'GATE', 'CONG_CHINH', 'Cá»•ng chÃ­nh Vinhomes', NULL, 10.8400, 106.8390, 1, TRUE),
                                                                                                                                                     (5, 1, 'BUILDING', 'TOA_B', 'TÃ²a S1.02', NULL, 10.8420, 106.8410, 2, TRUE),
                                                                                                                                                     (6, 2, 'ZONE', 'KHU_A1', 'Khu nhÃ  A1', NULL, 10.8805, 106.8005, 1, TRUE),
                                                                                                                                                     (7, 2, 'ROOM', 'PHONG_304', 'PhÃ²ng 304', 6, 10.8805, 106.8005, 4, TRUE),
                                                                                                                                                     (8, 3, 'WORKSHOP', 'XUONG_B3', 'XÆ°á»Ÿng sáº£n xuáº¥t B3', NULL, 10.8125, 106.6295, 3, TRUE),
                                                                                                                                                     (9, 4, 'BUILDING', 'MASTERI_T1', 'ThÃ¡p T1 Masteri', NULL, 10.8008, 106.7458, 1, TRUE),
                                                                                                                                                     (10, 5, 'LANDMARK', 'HAM_GIO_XE', 'Háº§m giá»¯ xe tÃ²a nhÃ ', NULL, 10.7982, 106.7432, 1, TRUE);

SELECT setval('auth.intra_zone_maps_id_seq', (SELECT MAX(id) FROM auth.intra_zone_maps));


-- ===========================================================
-- 7. TABLE: shop_profiles (2 báº£n ghi máº«u cho 2 Shop Manager)
-- ===========================================================
CREATE TABLE IF NOT EXISTS auth.shop_profiles (
                                                          id                    BIGSERIAL PRIMARY KEY,
                                                          owner_id              BIGINT NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    area_id               BIGINT NOT NULL REFERENCES auth.areas(id),
    location_node_id      BIGINT REFERENCES auth.intra_zone_maps(id),
    location_detail       VARCHAR(255),
    shop_lat              DOUBLE PRECISION,
    shop_lng              DOUBLE PRECISION,
    shop_name             VARCHAR(255) NOT NULL,
    shop_description      TEXT,
    logo_url              TEXT,
    cover_image_url       TEXT,
    phone                 VARCHAR(15),
    business_hours        JSONB DEFAULT '[]'::jsonb,
    approval_status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason      TEXT,
    is_open               BOOLEAN DEFAULT FALSE,
    is_accepting_orders   BOOLEAN DEFAULT TRUE,
    commission_rate       NUMERIC(5,2),
    documents             JSONB DEFAULT '[]'::jsonb,
    approved_by           BIGINT REFERENCES auth.users(id),
    approved_at           TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_shop_profiles_owner ON auth.shop_profiles(owner_id);
CREATE INDEX IF NOT EXISTS idx_shop_profiles_area ON auth.shop_profiles(area_id);
CREATE INDEX IF NOT EXISTS idx_shop_profiles_status ON auth.shop_profiles(approval_status);

INSERT INTO auth.shop_profiles (id, owner_id, area_id, location_node_id, location_detail, shop_lat, shop_lng, shop_name, shop_description, logo_url, cover_image_url, phone, business_hours, approval_status, is_open, is_accepting_orders, commission_rate, documents, approved_by, approved_at) VALUES
                                                                                                                                                                                                                                                                                                              (1, 2, 1, 1, 'Shophouse TÃ²a S1.01', 10.8415, 106.8405, 'CÆ¡m Táº¥m PhÃºc Lá»™c Thá»', 'ChuyÃªn cÆ¡m táº¥m sÆ°á»n bÃ¬ cháº£ Ä‘áº­m vá»‹ truyá»n thá»‘ng', 'https://cdn.example.com/logos/comtam.jpg', 'https://cdn.example.com/covers/comtam.jpg', '0902345678', '[{"day": 1, "open": "06:00", "close": "21:00", "is_closed": false}]'::jsonb, 'APPROVED', TRUE, TRUE, 10.00, '["https://cdn.example.com/docs/gpkd1.jpg"]'::jsonb, 1, NOW()),
                                                                                                                                                                                                                                                                                                              (2, 3, 1, 5, 'Shophouse TÃ²a S1.02', 10.8420, 106.8410, 'Tea & Coffee Highland Mini', 'Thá»©c uá»‘ng ngon, giao nhanh táº­n cá»­a ná»™i khu', 'https://cdn.example.com/logos/coffee.jpg', 'https://cdn.example.com/covers/coffee.jpg', '0903456789', '[{"day": 1, "open": "07:00", "close": "22:00", "is_closed": false}]'::jsonb, 'APPROVED', TRUE, TRUE, 8.50, '["https://cdn.example.com/docs/gpkd2.jpg"]'::jsonb, 1, NOW());

SELECT setval('auth.shop_profiles_id_seq', (SELECT MAX(id) FROM auth.shop_profiles));


-- ===========================================================
-- 8. TABLE: shipper_profiles (2 báº£n ghi máº«u cho 2 Shipper)
-- ===========================================================
CREATE TABLE IF NOT EXISTS auth.shipper_profiles (
                                                             id                    BIGSERIAL PRIMARY KEY,
                                                             user_id               BIGINT NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    id_card_number        VARCHAR(20),
    vehicle_type          VARCHAR(20),
    vehicle_plate         VARCHAR(20),
    vehicle_photo_url     TEXT,
    registered_area_ids   BIGINT[] DEFAULT ARRAY[]::BIGINT[],
    approval_status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    avg_rating            NUMERIC(3,2) DEFAULT 5.00,
    total_deliveries      INTEGER DEFAULT 0,
    rejection_reason      TEXT,
    approved_by           BIGINT REFERENCES auth.users(id),
    approved_at           TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

INSERT INTO auth.shipper_profiles (id, user_id, id_card_number, vehicle_type, vehicle_plate, vehicle_photo_url, registered_area_ids, approval_status, avg_rating, total_deliveries, approved_by, approved_at) VALUES
                                                                                                                                                                                                                          (1, 4, '079203001234', 'MOTORBIKE', '59-S1 123.45', 'https://cdn.example.com/vehicles/ship1.jpg', ARRAY[1::BIGINT], 'APPROVED', 4.95, 150, 1, NOW()),
                                                                                                                                                                                                                          (2, 5, '079203005678', 'EBIKE', '59-MD 987.65', 'https://cdn.example.com/vehicles/ship2.jpg', ARRAY[1::BIGINT, 2::BIGINT], 'APPROVED', 4.88, 98, 1, NOW());

SELECT setval('auth.shipper_profiles_id_seq', (SELECT MAX(id) FROM auth.shipper_profiles));
