# Killercoda Practice Guide

This is the first target environment. It is intentionally disposable; the EKS values and cloud resources are a later phase.

## A. Verify the repository and build images

Clone the upgrade branch:

```bash
git clone --branch upgrade/production-ready-devops --single-branch https://github.com/An619/three-tier-todo-devops.git
cd three-tier-todo-devops
```

Run the local three-tier stack first:

```bash
cp .env.example .env
# Edit .env and set unique values for MYSQL_ROOT_PASSWORD and MYSQL_APP_PASSWORD
docker-compose config
docker-compose up -d --build
docker-compose ps
curl -fsS http://localhost:8085/actuator/health
curl -I http://localhost:3000
```

Open port 3000 for the UI. Compose requires explicit database passwords in the local `.env` file and does not provide weak defaults. Keep `.env` out of Git. To stop the stack, run `docker-compose down`. To also delete the local database volume, run `docker-compose down -v` (this permanently deletes its data).

## B. Build and publish images for Kubernetes

Use your own Docker Hub username and a unique tag. Do not type a token into source files.

```bash
export DOCKERHUB_USERNAME="YOUR_DOCKERHUB_USERNAME"
export IMAGE_TAG="lab-1"
docker login
docker build -t "$DOCKERHUB_USERNAME/three-tier-todo-backend:$IMAGE_TAG" ./backend
docker build -t "$DOCKERHUB_USERNAME/three-tier-todo-frontend:$IMAGE_TAG" ./frontend
docker push "$DOCKERHUB_USERNAME/three-tier-todo-backend:$IMAGE_TAG"
docker push "$DOCKERHUB_USERNAME/three-tier-todo-frontend:$IMAGE_TAG"
```

Use a Docker Hub access token as the password. Keep the tag consistent for both images.

## C. Deploy using Helm

Check the cluster first:

```bash
kubectl get nodes
helm version
helm lint ./helm/taskmanager -f ./helm/taskmanager/values-killercoda.yaml
```

Create a dedicated namespace and a Kubernetes Secret. Replace the sample values locally; never commit them:

```bash
kubectl create namespace taskmanager --dry-run=client -o yaml | kubectl apply -f -
kubectl create secret generic taskmanager-db -n taskmanager \
  --from-literal=mysql-root-password='REPLACE_WITH_A_STRONG_LAB_PASSWORD' \
  --from-literal=db-username='taskapp' \
  --from-literal=db-password='REPLACE_WITH_A_DIFFERENT_STRONG_LAB_PASSWORD'
```

If the Secret already exists, use `kubectl create secret ... --dry-run=client -o yaml | kubectl apply -f -` to update it. Avoid putting real passwords in shell history; Jenkins should use its credential store.

Install the release:

```bash
helm upgrade --install todo ./helm/taskmanager \
  --namespace taskmanager \
  -f ./helm/taskmanager/values-killercoda.yaml \
  --set image.backend.repository="$DOCKERHUB_USERNAME/three-tier-todo-backend" \
  --set image.backend.tag="$IMAGE_TAG" \
  --set image.frontend.repository="$DOCKERHUB_USERNAME/three-tier-todo-frontend" \
  --set image.frontend.tag="$IMAGE_TAG" \
  --wait --timeout 10m
```

Check resources and events:

```bash
kubectl get pods,deployments,services,pvc -n taskmanager -o wide
kubectl get events -n taskmanager --sort-by=.lastTimestamp
kubectl logs deployment/todo-taskmanager-backend -n taskmanager
kubectl logs deployment/todo-taskmanager-mysql -n taskmanager
```

If Pods are ready, forward the frontend Service and use Killercoda's web preview for port 8080:

```bash
kubectl port-forward -n taskmanager svc/todo-taskmanager-frontend 8080:80
```

If the chart's service or deployment names differ after changing the Helm release name, confirm actual names with `kubectl get svc,deploy -n taskmanager`.

## D. Clean up

```bash
helm uninstall todo -n taskmanager
kubectl delete namespace taskmanager
```

The namespace deletion removes its resources. In the Killercoda values, MySQL uses ephemeral storage, so its data is intentionally not durable.

## Important limits

- This guide has not yet been executed end-to-end in a live Killercoda session. Validate each step and retain the actual output.
- The EKS values require AWS Load Balancer Controller, metrics-server, suitable IAM/networking, and persistent storage. EKS can incur charges; do not deploy it until you have a cost/cleanup plan.
- The Helm chart expects the database Secret keys `mysql-root-password`, `db-username`, and `db-password`.
