# ProcureSense API Contract (Sprint 3 Snapshot)

## Headers (required on all requests)
- `X-Org-Id`
- `X-Role`

## Reorder Predictions
- `GET /api/purchases/insights/reorders?limit=20`
- Response items include cadence, predicted reorder date, confidence, and `explanation` text.
- Explanations come from OpenAI when enabled and are cached in PostgreSQL (`reorder_insights`) per org + SKU so repeat requests reuse the same wording; deterministic fallback text is returned if OpenAI is disabled or fails.

## Bundle Recommendations
- `GET /api/purchases/insights/bundles/{sku}?limit=5`
- Response items now include a `rationale` string. The backend generates the rationale via OpenAI (when enabled) and caches it per org + SKU + related SKU in `bundle_insights`, falling back to deterministic text when AI is disabled or unavailable. Cache hits bypass OpenAI entirely so repeated demo clicks remain instant and reliable.

## Demo Data
- `POST /api/purchases/demo/load` seeds the curated CSV for the acting org.

## Purchase Summary
- `GET /api/purchases/summary` returns aggregates (orders, SKUs, date range, etc.).

## Assistant
- `POST /api/assistant/chat`
- Request: `{ message, context?: { selectedSku?, orgType?, projectType? } }`
- Response: `{ replyText, shoppingList: [{ sku, name, qty, reason }] }`
- Backend validates every SKU against the PostgreSQL catalog and always has a deterministic fallback kit so demos stay reliable even if OpenAI is down.

## Health
- `GET /api/health` echoes the service status so the frontend can show connectivity state.
