# Microservices migration

## Phase 2 status

- `auth-service`: independent Spring Boot service with separate MySQL database, BCrypt password hashing, registration/login and expiring signed JWTs.
- `task-service`: independent Spring Boot service with a separate database. Task API requires a bearer token and scopes CRUD operations by token subject (email).
- `frontend`: registration/login now use the auth API; task API calls send the bearer token.
- Nginx provides same-origin gateway-style routing for `/api/auth/**` and `/api/tasks/**`.
- `services/docker-compose.yml` runs both services with separate private data networks/databases.

This is a meaningful migration phase, but not yet a complete production-grade microservices system: event-driven notifications/message broker, dedicated gateway with rate limiting, Kubernetes/Helm/Argo CD per-service release config, observability correlation/tracing, data migration, and comprehensive security/integration tests remain.

## Run locally

1. From this directory, copy `.env.example` to `.env`.
2. Replace every example password and generate a random `JWT_SECRET` of at least 32 characters.
3. Run from the repository root: `docker compose --env-file services/.env -f services/docker-compose.yml up --build`.
4. Open `http://localhost:3000`, register a new account, then create tasks.

The task API does not trust user IDs from request bodies; ownership comes from the verified JWT subject. Existing legacy task rows with no owner are intentionally not exposed to any user. No cloud resources are created by this Compose setup.
