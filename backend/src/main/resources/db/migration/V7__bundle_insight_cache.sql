create table if not exists bundle_insights (
    id bigserial primary key,
    org_id varchar(64) not null,
    sku varchar(128) not null,
    related_sku varchar(128) not null,
    co_purchase_count bigint not null,
    rationale_text text,
    fingerprint varchar(255) not null,
    updated_at timestamptz not null default now(),
    unique (org_id, sku, related_sku)
);

create index if not exists idx_bundle_insights_org on bundle_insights (org_id);
