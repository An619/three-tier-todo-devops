# Production Engineering Upgrade Roadmap

This repository is a learning/portfolio environment. A checked-in configuration is not proof of production readiness: validate every stage in a disposable environment and record test evidence.

## Target delivery flow

1. A reviewed change is pushed to GitHub.
2. Jenkins checks out the repository and runs backend tests/package, frontend lint/build, and static/security checks.
3. Container images are built with immutable build tags and scanned before publication.
4. Images are pushed to a registry using Jenkins credentials (never credentials in source files).
5. Helm deploys to a selected Kubernetes environment only after build and scan stages pass.
6. Smoke tests verify the rollout; a failed rollout is rolled back and investigated.
7. Prometheus/Grafana and logs provide service and infrastructure visibility.

## Upgrade workstreams

Status note: **[x] means the configuration/documentation has been added to this branch, not that a live deployment or every test has passed.**

- [x] Reproducible frontend install, Maven verification stage, and automated build checks in Jenkins
- [x] Multi-stage backend/frontend images and non-root runtime users
- [x] Jenkins pipeline with credential binding, immutable tags, Trivy image scanning, and optional Helm deployment
- [x] Helm chart with separate Killercoda and EKS values
- [x] Kubernetes requests/limits, startup/readiness/liveness probes, and optional autoscaling
- [x] Optional Prometheus ServiceMonitor and Prometheus/Grafana practice guide
- [x] Terraform scaffold for AWS networking/EKS, restricted API CIDRs, and cost-aware cleanup guidance
- [x] Troubleshooting, rollback, and recovery runbooks
- [ ] Validate Compose startup, frontend-to-backend API flow, Helm lint/render, and full Killercoda deployment
- [ ] Validate Jenkins credential IDs, GitHub webhook trigger, Trivy gate, and image publication in the user's Jenkins environment
- [ ] Validate Terraform with current AWS/module versions and run a reviewed plan
- [ ] Complete database backup/restore exercise, failure-injection tests, alert verification, and incident postmortems
- [ ] Configure encrypted remote Terraform state and production secrets integration before any team/production use

## Evidence to collect

For each completed workstream, keep sanitized screenshots or build logs, commands used, pass/fail results, a short explanation of trade-offs, and a runbook. Do not commit tokens, kubeconfigs, cloud credentials, database dumps containing personal data, or unredacted security reports.

## Environment order

1. Local Docker Compose
2. Killercoda Kubernetes
3. A disposable AWS EKS environment only when needed

Destroy cloud resources after practice and verify that load balancers, NAT gateways, EKS nodes, EBS volumes, and public IPs are not left running.
