# Killercoda free-practice deployment

This runbook is for the `main` branch of this repository. It does not require AWS. Do not paste Docker Hub passwords, access tokens, database passwords, or JWT secrets into GitHub, this chat, or a committed file.

## 0. Confirm GitHub CI before deploying

Open [GitHub Actions](https://github.com/An619/three-tier-todo-devops/actions). Deploy only after the latest **Validate project** and **CodeQL** runs for the intended `main` commit succeed. The microservices Helm chart expects external MySQL databases and a Kubernetes Secret; it does not create databases.

## 1. Start Killercoda and clone main

1. Open a Killercoda Linux/Docker scenario if available. A Kubernetes playground may not include the Docker CLI; check before choosing the build-and-push path.
2. Run:

```bash
git clone https://github.com/An619/three-tier-todo-devops.git
cd three-tier-todo-devops
git switch main
git pull --ff-only origin main
git rev-parse --short HEAD
docker version
docker compose version
```

If `docker version` or `docker compose version` is unavailable in the selected scenario, do not attempt Compose there. Use a Docker-enabled scenario or build/push the images from a machine with Docker, then use the Kubernetes playground for cluster work.

## 2. Smoke-test the microservices with Compose (if Docker is available)

Create local-only secrets; these values remain in the Killercoda session and must not be committed:

```bash
cp .env.example .env
sed -i "s|replace-with-long-random-auth-root-password|$(openssl rand -hex 24)|" .env
sed -i "s|replace-with-long-random-auth-app-password|$(openssl rand -hex 24)|" .env
sed -i "s|replace-with-long-random-task-root-password|$(openssl rand -hex 24)|" .env
sed -i "s|replace-with-long-random-task-app-password|$(openssl rand -hex 24)|" .env
sed -i "s|replace-with-random-secret-at-least-32-characters|$(openssl rand -hex 32)|" .env
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

Wait for both Spring Boot services and MySQL to become healthy. Then check the frontend:

```bash
curl -fsS http://localhost:3000/
docker compose logs --tail=100 auth-service task-service
```

If the scenario offers a web preview, expose port **3000** and open the preview. Register a test account in the UI and create a task. If the services are still starting, inspect `docker compose ps` and logs before retrying.

Stop the stack when finished:

```bash
docker compose down
```

To delete the practice databases too (irreversible for this local lab):

```bash
docker compose down -v
```

## 3. Publish immutable images to Docker Hub

Use a Docker-enabled terminal. First create these repositories in Docker Hub under your own account (public is simplest for a free Kubernetes lab):

- `three-tier-todo-frontend`
- `three-tier-todo-auth-service`
- `three-tier-todo-task-service`

Set your own Docker Hub username and a unique image tag. Use a commit SHA tag rather than `latest` for reproducible deployments:

```bash
export DOCKERHUB_USERNAME='YOUR_DOCKERHUB_USERNAME'
export IMAGE_TAG="$(git rev-parse --short HEAD)"
test "$DOCKERHUB_USERNAME" != 'YOUR_DOCKERHUB_USERNAME' || { echo 'Set DOCKERHUB_USERNAME first'; exit 1; }
docker login --username "$DOCKERHUB_USERNAME"
```

Run `docker login` interactively and enter a Docker Hub access token when prompted. Do not put the token in the command line or repository.

Build the three images from the repository root:

```bash
docker build -t "$DOCKERHUB_USERNAME/three-tier-todo-auth-service:$IMAGE_TAG" services/auth-service
docker build -t "$DOCKERHUB_USERNAME/three-tier-todo-task-service:$IMAGE_TAG" services/task-service
docker build -f frontend/Dockerfile.microservices -t "$DOCKERHUB_USERNAME/three-tier-todo-frontend:$IMAGE_TAG" .
```

Push them:

```bash
docker push "$DOCKERHUB_USERNAME/three-tier-todo-auth-service:$IMAGE_TAG"
docker push "$DOCKERHUB_USERNAME/three-tier-todo-task-service:$IMAGE_TAG"
docker push "$DOCKERHUB_USERNAME/three-tier-todo-frontend:$IMAGE_TAG"
```

Verify each repository/tag is visible in Docker Hub before referencing it in Kubernetes. If a push is denied, check that the repository exists, the username is correct, and the logged-in account has permission; do not switch to invented credentials.

## 4. Kubernetes next step — important limitation

The chart is at `helm/microservices`. It currently expects:

- Three published image names and the exact tag, configured in `helm/microservices/values.yaml`.
- Separate reachable auth and task MySQL databases (or deliberately configured database endpoints).
- A Kubernetes Secret named by `secrets.name`, with keys `auth-db-password`, `task-db-password`, and `jwt-secret`.
- The same strong JWT secret for both services.

Do **not** run `helm install` with the placeholder Docker Hub username/tag or with missing database/Secret values. The standard Killercoda Kubernetes playground is an ephemeral learning environment; the current chart does not provision MySQL. The next Kubernetes step is to prepare a clearly labelled demo-only MySQL setup for Killercoda, create the Secret interactively, set image repositories/tags, then render and validate the Helm chart before installing it.

## Troubleshooting checklist

```bash
docker compose ps
docker compose logs --tail=200
docker compose config --quiet
docker image ls
kubectl get nodes
kubectl get pods -A
```

Run only commands supported by the scenario you selected. A local Compose smoke test or successful image push is not the same as a verified Kubernetes deployment.
