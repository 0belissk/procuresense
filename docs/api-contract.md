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

Loads the curated CSV datasets from `data/` into PostgreSQL. The endpoint resets current tables before inserting demo data.

```http
POST /api/purchases/demo/load HTTP/1.1
X-Org-Id: demo-org
X-Role: admin
```

```json
{
  "totalOrders": 9,
  "totalLineItems": 15,
  "totalQuantity": 162,
  "totalRevenue": 3918.53
}
```

## Purchase Summary

**GET `/api/purchases/summary`**

Provides a lightweight aggregate snapshot for dashboards or smoke tests.

```http
GET /api/purchases/summary HTTP/1.1
X-Org-Id: demo-org
X-Role: admin
```

```json
{
  "totalOrders": 9,
  "totalLineItems": 15,
  "totalQuantity": 162,
  "totalRevenue": 3918.53
}
```

### Future Considerations

- All Sprint 1–3 endpoints must keep the `X-Org-Id`/`X-Role` contract.
- Use the summary payload as the canonical wrapper for analytics-style responses to stay consistent.
