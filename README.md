# Task Manager Microservices — DevOps Engineering Lab

The migration branch separates the React frontend, Spring Boot authentication service, and Spring Boot task service. Authentication and task data use separate MySQL databases. The older backend module remains temporarily for reference and backward-compatibility during migration; the canonical local stack is the root Compose file, which runs the microservices stack.

> **Status:** microservices migration is in progress. CI tests and local Compose integration checks are required before calling it complete or production-ready. The Kubernetes chart expects separately provisioned databases and an external Secret; no AWS resources have been created.

## Architecture

```mermaid
flowchart LR
    Dev[Developer] --> GH[GitHub]
    GH --> J[Jenkins / GitHub Actions]
    J --> T[Tests + SonarQube + Trivy + SBOM]
    T --> R[Immutable service images]
    R --> A[Argo CD + Helm]
    A --> K[Kubernetes]
    K --> UI[React / Nginx]
    UI --> AUTH[Auth service]
    UI --> TASK[Task service]
    AUTH --> UDB[(Auth database)]
    TASK --> TDB[(Task database)]
    AUTH --> M[Metrics]
    TASK --> M
```

## What the upgrade adds

- Jenkins CI pipeline that runs Maven verification/unit tests, SonarQube analysis and Quality Gate enforcement, frontend lint/build, immutable image builds, Trivy scanning, CycloneDX SBOM generation, and optional credential-backed Docker Hub publishing. Jenkins does not deploy Kubernetes resources.
- GitHub Actions checks for backend tests, frontend lint/build, Helm lint, Terraform format/validation, plus CodeQL scans for Java and TypeScript and Dependabot update PRs.
- Multi-stage container builds, a non-root backend runtime, unprivileged Nginx frontend, and same-origin `/api` routing.
- Docker Compose health checks, service networking, and startup ordering.
- Helm chart for frontend, backend, and MySQL with probes, resource requests/limits, externalized database credentials, optional HPA, ALB Ingress, and Prometheus ServiceMonitor.
- Argo CD Application manifest and dedicated GitOps values file. Kubernetes delivery is controlled by reviewed image-tag changes in Git; Argo CD owns reconciliation.
- Terraform scaffold for AWS VPC/EKS, with endpoint CIDR restriction and cost-aware cleanup instructions.
- Runbooks for Kubernetes troubleshooting, rollback/recovery, and monitoring.

## Repository layout

```text
backend/                       Legacy Spring Boot API retained during migration
frontend/                      React/Vite UI and microservices-aware Nginx image
services/auth-service/         Registration/login, BCrypt, signed JWTs
services/task-service/         Owner-scoped task API and JWT validation
services/docker-compose.yml    Standalone microservices Compose stack
docker-compose.yml             Canonical local microservices stack
Jenkinsfile                    Jenkins CI pipeline
argocd/                        Argo CD Application and GitOps guide
sonar-project.properties       SonarQube source configuration
.github/workflows/             Validation and CodeQL security workflows
.github/dependabot.yml          Automated dependency update PRs
helm/taskmanager/              Legacy three-tier chart during migration
helm/microservices/            Frontend/auth/task service chart
terraform/eks/                 Terraform VPC/EKS lab scaffold
monitoring/                    Prometheus/Grafana practice guide
docs/                          Setup guides, roadmap and runbooks
K8s/                           Legacy Kubernetes examples; not canonical
azure-pipelines.yml             Legacy pipeline example
```

## 1. Local validation with Docker Compose

Requirements: Git, Docker, and Docker Compose v2.

```bash
git clone --branch feature/microservices-migration --single-branch https://github.com/An619/three-tier-todo-devops.git
cd three-tier-todo-devops
cp .env.example .env
# Replace every example password and JWT secret with unique random values.
docker compose config --quiet
docker compose up -d --build
docker compose ps
curl -I http://localhost:3000
```

Open `http://localhost:3000`, register a new account, and create tasks. Nginx routes `/api/auth/**` and `/api/tasks/**` to separate services. The task API requires a bearer token and scopes records to the authenticated email. Compose refuses to start without explicit database passwords and a JWT secret in the ignored local `.env` file. Never commit `.env`.

Useful commands:

```bash
docker compose logs -f
docker compose logs -f auth-service
docker compose logs -f task-service
docker compose down
```

Only use `docker-compose down -v` when you intentionally want to delete the local MySQL volume and all data stored in it.

## 2. Practise on Killercoda first

Follow [docs/deploy-killercoda.md](docs/deploy-killercoda.md) for the step-by-step sequence:

1. Build and verify the application locally.
2. Build and push uniquely tagged images to your Docker Hub account.
3. Create the `taskmanager-db` Secret outside Git.
4. Run `helm lint` and install the chart with `values-killercoda.yaml`.
5. Verify Pods, Services, logs, events, and the UI using port-forward/web preview.
6. Clean up the Helm release and namespace.

The Killercoda values use ephemeral database storage, so data can be lost if the MySQL Pod is recreated.

## 3. Jenkins pipeline

See [docs/jenkins-setup.md](docs/jenkins-setup.md) and [SonarQube setup](docs/sonarqube-setup.md) before running Jenkins. The CI agent needs Git, JDK 17, Maven, Node.js/npm, Docker daemon access, Trivy, and the SonarQube Scanner. Helm/kubectl are needed for separate manual lab exercises, not the Jenkins CI pipeline.

Create the documented Jenkins credentials before enabling image publishing:

- `dockerhub-creds` — Docker Hub username and access token.
- Argo CD is configured separately in the target cluster. Jenkins CI does not need cluster credentials.
- Do not create Kubernetes credentials in Jenkins for the standard CI pipeline; Argo CD owns delivery.

Configure the Jenkins job to use this repository and branch, then set up a secure GitHub webhook if Jenkins is reachable from GitHub. Start with `PUBLISH_IMAGES=false`; only publish after the test, Quality Gate, and scan stages pass. The frontend lint stage may expose existing application issues that must be fixed rather than bypassed. See [argocd/README.md](argocd/README.md) for the GitOps release workflow.

## 4. Helm chart

Microservices chart path: `helm/microservices`. The legacy chart at `helm/taskmanager` remains during migration.

- `values-killercoda.yaml` — low-footprint, disposable lab configuration.
- `values-eks.yaml` — ALB Ingress, autoscaling, and persistent database storage; requires AWS Load Balancer Controller, metrics-server, and EBS CSI support.

The microservices chart expects separately provisioned auth/task databases and a Secret named `taskmanager-microservices` with keys `auth-db-password`, `task-db-password`, and `jwt-secret`. Do not commit secret values. Replace image placeholders and set database JDBC URLs before Argo CD sync.

## 5. Terraform and AWS EKS

Start with [terraform/eks/README.md](terraform/eks/README.md). The Terraform files are a learning scaffold and have not been applied or validated against your AWS account.

**AWS costs are possible** for EKS, EC2 nodes, NAT Gateway, EBS volumes, ALB, public IPv4 addresses, and data transfer. Set a budget alert, inspect `terraform plan`, and destroy the lab after practice. Replace the example EKS API CIDR with your own public IP in `/32` form before applying.

## 6. Monitoring

Follow [monitoring/README.md](monitoring/README.md) to install kube-prometheus-stack, enable the backend ServiceMonitor, and practise Prometheus/Grafana. The application exposes `/actuator/prometheus` for scraping; do not expose Actuator endpoints publicly.

## Troubleshooting and recovery

- [Kubernetes troubleshooting runbook](docs/runbooks/kubernetes-troubleshooting.md)
- [Rollback and recovery runbook](docs/runbooks/rollback-and-recovery.md)
- [Production engineering roadmap](docs/production-engineering-roadmap.md)

For each failure exercise, record the symptoms, evidence, impact, root cause, mitigation, and a test that prevents recurrence. Redact credentials from screenshots and logs.

## Known application limitations

- The current frontend authentication flow is a demo that stores user records and passwords in browser `localStorage`; it is **not secure for real users**. Do not use real passwords or present this authentication as production-ready. Replace it with server-side authentication, password hashing, session/token lifecycle controls, and authorization before any real deployment.
- The app is one Spring Boot backend plus a MySQL database. Docker/Kubernetes do not make it microservices; a genuine microservices conversion would require separate business services and ownership boundaries.
- The Compose fallback credentials are for disposable local labs only. Use managed secrets and unique credentials outside the lab.

## Scope and honesty

This project can help demonstrate advanced DevOps engineering skills when its workflows are run successfully and you can explain the trade-offs. It does not itself establish five years of professional work experience. Keep screenshots and logs of real test runs; do not claim stages have passed until they have.

The original application and its upstream project structure are credited to [17J](https://github.com/17J/Three-Tier-Todo-Application).
