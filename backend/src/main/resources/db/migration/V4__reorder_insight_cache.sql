create table if not exists reorder_insights (
    id bigserial primary key,
    org_id varchar(64) not null,
    sku varchar(128) not null,
    last_purchase_at timestamptz not null,
    predicted_reorder_at timestamptz not null,
    median_days_between integer not null,
    confidence numeric(5,2) not null,
    explanation_text text,
    fingerprint varchar(255) not null,
    updated_at timestamptz not null default now(),
    unique (org_id, sku)
);

create index if not exists idx_reorder_insights_org on reorder_insights (org_id);
