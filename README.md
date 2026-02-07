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

The response returns total orders, line items, quantity, and revenue. Use `GET /api/purchases/summary` for a read-only check.

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

Expect the `totalOrders` field to be `9` after loading demo data.

## Branching Strategy

- `main` – demo-stable branch; cut releases/tags from here only.
- `dev` – integration branch where feature branches merge via PR.
- Feature branches should be named `feature/<short-description>`.

Every PR must answer "What changed?" and "How was it tested?" via the PR template checklist.

## Identity + Secrets

- All API requests **must** send both `X-Org-Id` and `X-Role`. The backend rejects calls missing either header (health endpoints included).
- Never ship `OPENAI_API_KEY` or other secrets in the frontend bundle. Treat `.env` + Docker secrets as backend-only.

## Local Configuration Notes

- Spring Boot dev profile points to `jdbc:postgresql://localhost:5432/procuresense` with the `procure_local` user/password. Override via environment variables if needed.
- Demo CSVs live in `/data`. The backend references them by relative path so the same files feed tests and runtime.
- Frontend headers/URL live in `frontend/src/environments/`. Update values there if your setup differs.

## Next Steps

1. Flush out Sprint 1 stories on the `dev` branch using this foundation.
2. Extend the API contract docs (`docs/api-contract.md`) as new endpoints emerge.
3. Keep smoke tests in the README honest by updating values if datasets change.
