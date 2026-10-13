# Microservices migration

This directory starts the controlled extraction from the original three-tier Task Manager application.

## Current state

- `task-service/` is an independently buildable Spring Boot service extracted from the former `backend/` module. It owns the task CRUD API and its own database connection configuration.
- The original `backend/` remains temporarily for backwards compatibility with the existing UI, Compose smoke test, and Helm chart.
- This is **migration phase 1**, not a claim that the whole application is already microservices.

## Target service boundaries

| Service | Responsibility | Data ownership |
|---|---|---|
| frontend | React UI; no business persistence | none |
| API gateway | one external entry point, routing, rate limits | none |
| auth-service | registration, login, password hashing, token issuance/validation | identity database |
| task-service | task CRUD and task authorization | task database |
| notification-service | consume task lifecycle events and deliver notifications | notification/outbox data if needed |

Services should not read or write another service's database. Cross-service communication should use HTTP APIs or versioned events, not shared tables.

## Migration gates before calling this complete

1. Implement and test server-side auth with hashed passwords and signed, expiring tokens.
2. Enforce token validation and per-user task ownership in `task-service`.
3. Switch the frontend from localStorage-based password storage and sample-data fallback to the service APIs.
4. Add a gateway and an event-driven notification service only where the product has a real notification use case.
5. Update Compose, Kubernetes/Helm, Argo CD, Jenkins and CI to build, scan, test and deploy each service independently.
6. Run integration tests proving one user's tasks cannot be read or modified by another user; test service outage, rollback and data migration.

Do not deploy this migration to real users until those gates pass. No cloud resources are created by these files.
