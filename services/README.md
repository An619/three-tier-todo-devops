# Microservices architecture and local run

## Implemented in this migration branch

- auth-service: independent Spring Boot service with its own MySQL database, BCrypt password hashing, registration/login, and expiring signed JWTs.
- task-service: independent Spring Boot service with its own MySQL database. Every task API route requires a valid bearer token and scopes reads/updates/deletes to the token subject (email).
- frontend: React login/registration calls the auth API; task API calls include the bearer token.
- Nginx reverse-proxy routing sends /api/auth/** to auth-service and /api/tasks/** to task-service.
- Compose uses separate internal database networks; database ports are not published to the host.
- Helm chart and Argo CD Application manifest are provided for independent service deployments.
- Jenkins and GitHub Actions build/test the auth and task services separately and build three immutable container images.

## Run locally

1. From the repository root, copy .env.example to .env.
2. Replace every sample password and JWT secret with strong, unique random values. JWT_SECRET must be at least 32 characters and must be identical for auth-service and task-service.
3. Start the stack: docker compose up -d --build.
4. Open http://localhost:3000, register an account, and create tasks.
5. Inspect logs with docker compose logs -f auth-service task-service.

To stop the stack, run docker compose down. To remove local database data too, run docker compose down -v.

## Remaining production work

This is a real service separation, but it is not automatically production-ready. The microservices Helm chart expects separately provisioned databases and an external Secret. Before production use, complete database migrations/backups, HTTPS and secure browser token handling (prefer HttpOnly secure cookies or a suitable OIDC flow over localStorage), rate limiting, security headers, request validation, integration/load testing, distributed tracing, alerting, and disaster recovery. No cloud resources have been created.
