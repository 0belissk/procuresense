# ProcureSense API Contract (Sprint 3 Snapshot)

## Headers (required on all requests)
- `X-Org-Id`
- `X-Role`

## Reorder Predictions
- `GET /api/purchases/insights/reorders?limit=20`
- Response items include cadence, predicted reorder date, confidence, and `explanation` text.
- Explanations come from OpenAI when enabled and are cached in PostgreSQL (`reorder_insights`) per org + SKU so repeat requests reuse the same wording; deterministic fallback text is returned if OpenAI is disabled or fails.

## Demo Data
- `POST /api/purchases/demo/load` seeds the curated CSV for the acting org.

## Purchase Summary
- `GET /api/purchases/summary` returns aggregates (orders, SKUs, date range, etc.).

## Health
- `GET /api/health` echoes the service status so the frontend can show connectivity state.
