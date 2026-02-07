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

### Future Considerations

- All Sprint 1–3 endpoints must keep the `X-Org-Id`/`X-Role` contract.
- Use the summary payload as the canonical wrapper for analytics-style responses to stay consistent.
