# Argo CD GitOps

Argo CD owns Kubernetes reconciliation. Jenkins performs CI: Maven tests for the services, SonarQube quality-gate enforcement, frontend lint/build, immutable container builds, Trivy scans, SBOM generation, and optional registry publishing. Jenkins does not run kubectl apply or helm upgrade for application delivery.

## Microservices application

The microservices chart is helm/microservices and the dedicated Application manifest is argocd/microservices-application.yaml. It deploys frontend, auth-service, and task-service independently.

Before syncing:

1. Confirm helm lint ./helm/microservices -f ./helm/microservices/values-argocd.yaml passes.
2. Publish auth, task, and frontend images and replace all placeholder repositories/tags with immutable tags that exist.
3. Provision separate auth and task databases and set database.auth.url and database.task.url in the values file.
4. Create the external Secret named by chart values (default taskmanager-microservices) with auth-db-password, task-db-password, and jwt-secret. The auth and task services must use the same strong JWT secret. Never commit secret values.
5. Review rendered manifests with helm template and manually sync only after database connectivity and Secret creation are confirmed.
6. The Application currently tracks feature/microservices-migration for review. After the migration PR is approved and merged, change targetRevision to the protected release branch.

Install Argo CD using the official installation instructions for your Kubernetes distribution. After it is installed, apply the Application manifest:

kubectl apply -f argocd/microservices-application.yaml
kubectl get applications -n argocd
kubectl get pods -n taskmanager

The manifest does not install Argo CD itself, provision databases, create the required Secret, or create AWS infrastructure.

## Release promotion

1. Jenkins tests and scans the source and all three images.
2. Publish immutable tags only after all checks pass.
3. Update all three image tags in helm/microservices/values-argocd.yaml in a reviewed Git change.
4. Argo CD reconciles the desired state.
5. Verify application health, rollouts, logs, and metrics. Roll back by reverting the reviewed GitOps image-tag change.

Keep Jenkins credentials, kubeconfigs, database passwords, and JWT secrets in secret stores, never in Git.
