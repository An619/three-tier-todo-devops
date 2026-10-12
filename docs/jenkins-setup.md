# Jenkins Setup for This Repository

## Required agent tools

Install and verify these tools on the Jenkins agent (or use a controlled agent image):

- Git
- JDK 17
- Maven 3.9+
- Node.js/npm compatible with the frontend lockfile
- Docker CLI and access to a Docker daemon
- Trivy (if enabled in the pipeline)
- kubectl and Helm (only for Kubernetes deployment stages)

Do not assume installing the Docker CLI alone provides a working daemon. Verify with `docker info`.

## Credentials

Create credentials in Jenkins; do not store values in Git.

- `dockerhub-creds`: Username with a Docker Hub access token as password.
- `kubeconfig-killercoda`: Secret file containing the kubeconfig for the temporary Killercoda cluster, if supported by the lab session.
- `kubeconfig-eks`: Secret file containing the least-privilege kubeconfig for a disposable EKS cluster.
- `mysql-root-password`: Secret text for the disposable lab MySQL root password.
- `mysql-app-password`: Secret text for the application database user password.
- Add SonarQube/SCA credentials only if those optional integrations are enabled.

Do not use a cluster-admin kubeconfig for routine deployments. Prefer a dedicated namespace and least-privilege service account/RBAC. The Helm deployment stage creates/updates the `taskmanager-db` Kubernetes Secret from the two Jenkins secret-text credentials; keep those values out of Git.

## GitHub push trigger

1. Create a Pipeline job that uses this repository and the intended branch.
2. Configure a GitHub webhook to the Jenkins endpoint only if Jenkins is reachable securely from GitHub.
3. Enable the GitHub hook trigger in the job.
4. Push a harmless documentation commit and confirm that Jenkins checks out the expected branch.
5. Keep webhook secrets and Jenkins credentials outside the repository.

For a Killercoda lab, cluster access can expire when the lab session ends. Reconnect and refresh kubeconfig before running a deployment.
