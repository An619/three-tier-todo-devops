# Killercoda Microservices Practice Guide

Use Killercoda first. This is a disposable learning environment; no AWS resources are created by the local steps.

## A. Run the complete microservices stack

Clone the migration branch:

git clone --branch feature/microservices-migration --single-branch https://github.com/An619/three-tier-todo-devops.git
cd three-tier-todo-devops

Copy .env.example to .env, replace all sample database passwords with unique values, and set JWT_SECRET to a strong random value of at least 32 characters. Keep .env out of Git.

docker compose config --quiet
docker compose up -d --build
docker compose ps

Open http://localhost:3000, register a new account, and create tasks. Nginx routes auth and task requests to separate services. The task API rejects unauthenticated requests and scopes task records to the authenticated user.

Useful diagnostics:

docker compose logs -f auth-service
docker compose logs -f task-service
docker compose logs -f frontend
docker compose down

Use docker compose down -v only when you intentionally want to delete local database data.

## B. Validate service security and ownership

The GitHub Actions microservices workflow performs the integration checks: registration, token issuance, rejecting an unauthenticated task request, creating a task, and verifying that a second account cannot read the first account's task. Re-run these tests after any changes to authentication or task ownership.

## C. Kubernetes / Helm

Chart: helm/microservices. It deploys frontend, auth-service, and task-service separately. The chart intentionally expects external auth/task databases and an external Kubernetes Secret; it does not install MySQL in Kubernetes.

Before installation:

1. Build and publish the three images with immutable tags.
2. Provision separate reachable databases for auth and tasks.
3. Set database.auth.url and database.task.url in a reviewed values file.
4. Create the Secret named by secrets.name (default taskmanager-microservices) with keys auth-db-password, task-db-password, and jwt-secret. The JWT secret must match for both services.
5. Replace image placeholders in values and run helm lint ./helm/microservices -f ./helm/microservices/values-argocd.yaml.
6. Review helm template output, then install or sync only after the external database endpoints and Secret exist.

Do not paste real secrets into Git or public chat. For a Killercoda-only experiment, use temporary random lab credentials and clean up all resources at the end.

## D. AWS caution

Do not create EKS just to validate this migration. EKS, EC2, NAT Gateway, EBS, ALB, public IPv4 addresses, and data transfer may incur charges. First prove the local Compose stack and Helm rendering; then make a cost/cleanup plan before using AWS.
