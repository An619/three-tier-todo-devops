# Legacy Kubernetes manifests

The YAML files in this directory are retained as reference material from the original project. They are not the canonical deployment path for the `upgrade/production-ready-devops` branch.

Use `helm/taskmanager` with the environment-specific values files and follow `docs/deploy-killercoda.md`. Do not apply these legacy manifests together with the Helm release in the same namespace. Database credentials must be created from a secure local source or Jenkins credentials; never commit Secret values to Git.
