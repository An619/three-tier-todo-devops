# Jenkins Setup for This Repository

## Pipeline responsibility

Jenkins is the **CI** system. It checks out code, runs Maven tests, submits analysis to SonarQube, blocks on the SonarQube Quality Gate, lints/builds the frontend, builds Docker images, scans them with Trivy, and optionally publishes immutable tags to Docker Hub.

**Jenkins must not run `helm upgrade` or `kubectl apply` for this app.** Argo CD is the single controller for Kubernetes delivery. After a successful build, promote the exact image tags by changing `helm/taskmanager/values-argocd.yaml` in a reviewed Git commit or pull request.

## Required agent tools

Install and verify these tools on the Jenkins agent (or use a controlled agent image):

- Git
- JDK 17
- Maven 3.9+
- Node.js/npm compatible with the frontend lockfile
- Docker CLI and access to a Docker daemon
- Trivy
- SonarQube Scanner configured in Jenkins as `sonar-scanner`

Verify Docker access with `docker info`. The Jenkins user needs Docker daemon access; do not solve this by exposing an unauthenticated remote Docker daemon.

## Jenkins global configuration

1. Under **Manage Jenkins → Tools**, add a SonarQube Scanner installation named exactly `sonar-scanner`.
2. Under **Manage Jenkins → System**, add a SonarQube server named exactly `sonar`.
3. Configure the server authentication token using Jenkins credentials; do not put the token in the repository.
4. In SonarQube, configure a webhook pointing to `https://YOUR_JENKINS_URL/sonarqube-webhook/` (include the trailing slash). Jenkins' SonarQube plugin uses this callback for `waitForQualityGate`.
5. Confirm the Jenkins SonarQube Scanner and Pipeline plugins are installed and current.

The repository root contains `sonar-project.properties`. The pipeline intentionally fails if the Quality Gate fails; fix the reported issues instead of bypassing the gate.

## Credentials

Create these only if you enable the relevant pipeline action:

- `dockerhub-creds`: username with a Docker Hub access token as password. Required only when `PUBLISH_IMAGES=true`.

Do not store passwords, tokens, kubeconfigs, or database credentials in Git. Standard Jenkins CI does not need Kubernetes credentials; Argo CD owns cluster delivery.

## Create the Pipeline job

1. Create a Pipeline job and select **Pipeline script from SCM**.
2. Select Git and use `https://github.com/An619/three-tier-todo-devops.git`.
3. Set the branch specifier to `*/upgrade/production-ready-devops` while validating this draft branch.
4. Set the script path to `Jenkinsfile`.
5. Configure a secure GitHub webhook only if Jenkins is reachable securely from GitHub, then enable the GitHub hook trigger.
6. Run first with `PUBLISH_IMAGES=false`. Confirm backend tests, SonarQube Quality Gate, frontend lint/build, image build and Trivy stages pass.
7. Only after those checks pass, configure `dockerhub-creds` and run with `PUBLISH_IMAGES=true`.

## GitOps release flow

1. Copy the immutable image tags printed by Jenkins.
2. Update both tags in `helm/taskmanager/values-argocd.yaml`; replace the placeholder values.
3. Submit a reviewed Git commit/PR. Do not promote `latest` in a release workflow.
4. Argo CD detects the desired-state change and syncs the Helm chart. Verify the Argo CD Application and Kubernetes rollout.

Keep logs and build evidence, but redact credentials. A Killercoda session is temporary; do not depend on its kubeconfig or services persisting after the lab ends.
