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

- [ ] Reproducible builds and automated tests
- [ ] Secure multi-stage container images and non-root runtime users
- [ ] Jenkins pipeline with credential binding and immutable tags
- [ ] Helm chart with separate Killercoda and EKS values
- [ ] Kubernetes requests/limits, startup/readiness/liveness probes, and autoscaling
- [ ] Image and dependency scanning with documented severity thresholds
- [ ] Terraform for AWS networking/EKS, remote state, and least-privilege IAM
- [ ] Monitoring, dashboards, alert rules, log collection, and SLO examples
- [ ] Database backup/restore exercise and recovery runbook
- [ ] Failure-injection exercises and incident postmortems

## Evidence to collect

For each completed workstream, keep sanitized screenshots or build logs, commands used, pass/fail results, a short explanation of trade-offs, and a runbook. Do not commit tokens, kubeconfigs, cloud credentials, database dumps containing personal data, or unredacted security reports.

## Environment order

1. Local Docker Compose
2. Killercoda Kubernetes
3. A disposable AWS EKS environment only when needed

Destroy cloud resources after practice and verify that load balancers, NAT gateways, EKS nodes, EBS volumes, and public IPs are not left running.
