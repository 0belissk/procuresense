ALTER TABLE purchases
    ADD COLUMN IF NOT EXISTS org_id VARCHAR(100);

UPDATE purchases
SET org_id = COALESCE(org_id, 'demo-org-a')
WHERE org_id IS NULL;

ALTER TABLE purchases
    ALTER COLUMN org_id SET NOT NULL;
