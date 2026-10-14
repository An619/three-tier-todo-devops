# Jenkins Setup for the Microservices Repository

## Pipeline responsibility

Jenkins is the CI system. It runs Maven verification for the legacy reference module plus auth-service and task-service, runs SonarQube analysis and enforces the Quality Gate, lints/builds the frontend, builds immutable images for auth, task, and frontend, scans each image with Trivy, generates CycloneDX SBOMs, archives reports, and optionally publishes all three images to Docker Hub.

Jenkins does not deploy Kubernetes resources. Argo CD owns reconciliation from reviewed Git desired state.

## Required agent tools

- Git
- JDK 17
- Maven 3.9+
- Node.js/npm compatible with the frontend lockfile
- Docker CLI and access to a Docker daemon
- Trivy
- SonarQube Scanner configured in Jenkins as sonar-scanner

Verify Docker access with docker info. Do not expose an unauthenticated remote Docker daemon.

## Jenkins global configuration

1. Under Manage Jenkins → Tools, add a SonarQube Scanner named exactly sonar-scanner.
2. Under Manage Jenkins → System, add a SonarQube server named exactly sonar.
3. Configure authentication using Jenkins credentials; never put tokens in the repository.
4. Configure the SonarQube webhook to https://YOUR_JENKINS_URL/sonarqube-webhook/ including the trailing slash.
5. Confirm the SonarQube and Pipeline plugins are installed and current.

The repository root contains sonar-project.properties. The pipeline intentionally fails if the Quality Gate fails.

## Credentials

Create dockerhub-creds (Docker Hub username and access token) only if you enable PUBLISH_IMAGES=true. Standard Jenkins CI does not need Kubernetes credentials.

## Create the Pipeline job

1. Create a Pipeline job and select Pipeline script from SCM.
2. Use https://github.com/An619/three-tier-todo-devops.git.
3. During migration review, set the branch specifier to */feature/microservices-migration.
4. Set script path to Jenkinsfile.
5. Run with PUBLISH_IMAGES=false first. Confirm Maven tests, SonarQube Quality Gate, frontend lint/build, all three image builds, Trivy scans, and SBOM generation.
6. Only after those checks pass, configure dockerhub-creds and run with PUBLISH_IMAGES=true.

## GitOps release flow

1. Copy the immutable tags printed by Jenkins for auth, task, and frontend.
2. Update all three image tags and repositories in helm/microservices/values-argocd.yaml in a reviewed Git change.
3. Provision separate auth/task databases and create the external Secret named in chart values, containing auth-db-password, task-db-password, and jwt-secret. Never commit secret values.
4. Configure the Argo CD Application and target revision, then manually sync after reviewing the rendered manifests and database endpoints.
5. Verify Argo CD health and Kubernetes rollout status.

Keep build evidence but redact credentials. Killercoda sessions are temporary; do not rely on their kubeconfig or services persisting.
