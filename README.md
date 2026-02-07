# ProcureSense Sprint 0 Foundation

ProcureSense is a procurement intelligence assistant. Sprint 0 focuses on the plumbing so Sprint 1–3 feature work is productive. This repo holds the Angular frontend, Spring Boot backend, demo datasets, docs, and infra helpers.

## Repo Layout

```
backend/     # Spring Boot (Java 21) API
frontend/    # Angular 17 shell that calls /api/health
infra/       # Local Docker compose (PostgreSQL)
data/        # Demo CSV assets loaded by the backend
scripts/     # Helper scripts (reserved for future tooling)
docs/        # API contract and architecture notes
```

Additional documentation resources:
- `docs/architecture.md` – component diagram + cross-cutting concerns
- `docs/adr/0001-sprint0-foundation.md` – Sprint 0 architecture decisions
- `docs/sprint-0-summary.md` – sprint goal, deliverables, and verification checklist

## Requirements (Pinned)

| Tool | Version |
| --- | --- |
| Java | 21 (tested with Homebrew OpenJDK 21)
| Maven | 3.9+
| Node.js | 20.11+
| npm | 10+
| Docker | 24+

> ℹ️ The Angular app uses `npm` + `@angular/cli`. The backend relies on Maven without a wrapper due to offline scaffolding; install Maven locally or via CI (GitHub Action does this automatically).

## Initial Setup

1. Copy the sample environment file and fill in secrets (OpenAI key is backend-only).
   ```bash
   cp .env.example .env
   ```
2. Start PostgreSQL locally.
   ```bash
   docker compose -f infra/docker-compose.yml up -d
   ```
3. Install frontend dependencies (first run only).
   ```bash
   cd frontend
   npm install
   ```
4. Run the backend.
   ```bash
   cd backend
   mvn spring-boot:run
   ```
5. Run the Angular dev server (new terminal).
   ```bash
   cd frontend
   npm start
   ```

The frontend proxies `/api/*` to `http://localhost:8080` and automatically sends the demo identity headers (`X-Org-Id`, `X-Role`).

## Demo Data Workflow

Load curated demo products/purchases into PostgreSQL with a single command once the backend is running:

```bash
curl -X POST http://localhost:8080/api/purchases/demo/load \
  -H 'X-Org-Id: demo-org' \
  -H 'X-Role: admin'
```

The response returns `{ orgId, importedRows }` so you can confirm the load succeeded quickly. Use `GET /api/purchases/summary` to read the derived totals, date range, and `lastLoadedAt` timestamp for that org.

### Demo datasets

- `data/demo_products.csv` – `sku`, `name`, `category`, `unit_price`. This is the lookup table the backend uses to ensure bundle/reorder responses can render friendly product labels.
- `data/demo_purchases.csv` – `order_id`, `sku`, `quantity`, `unit_price`, `purchased_at`. Identity headers supply the org, so the CSV only carries immutable facts. The file purposely contains multi-item orders and at least five SKUs with repeated purchase intervals to feed the reorder + bundle heuristics.

Loading the demo data should yield `totalOrders=15`, `totalLineItems=27`, `totalQuantity=310`, `totalRevenue=7724.20`, `totalSkus=12`, and a `dateRange` of `2024-04-01T10:15:00Z` → `2024-04-22T16:45:00Z`. `lastLoadedAt` reflects when you last ran the loader/import for that org. If these numbers drift, the heuristics and unit tests will no longer match expectations.

## Smoke Test Checklist

Run these commands whenever you need to confirm the stack is alive:

```bash
# 1. Health endpoint
curl http://localhost:8080/api/health \
  -H 'X-Org-Id: demo-org' \
  -H 'X-Role: admin'

# 2. Load demo data
curl -X POST http://localhost:8080/api/purchases/demo/load \
  -H 'X-Org-Id: demo-org' \
  -H 'X-Role: admin'

# 3. Fetch purchase summary
curl http://localhost:8080/api/purchases/summary \
  -H 'X-Org-Id: demo-org' \
  -H 'X-Role: admin'
```

Expect the `totalOrders` field to be `15` after loading demo data.

## Uploading Purchase CSVs

Use the `/api/purchases/upload` endpoint when you have your own purchase history CSV. The `file` field must contain UTF-8 CSV data with these headers (case-insensitive): `order_id`, `sku`, `product_name`, `category`, `quantity`, `unit_price`, `purchased_at` (ISO-8601 timestamp).

```bash
curl -X POST http://localhost:8080/api/purchases/upload \
  -H 'X-Org-Id: demo-org' \
  -H 'X-Role: admin' \
  -F file=@purchases.csv
```

The response returns `{importedRows, rejectedRows, sampleErrors[]}` so you can confirm whether any rows failed validation without halting the whole import.

## Branching Strategy

- `main` – demo-stable branch; cut releases/tags from here only.
- `dev` – integration branch where feature branches merge via PR.
- Feature branches should be named `feature/<short-description>`.

Every PR must answer "What changed?" and "How was it tested?" via the PR template checklist.

## Identity + Secrets

- All API requests **must** send both `X-Org-Id` and `X-Role`. The backend rejects calls missing either header (health endpoints included).
- Never ship `OPENAI_API_KEY` or other secrets in the frontend bundle. Treat `.env` + Docker secrets as backend-only.
- Sprint 3 features call the OpenAI Responses API from the backend only. Set `OPENAI_API_KEY=<key>` and `OPENAI_ENABLED=true` in `.env` (or your shell) to turn on live explanations; otherwise the system returns deterministic fallback text so the demo remains reliable without network access.

## AI Explanations + Fallbacks

- Reorder predictions now include a short explanation sourced from OpenAI when enabled.
- The backend never asks AI to compute cadence/dates—those values are calculated in Java and passed to the prompt.
- When OpenAI is disabled or errors, ProcureSense emits a deterministic explanation highlighting the cadence, last purchase, predicted date, and confidence so the UI and judges always see informative text.

## Local Configuration Notes

- Spring Boot dev profile points to `jdbc:postgresql://localhost:5432/procuresense` with the `procure_local` user/password. Override via environment variables if needed.
- Demo CSVs live in `/data`. The backend references them by relative path so the same files feed tests and runtime.
- Frontend headers/URL live in `frontend/src/environments/`. Update values there if your setup differs.

## Next Steps

1. Flush out Sprint 1 stories on the `dev` branch using this foundation.
2. Extend the API contract docs (`docs/api-contract.md`) as new endpoints emerge.
3. Keep smoke tests in the README honest by updating values if datasets change.
4. Record new ADRs/sprint notes under `docs/` so decisions remain traceable.
