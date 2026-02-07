alter table if exists reorder_insights
    alter column confidence type double precision using confidence::double precision;
