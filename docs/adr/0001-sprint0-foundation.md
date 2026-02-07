# ADR 0001 – Sprint 0 Architecture Foundation

Date: 2026-02-06

## Status
Accepted

## Context
We needed to unblock feature development by providing a minimal-yet-realistic stack that matches the hackathon product vision. The team agreed on:

- Backend in Java/Spring Boot to leverage existing experience, validation, and logging out of the box.
- Angular frontend so we can scaffold quickly with a strong component ecosystem.
- PostgreSQL as the source of truth starting on day one.
- Identity enforcement (`X-Org-Id`, `X-Role`) baked into every API call to avoid retrofitting auth semantics later.
- Demo data load endpoint to keep demos reliable even when CSV import stories are unfinished.

## Decision
1. **Monorepo structure**: keep `backend/`, `frontend/`, `infra/`, `data/`, `docs/`, `scripts/` at the repo root so every subteam lands in the same repo.
2. **Backend stack**: Spring Boot 3.3.x + Java 21 with Web, Validation, Data JPA, PostgreSQL driver. Identity headers enforced via MVC interceptor.
3. **Frontend stack**: Angular 17 (standalone components + signals) with dev proxy to the backend and health dashboard as the landing page.
4. **Data & infra**: Docker Compose-managed PostgreSQL (15-alpine) with schema managed via `ddl-auto=update` for Sprint 0, plus committed demo CSVs.
5. **CI/branching**: GitHub Actions workflow splitting backend/frontend jobs; `main` (demo) and `dev` (integration) branches.

## Consequences
- Future sprints must continue to honor the header contract; any new endpoint should extend `docs/api-contract.md`.
- Since we rely on JPA auto-DDL for Sprint 0, we need a migration strategy (Flyway/Liquibase) before production but not immediately.
- Angular/TypeScript versions are pinned; dependency upgrades should follow the PR checklist to avoid breaking CI.
- PostgreSQL must be running locally for backend startup and integration tests; README documents the workflow.
