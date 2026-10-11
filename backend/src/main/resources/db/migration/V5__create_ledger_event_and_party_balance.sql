CREATE TYPE ledger_event_type AS ENUM (
    'PURCHASE_RECEIVED',
    'SALE',
    'PAYMENT_MADE',
    'PAYMENT_RECEIVED'
);

CREATE TABLE ledger_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    party_id BIGINT NOT NULL
        REFERENCES party(party_id) ON DELETE RESTRICT,

    item_id BIGINT
        REFERENCES item(item_id) ON DELETE RESTRICT,

    event_type ledger_event_type NOT NULL,

    quantity INTEGER
        CHECK (quantity IS NULL OR quantity > 0),

    unit_price NUMERIC(10,2)
        CHECK (unit_price IS NULL OR unit_price >= 0),

    amount NUMERIC(12,2) NOT NULL,

    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    idempotency_key UUID NOT NULL UNIQUE,

    created_by VARCHAR(100) NOT NULL,

    notes TEXT
);

CREATE INDEX idx_ledger_event_party_id
    ON ledger_event (party_id, occurred_at);

CREATE INDEX idx_ledger_event_item_id
    ON ledger_event (item_id, occurred_at);

CREATE TABLE party_balance (
    party_id BIGINT PRIMARY KEY
        REFERENCES party(party_id) ON DELETE RESTRICT,

    balance NUMERIC(12,2) NOT NULL DEFAULT 0.00,

    version BIGINT NOT NULL DEFAULT 0,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO party_balance (party_id, balance)
SELECT party_id, 0.00
FROM party
ON CONFLICT (party_id) DO NOTHING;
