# Deployment Rollback and Recovery Runbook

## Kubernetes deployment rollback

```bash
kubectl rollout status deployment/<deployment> -n <namespace>
kubectl rollout history deployment/<deployment> -n <namespace>
kubectl rollout undo deployment/<deployment> -n <namespace>
kubectl rollout status deployment/<deployment> -n <namespace>
kubectl get pods -n <namespace>
```

For Helm-managed releases:

```bash
helm history <release> -n <namespace>
helm rollback <release> <previous-revision> -n <namespace> --wait --timeout 5m
helm status <release> -n <namespace>
```

Only roll back to a known-good revision. Confirm application compatibility with the current database schema first; application rollback does not automatically reverse database migrations.

## Database recovery exercise

1. Use a disposable test environment and non-sensitive sample data.
2. Create a backup using the database-specific supported procedure.
3. Verify the backup file exists and record its checksum.
4. Restore to a separate test database/volume, not over the live database.
5. Run basic read/write checks and compare expected record counts.
6. Record restore duration and any missing steps.

## Change safety checklist

- [ ] Current revision and image tags recorded
- [ ] Events and logs captured before rollback
- [ ] Rollback target verified
- [ ] Health checks pass after rollback
- [ ] Database schema compatibility checked
- [ ] Incident notes and follow-up tasks recorded

This runbook is a practice guide, not a substitute for testing recovery against the exact database engine and storage configuration in use.
