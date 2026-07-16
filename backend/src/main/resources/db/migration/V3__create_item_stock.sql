CREATE TABLE item_stock (

    item_id BIGINT PRIMARY KEY,

    available_quantity INTEGER
        NOT NULL
        DEFAULT 0
        CHECK (available_quantity >= 0),

    last_stock_updated_at TIMESTAMPTZ
        NOT NULL
        DEFAULT NOW(),

    CONSTRAINT fk_item_stock_item
        FOREIGN KEY (item_id)
        REFERENCES item(item_id)
        ON DELETE RESTRICT
);