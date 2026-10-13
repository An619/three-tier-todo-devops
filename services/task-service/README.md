# Task Service

Extracted Spring Boot task API, currently exposing `/api/tasks` and actuator health endpoints.

## Run independently

From the repository root:

```bash
export MYSQL_ROOT_PASSWORD='change-this-local-root-password'
export MYSQL_APP_PASSWORD='change-this-local-app-password'
docker compose -f services/docker-compose.yml up --build
```

- Task API: http://localhost:8085/api/tasks
- Health: http://localhost:8085/actuator/health

The extraction preserves the legacy API contract to make the later frontend cutover safer.

## Important limitation

The extracted API does not yet enforce authenticated per-user ownership. Keep it local to a trusted development network only until the auth/token and ownership migration gates in `../README.md` are complete.
