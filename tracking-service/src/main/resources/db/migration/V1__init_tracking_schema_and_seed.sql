-- ===========================================================
-- 1. TẠO SCHEMA
-- ===========================================================
CREATE SCHEMA IF NOT EXISTS tracking_db;

-- ===========================================================
-- 2. TABLE: shipper_shifts
-- ===========================================================
CREATE TABLE IF NOT EXISTS tracking_db.shipper_shifts (
                                                          id                    BIGSERIAL PRIMARY KEY,
                                                          shipper_id            BIGINT NOT NULL,
                                                          area_id               BIGINT NOT NULL,
                                                          shift_type            VARCHAR(20),
    planned_start         TIMESTAMPTZ NOT NULL,
    planned_end           TIMESTAMPTZ NOT NULL,
    actual_start          TIMESTAMPTZ,
    actual_end            TIMESTAMPTZ,
    total_orders          SMALLINT DEFAULT 0,
    total_distance_m      INTEGER DEFAULT 0,
    total_earnings        NUMERIC(12,2) DEFAULT 0,
    status                VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_shifts_shipper ON tracking_db.shipper_shifts(shipper_id, status);
CREATE INDEX IF NOT EXISTS idx_shifts_area ON tracking_db.shipper_shifts(area_id, planned_start);

INSERT INTO tracking_db.shipper_shifts (id, shipper_id, area_id, shift_type, planned_start, planned_end, actual_start, total_orders, total_distance_m, total_earnings, status) VALUES
                                                                                                                                                                                   (1, 4, 1, 'MORNING', NOW() - INTERVAL '4 hours', NOW() + INTERVAL '2 hours', NOW() - INTERVAL '4 hours', 5, 12500, 150000.00, 'ACTIVE'),
                                                                                                                                                                                   (2, 5, 1, 'AFTERNOON', NOW() + INTERVAL '1 hour', NOW() + INTERVAL '7 hours', NULL, 0, 0, 0.00, 'SCHEDULED');

SELECT setval('tracking_db.shipper_shifts_id_seq', (SELECT MAX(id) FROM tracking_db.shipper_shifts));


-- ===========================================================
-- 3. TABLE: deliveries
-- ===========================================================
CREATE TABLE IF NOT EXISTS tracking_db.deliveries (
                                                      id                    BIGSERIAL PRIMARY KEY,
                                                      order_id              BIGINT NOT NULL UNIQUE,
                                                      area_id               BIGINT NOT NULL,
                                                      shipper_id            BIGINT,
                                                      shift_id              BIGINT REFERENCES tracking_db.shipper_shifts(id),
    pickup_lat            DOUBLE PRECISION NOT NULL,
    pickup_lng            DOUBLE PRECISION NOT NULL,
    pickup_address        TEXT NOT NULL,
    delivery_lat          DOUBLE PRECISION NOT NULL,
    delivery_lng          DOUBLE PRECISION NOT NULL,
    delivery_address      TEXT NOT NULL,
    pickup_node_id        BIGINT,
    delivery_node_id      BIGINT,
    delivery_building     VARCHAR(100),
    delivery_floor        VARCHAR(10),
    delivery_unit         VARCHAR(50),
    delivery_gate         VARCHAR(50),
    estimated_distance_m  INTEGER,
    estimated_duration_s  INTEGER,
    current_eta_minutes   SMALLINT,
    confirm_method        VARCHAR(20) NOT NULL DEFAULT 'OTP',
    delivery_otp          VARCHAR(10),
    proof_photo_url       TEXT,
    batch_id              BIGINT,
    batch_sequence        SMALLINT,
    cod_amount            NUMERIC(12,2),
    cod_collected         BOOLEAN DEFAULT FALSE,
    cod_collected_at      TIMESTAMPTZ,
    status                VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    failure_reason        TEXT,
    assigned_at           TIMESTAMPTZ,
    going_pickup_at       TIMESTAMPTZ,
    picked_up_at          TIMESTAMPTZ,
    delivered_at          TIMESTAMPTZ,
    failed_at             TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_deliveries_order ON tracking_db.deliveries(order_id);
CREATE INDEX IF NOT EXISTS idx_deliveries_shipper ON tracking_db.deliveries(shipper_id, status);
CREATE INDEX IF NOT EXISTS idx_deliveries_area ON tracking_db.deliveries(area_id, status);
CREATE INDEX IF NOT EXISTS idx_deliveries_batch ON tracking_db.deliveries(batch_id);

INSERT INTO tracking_db.deliveries (id, order_id, area_id, shipper_id, shift_id, pickup_lat, pickup_lng, pickup_address, delivery_lat, delivery_lng, delivery_address, delivery_building, delivery_floor, delivery_unit, estimated_distance_m, estimated_duration_s, current_eta_minutes, confirm_method, delivery_otp, cod_amount, status, assigned_at, going_pickup_at, picked_up_at) VALUES
                                                                                                                                                                                                                                                                                                                                                                                            (1, 101, 1, 4, 1, 10.8415, 106.8405, 'Shophouse Tòa S1.01', 10.7769, 106.7009, 'Tòa A, P.1201', 'Tòa A', '12', '1201', 1200, 300, 5, 'OTP', '123456', NULL, 'DELIVERING', NOW() - INTERVAL '20 minutes', NOW() - INTERVAL '15 minutes', NOW() - INTERVAL '10 minutes'),
                                                                                                                                                                                                                                                                                                                                                                                            (2, 102, 1, NULL, NULL, 10.8420, 106.8410, 'Shophouse Tòa S1.02', 10.7790, 106.7030, 'Tòa C, P.101', 'Tòa C', '1', '101', 800, 200, 10, 'OTP', '654321', 45000.00, 'PENDING', NULL, NULL, NULL);

SELECT setval('tracking_db.deliveries_id_seq', (SELECT MAX(id) FROM tracking_db.deliveries));


-- ===========================================================
-- 4. TABLE: live_locations
-- ===========================================================
CREATE TABLE IF NOT EXISTS tracking_db.live_locations (
                                                          shipper_id            BIGINT PRIMARY KEY,
                                                          delivery_id           BIGINT REFERENCES tracking_db.deliveries(id),
    lat                   DOUBLE PRECISION NOT NULL,
    lng                   DOUBLE PRECISION NOT NULL,
    accuracy_m            REAL,
    heading_deg           REAL,
    speed_ms              REAL,
    altitude_m            REAL,
    eta_to_pickup_s       INTEGER,
    eta_to_delivery_s     INTEGER,
    is_online             BOOLEAN NOT NULL DEFAULT TRUE,
    device_timestamp      TIMESTAMPTZ NOT NULL,
    server_received_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_live_locations_online ON tracking_db.live_locations(is_online) WHERE is_online = TRUE;

INSERT INTO tracking_db.live_locations (shipper_id, delivery_id, lat, lng, accuracy_m, heading_deg, speed_ms, altitude_m, eta_to_delivery_s, is_online, device_timestamp) VALUES
                                                                                                                                                                              (4, 1, 10.7800, 106.7100, 3.5, 90.0, 6.5, 15.0, 300, TRUE, NOW()),
                                                                                                                                                                              (5, NULL, 10.8410, 106.8400, 4.0, 0.0, 0.0, 0.0, NULL, TRUE, NOW() - INTERVAL '5 minutes');


-- ===========================================================
-- 5. TABLE: delivery_matching_logs
-- ===========================================================
CREATE TABLE IF NOT EXISTS tracking_db.delivery_matching_logs (
                                                                  id                    BIGSERIAL PRIMARY KEY,
                                                                  delivery_id           BIGINT NOT NULL REFERENCES tracking_db.deliveries(id),
    shipper_id            BIGINT NOT NULL,
    match_score           NUMERIC(5,2),
    distance_m            INTEGER,
    shipper_rating        NUMERIC(3,2),
    offered_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    response_at           TIMESTAMPTZ,
    response              VARCHAR(20),
    rejection_reason      VARCHAR(100)
    );

CREATE INDEX IF NOT EXISTS idx_matching_delivery ON tracking_db.delivery_matching_logs(delivery_id, offered_at);

INSERT INTO tracking_db.delivery_matching_logs (id, delivery_id, shipper_id, match_score, distance_m, shipper_rating, response, response_at) VALUES
    (1, 1, 4, 95.50, 150, 4.95, 'ACCEPTED', NOW() - INTERVAL '20 minutes');

SELECT setval('tracking_db.delivery_matching_logs_id_seq', (SELECT MAX(id) FROM tracking_db.delivery_matching_logs));


-- ===========================================================
-- 6. TABLE: shipper_incident_reports
-- ===========================================================
CREATE TABLE IF NOT EXISTS tracking_db.shipper_incident_reports (
                                                                    id                    BIGSERIAL PRIMARY KEY,
                                                                    delivery_id           BIGINT NOT NULL REFERENCES tracking_db.deliveries(id),
    shipper_id            BIGINT NOT NULL,
    incident_lat          DOUBLE PRECISION,
    incident_lng          DOUBLE PRECISION,
    incident_type         VARCHAR(50) NOT NULL,
    description           TEXT,
    photo_urls            TEXT[] DEFAULT ARRAY[]::TEXT[],
    resolved_by_admin     BIGINT,
    resolution            TEXT,
    resolved_at           TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

SELECT setval('tracking_db.shipper_incident_reports_id_seq', (SELECT MAX(id) FROM tracking_db.shipper_incident_reports));