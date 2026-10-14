# Task Service

Independent Spring Boot task API exposing /api/tasks and actuator health endpoints. It validates JWTs using the shared secret configured for auth-service and stores an owner email on each new task. The owner is derived from the verified token, never from the request body.

## Run

From the repository root, copy .env.example to .env, set unique values for AUTH_DB_ROOT_PASSWORD, AUTH_DB_PASSWORD, TASK_DB_ROOT_PASSWORD, TASK_DB_PASSWORD, and a random JWT_SECRET of at least 32 characters, then run:

docker compose up --build

The UI is at http://localhost:3000. The task API is intentionally not published directly to the host; requests go through Nginx and require a bearer token. Existing legacy task rows without an owner are not returned to any account.
