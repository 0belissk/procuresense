# Architecture Overview

## High-Level Diagram

```
[Angular SPA] --proxy--> [Spring Boot API] --JPA--> [PostgreSQL]
                                  |
                                CSV loader (demo data)
```

## Components

### Frontend (Angular 17)
- Standalone components using signals for simple state.
- `HealthService` sends `X-Org-Id`/`X-Role` headers and calls `/api/health` through `ng serve` proxy.
- Base layout + hero card communicates status for Sprint 0 smoke tests.

### Backend (Spring Boot 3)
- Layers: controllers (`/api/health`, `/api/purchases/*`), services (`DemoDataService`), repositories (JPA), entities (`Product`, `Purchase`).
- `IdentityHeaderInterceptor` enforces required headers for all `/api/**` routes.
- Demo data loader parses CSVs from `data/` and persists to PostgreSQL via JPA.
- Aggregations done with repository queries (`countDistinctOrders`, sums).

### Data & Infra
- PostgreSQL 15 via Docker Compose; credentials and URLs specified in `.env.example`.
- Demo CSVs live in `data/` and double as fixtures for tests.
- Future infra (cloud deploy, migrations) will extend `infra/` but is out of Sprint 0 scope.

## Cross-Cutting Concerns
- **Identity**: enforced centrally; tests and frontend supply headers via shared environment config.
- **Configuration**: `.env` + Spring profiles for dev/test; Angular environment files for frontend.
- **CI/CD**: `.github/workflows/ci.yml` runs Maven tests + Angular lint/build on PRs to `dev`/`main`.
- **Documentation**: `README`, API contract, ADRs capture decisions to help Sprint 1+ onboarding.

## Future Considerations
- Introduce migration tooling before schema becomes complex.
- Extend smoke tests to cover future endpoints and integrate with automated e2e.
- Harden security (authN/Z) beyond header enforcement.
