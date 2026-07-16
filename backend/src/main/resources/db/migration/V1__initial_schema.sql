CREATE TYPE party_type AS ENUM (
    'SUPPLIER',
    'GARAGE'
);

CREATE TABLE party (
    party_id BIGSERIAL PRIMARY KEY,

    party_name VARCHAR(150) NOT NULL,

    party_type party_type NOT NULL,

    phone_number VARCHAR(15),

    gst_number VARCHAR(15),

    default_discount_percentage NUMERIC(5,2) NOT NULL DEFAULT 0.00,

    address TEXT,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);