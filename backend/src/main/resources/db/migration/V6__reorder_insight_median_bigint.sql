alter table if exists reorder_insights
    alter column median_days_between type bigint using median_days_between::bigint;
