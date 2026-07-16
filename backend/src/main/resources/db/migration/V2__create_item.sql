CREATE TYPE item_category AS ENUM (
    'ENGINE',
    'BRAKE',
    'CABLE',
    'ELECTRICAL',
    'BODY_FIBER',
    'LIGHTING',
    'CHAIN_SPROCKET',
    'BEARING',
    'FILTER',
    'BATTERY',
    'TYRE_TUBE',
    'HANDLE_CONTROL',
    'SUSPENSION',
    'LUBRICANTS',
    'ACCESSORIES',
    'OTHER'
);


CREATE TABLE item (

    item_id BIGSERIAL PRIMARY KEY,

    manufacturer_part_number VARCHAR(100) UNIQUE,

    item_name VARCHAR(150) NOT NULL,

    search_reference VARCHAR(100),

    item_category item_category NOT NULL,

    item_brand VARCHAR(100) NOT NULL,

    shelf_location VARCHAR(100),

    default_selling_price NUMERIC(10,2)
        NOT NULL
        DEFAULT 0.00
        CHECK (default_selling_price >= 0),

    minimum_stock INTEGER
        NOT NULL
        DEFAULT 0
        CHECK (minimum_stock >= 0),

    is_active BOOLEAN
        NOT NULL
        DEFAULT TRUE,

    created_at TIMESTAMPTZ
        NOT NULL
        DEFAULT NOW(),

    updated_at TIMESTAMPTZ
        NOT NULL
        DEFAULT NOW()
);