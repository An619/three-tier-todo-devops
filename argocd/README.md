# Argo CD GitOps

Argo CD is the deployment controller for this project. Jenkins performs CI (tests, SonarQube, image build, Trivy scan and optional image publishing); it does **not** run `helm upgrade` or `kubectl apply` for the application. Argo CD reads the Helm chart and desired image tags from Git.

## Before installing

1. Ensure the chart has passed `helm lint ./helm/taskmanager -f ./helm/taskmanager/values-killercoda.yaml`.
2. Build and scan the frontend/backend images, then publish them to a registry accessible by the cluster.
3. Edit `helm/taskmanager/values-argocd.yaml` and set both image repositories and immutable tags to images that actually exist. Do not leave `latest` as the release tag for a real deployment.
4. Review database persistence and secrets for your target environment. Do not commit passwords to Git; create the `taskmanager-db` Kubernetes Secret out-of-band.
5. The Application currently tracks `upgrade/production-ready-devops`. After the upgrade PR is reviewed and merged, change `targetRevision` in `application.yaml` to `main` (or your chosen protected release branch).

## Install Argo CD

Install Argo CD using its official installation instructions for your Kubernetes distribution. Then apply this application manifest:

```bash
kubectl create namespace argocd
# Install Argo CD into argocd first, then:
kubectl apply -f argocd/application.yaml
kubectl get applications -n argocd
kubectl get pods -n taskmanager
```

The first command alone does not install Argo CD; follow the official Argo CD installation guide before applying the Application.

## Release promotion

1. Jenkins tests the source, waits for the SonarQube Quality Gate, builds images and scans them.
2. Publish immutable image tags only after checks pass.
3. Update the image tags in `helm/taskmanager/values-argocd.yaml` in a reviewed Git commit/PR.
4. Argo CD detects the desired-state change and reconciles the cluster.
5. Verify the Argo CD application health and Kubernetes rollout. Revert the GitOps commit to roll back to a previously known-good image tag.

For a team workflow, protect the release branch and require reviews for image-tag changes. Keep Jenkins credentials, kubeconfigs and application passwords in the relevant secret stores, never in Git.
