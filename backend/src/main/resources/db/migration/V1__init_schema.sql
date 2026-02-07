CREATE TABLE IF NOT EXISTS products (
    id          BIGSERIAL PRIMARY KEY,
    sku         VARCHAR(255) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    category    VARCHAR(255),
    unit_price  NUMERIC(12,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS purchases (
    id            BIGSERIAL PRIMARY KEY,
    org_id        VARCHAR(100) NOT NULL,
    order_id      VARCHAR(255) NOT NULL,
    product_id    BIGINT NOT NULL REFERENCES products (id),
    quantity      INTEGER NOT NULL,
    unit_price    NUMERIC(12,2) NOT NULL,
    purchased_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_products_sku ON products (sku);
CREATE INDEX IF NOT EXISTS idx_purchases_org_product ON purchases (org_id, product_id);
CREATE INDEX IF NOT EXISTS idx_purchases_order_id ON purchases (order_id);
