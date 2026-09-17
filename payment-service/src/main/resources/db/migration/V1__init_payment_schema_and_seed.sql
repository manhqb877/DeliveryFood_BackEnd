-- ===========================================================
-- 1. TẠO SCHEMA
-- ===========================================================
CREATE SCHEMA IF NOT EXISTS payment;

-- ===========================================================
-- 2. TABLE: transactions
-- ===========================================================
CREATE TABLE IF NOT EXISTS payment.transactions (
    id                     BIGSERIAL PRIMARY KEY,
    order_id               BIGINT NOT NULL,
    user_id                BIGINT,
    transaction_type       VARCHAR(20) NOT NULL,
    payment_gateway        VARCHAR(30),
    amount                 NUMERIC(12,2) NOT NULL,
    currency               VARCHAR(3) DEFAULT 'VND',
    status                 VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    gateway_transaction_id VARCHAR(255) UNIQUE,
    gateway_response       JSONB,
    idempotency_key        VARCHAR(64) UNIQUE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_transactions_order ON payment.transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_transactions_user ON payment.transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_gateway_id ON payment.transactions(gateway_transaction_id);

INSERT INTO payment.transactions (id, order_id, user_id, transaction_type, payment_gateway, amount, currency, status, gateway_transaction_id, gateway_response, idempotency_key) VALUES
(1, 101, 6, 'ORDER_PAYMENT', 'VNPAY', 95000.00, 'VND', 'SUCCESS', 'VNPAY_TXN_998877', '{"vnp_ResponseCode": "00", "vnp_BankCode": "NCB"}'::jsonb, 'idem_pay_001'),
(2, 102, NULL, 'ORDER_PAYMENT', 'COD', 45000.00, 'VND', 'PENDING', NULL, NULL, 'idem_pay_002');

SELECT setval('payment.transactions_id_seq', (SELECT MAX(id) FROM payment.transactions));


-- ===========================================================
-- 3. TABLE: wallets
-- ===========================================================
CREATE TABLE IF NOT EXISTS payment.wallets (
    id                    BIGSERIAL PRIMARY KEY,
    user_id               BIGINT NOT NULL UNIQUE,
    balance               NUMERIC(12,2) NOT NULL DEFAULT 0,
    credit_limit          NUMERIC(12,2) NOT NULL DEFAULT 0,
    status                VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_transaction_at   TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO payment.wallets (id, user_id, balance, credit_limit, status, last_transaction_at) VALUES
(1, 6, 500000.00, 1000000.00, 'ACTIVE', NOW() - INTERVAL '1 day'),
(2, 7, 200000.00, 500000.00, 'ACTIVE', NOW() - INTERVAL '2 days');

SELECT setval('payment.wallets_id_seq', (SELECT MAX(id) FROM payment.wallets));


-- ===========================================================
-- 4. TABLE: wallet_transactions
-- ===========================================================
CREATE TABLE IF NOT EXISTS payment.wallet_transactions (
    id                BIGSERIAL PRIMARY KEY,
    wallet_id         BIGINT NOT NULL REFERENCES payment.wallets(id),
    order_id          BIGINT,
    amount            NUMERIC(12,2) NOT NULL,
    balance_before    NUMERIC(12,2) NOT NULL,
    balance_after     NUMERIC(12,2) NOT NULL,
    tx_type           VARCHAR(20) NOT NULL,
    description       TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_wallet_tx_wallet ON payment.wallet_transactions(wallet_id, created_at DESC);

INSERT INTO payment.wallet_transactions (id, wallet_id, order_id, amount, balance_before, balance_after, tx_type, description) VALUES
(1, 1, 101, -95000.00, 595000.00, 500000.00, 'DEBIT', 'Thanh toán đơn hàng ORD-20260917-A1B2');

SELECT setval('payment.wallet_transactions_id_seq', (SELECT MAX(id) FROM payment.wallet_transactions));


-- ===========================================================
-- 5. TABLE: commission_configs
-- ===========================================================
CREATE TABLE IF NOT EXISTS payment.commission_configs (
    id                  BIGSERIAL PRIMARY KEY,
    shop_id             BIGINT,
    area_id             BIGINT,
    commission_type     VARCHAR(20) NOT NULL DEFAULT 'PERCENT',
    rate                NUMERIC(5,2) NOT NULL,
    valid_from          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    valid_until         TIMESTAMPTZ,
    created_by          BIGINT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO payment.commission_configs (id, shop_id, area_id, commission_type, rate, created_by) VALUES
(1, NULL, 1, 'PERCENT', 10.00, 1),
(2, 2, NULL, 'PERCENT', 8.50, 1);

SELECT setval('payment.commission_configs_id_seq', (SELECT MAX(id) FROM payment.commission_configs));


-- ===========================================================
-- 6. TABLE: cod_records
-- ===========================================================
CREATE TABLE IF NOT EXISTS payment.cod_records (
    id                 BIGSERIAL PRIMARY KEY,
    delivery_id        BIGINT NOT NULL UNIQUE,
    shipper_id         BIGINT NOT NULL,
    shop_id            BIGINT NOT NULL,
    amount             NUMERIC(12,2) NOT NULL,
    collected_at       TIMESTAMPTZ NOT NULL,
    reconcile_status   VARCHAR(20) DEFAULT 'PENDING',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO payment.cod_records (id, delivery_id, shipper_id, shop_id, amount, collected_at, reconcile_status) VALUES
(1, 2, 4, 2, 45000.00, NOW() - INTERVAL '30 minutes', 'PENDING');

SELECT setval('payment.cod_records_id_seq', (SELECT MAX(id) FROM payment.cod_records));