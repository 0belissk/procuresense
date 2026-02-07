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
# ProcureSense API Contract (Sprint 0)

All endpoints require the identity headers below and respond with JSON.

- `X-Org-Id` – logical tenant identifier (e.g., `demo-org`)
- `X-Role` – caller role (`admin`, `buyer`, etc.)

## Health

**GET `/api/health`**

Returns the service status so the frontend can render connectivity state.

```http
GET /api/health HTTP/1.1
X-Org-Id: demo-org
X-Role: admin
```

```json
{
  "status": "UP",
  "timestamp": "2024-04-12T17:21:53.381Z"
}
```

## Demo Purchases Loader

**POST `/api/purchases/demo/load`**

Loads the curated CSV datasets from `data/` into PostgreSQL. The endpoint clears purchases for the acting `X-Org-Id` before inserting demo data so repeated calls are idempotent for that org.

```http
POST /api/purchases/demo/load HTTP/1.1
X-Org-Id: demo-org
X-Role: admin
```

```json
{
  "orgId": "demo-org",
  "importedRows": 27
}
```

## Purchase CSV Upload

**POST `/api/purchases/upload`** (multipart form-data)

Uploads a purchase history CSV so the backend can normalize data into PostgreSQL. The uploaded `file` part must use UTF-8 CSV with the headers below (case-insensitive):

- `order_id`
- `sku`
- `product_name`
- `category`
- `quantity`
- `unit_price`
- `purchased_at` (ISO-8601 timestamp)

Sample request body:

```http
POST /api/purchases/upload HTTP/1.1
X-Org-Id: demo-org
X-Role: admin
Content-Type: multipart/form-data; boundary=---BOUNDARY

-----BOUNDARY
Content-Disposition: form-data; name="file"; filename="purchases.csv"
Content-Type: text/csv

order_id,sku,product_name,category,quantity,unit_price,purchased_at
ORD-1001,SKU-42,Smart Tape,Logistics,4,12.80,2024-02-01T08:00:00Z
...
-----BOUNDARY--
```

Sample response:

```json
{
  "importedRows": 42,
  "rejectedRows": 3,
  "sampleErrors": [
    "Row 5: quantity must be greater than zero"
  ]
}
```

Requests missing required columns or containing malformed data return HTTP 400 with an explanatory message.

## Purchase Summary

**GET `/api/purchases/summary`**

Provides a lightweight aggregate snapshot for dashboards or smoke tests. Response fields:

- `orgId` – echoes `X-Org-Id`.
- `totalOrders`, `totalLineItems`, `totalQuantity`, `totalRevenue` – aggregates of normalized purchases.
- `totalSkus` – count of distinct SKUs seen for the acting org.
- `dateRange.start/end` – earliest and latest `purchased_at` timestamps for that org.
- `lastLoadedAt` – timestamp when demo load or CSV upload last completed for the org.

```http
GET /api/purchases/summary HTTP/1.1
X-Org-Id: demo-org
X-Role: admin
```

```json
{
  "orgId": "demo-org",
  "totalOrders": 15,
  "totalLineItems": 27,
  "totalQuantity": 310,
  "totalRevenue": 7724.20,
  "totalSkus": 12,
  "dateRange": {
    "start": "2024-04-01T10:15:00Z",
    "end": "2024-04-22T16:45:00Z"
  },
  "lastLoadedAt": "2024-04-12T17:21:53.381Z"
}
```

## Reorder Predictions

**GET `/api/purchases/insights/reorders?limit=20`**

Returns deterministic reorder insights sorted by urgency for the acting org. Requires at least two purchases per SKU to appear. The backend computes the cadence and predicted date; confidence reflects interval stability. Each prediction now includes a short `explanation` string that comes from OpenAI (or a deterministic fallback when AI is disabled).

```http
GET /api/purchases/insights/reorders?limit=3 HTTP/1.1
X-Org-Id: demo-org
X-Role: buyer
```

```json
[
  {
    "orgId": "demo-org",
    "sku": "SKU-1001",
    "productName": "Eco Paper Towels",
    "lastPurchaseAt": "2024-04-10T09:45:00Z",
    "medianDaysBetween": 10,
    "predictedReorderAt": "2024-04-20T09:45:00Z",
    "confidence": 0.85,
    "explanation": "Eco Paper Towels usually need replenishment every 10 days; the last order was Apr 10 so plan for the next one near Apr 20 (5 days away, 85% confidence)."
  },
  {
    "orgId": "demo-org",
    "sku": "SKU-1012",
    "productName": "Warehouse Gloves (10)",
    "lastPurchaseAt": "2024-04-10T09:47:00Z",
    "medianDaysBetween": 8,
    "predictedReorderAt": "2024-04-18T09:47:00Z",
    "confidence": 0.72,
    "explanation": "Gloves are purchased roughly every 8 days; with the last shipment on Apr 10 the cadence points to Apr 18 (3 days out, 72% confidence)."
  }
]
```

If fewer than `limit` predictions exist, the endpoint returns all of them. An empty array indicates the org lacks sufficient purchase history.

## Bundle Recommendations

**GET `/api/purchases/insights/bundles/{sku}?limit=5`**

Returns the top co-purchased SKUs for the selected item, scoped to `X-Org-Id`. The backend counts how often the selected SKU appears in the same order (or same purchase day when order IDs are missing) with other SKUs.

```http
GET /api/purchases/insights/bundles/SKU-1001?limit=3 HTTP/1.1
X-Org-Id: demo-org
X-Role: buyer
```

```json
[
  {
    "orgId": "demo-org",
    "sku": "SKU-1001",
    "relatedSku": "SKU-1012",
    "relatedName": "Warehouse Gloves (10)",
    "coPurchaseCount": 4,
    "rationale": "Gloves and towels are pulled together for aisle resets."
  },
  {
    "orgId": "demo-org",
    "sku": "SKU-1001",
    "relatedSku": "SKU-1008",
    "relatedName": "Heavy Duty Tape",
    "coPurchaseCount": 3,
    "rationale": "Maintenance carts pair tape with wipes for return processing."
  }
]
```

An empty array indicates the org does not have enough co-purchase history for the requested SKU. The `limit` parameter caps how many related SKUs are returned.

### Future Considerations

- All Sprint 1–3 endpoints must keep the `X-Org-Id`/`X-Role` contract.
- Use the summary payload as the canonical wrapper for analytics-style responses to stay consistent.
