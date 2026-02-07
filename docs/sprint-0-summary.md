# Sprint 0 – Project Setup + Foundation

**Goal**: Stand up a clean, hackathon-friendly codebase so feature teams can start Sprint 1 without setup friction.

## What Shipped
- Monorepo scaffolding with Spring Boot backend, Angular frontend, infra + data folders.
- `/api/health`, `/api/purchases/demo/load`, `/api/purchases/summary` + identity header enforcement.
- Angular health dashboard that exercises `/api/health` with shared headers.
- Demo CSV assets (`data/demo_products.csv`, `data/demo_purchases.csv`) and loader service.
- `.env.example`, Docker Compose for PostgreSQL, README smoke-test checklist.
- API contract doc, ADR-0001, architecture overview, CI workflow, PR checklist.

## Verification Checklist
1. `docker compose -f infra/docker-compose.yml up -d` (PostgreSQL).
2. `cd backend && mvn test` then `mvn spring-boot:run`.
3. Smoke tests (from README):
   - `curl http://localhost:8080/api/health ...`
   - `curl -X POST http://localhost:8080/api/purchases/demo/load ...`
   - `curl http://localhost:8080/api/purchases/summary ...`
4. `cd frontend && npm install && npm start`; UI should show “Backend API is reachable”.
5. Push branch -> CI pipeline passes (backend tests + frontend lint/build).

## Lessons / Follow-ups
- Need a formal DB migration strategy (Flyway/Liquibase) before Sprint 2.
- Future stories should extend the API contract and smoke tests when endpoints change.
- Plan to add auth beyond header enforcement (OIDC) when the assistant flow arrives.
