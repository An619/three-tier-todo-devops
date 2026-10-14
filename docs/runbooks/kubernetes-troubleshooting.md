# Kubernetes Troubleshooting Runbook

Use this sequence before changing resources. Replace `<namespace>` and `<pod>` with actual names.

## 1. Establish current state

```bash
kubectl get nodes -o wide
kubectl get pods,deployments,services,ingress -n <namespace> -o wide
kubectl get events -n <namespace> --sort-by=.lastTimestamp
```

## 2. Pod is Pending

```bash
kubectl describe pod <pod> -n <namespace>
kubectl get pvc -n <namespace>
kubectl describe nodes
```

Check insufficient CPU/memory, node selectors/taints, image pull secrets, and PVC binding.

## 3. ImagePullBackOff / ErrImagePull

```bash
kubectl describe pod <pod> -n <namespace>
```

Verify the image repository and immutable tag, registry visibility, and pull-secret configuration. Never paste registry tokens into chat or logs.

## 4. CrashLoopBackOff

```bash
kubectl logs <pod> -n <namespace> --all-containers
kubectl logs <pod> -n <namespace> --all-containers --previous
kubectl describe pod <pod> -n <namespace>
```

Check missing environment variables, database connectivity, probe paths/ports, Java memory limits, and application startup exceptions.

## 5. Service has no endpoints

```bash
kubectl get svc,endpoints -n <namespace>
kubectl get pods -n <namespace> --show-labels
kubectl describe svc <service> -n <namespace>
```

Confirm Service selectors match Pod labels and that Pods are Ready.

## 6. Rollout is stuck

```bash
kubectl rollout status deployment/<deployment> -n <namespace>
kubectl rollout history deployment/<deployment> -n <namespace>
kubectl describe deployment <deployment> -n <namespace>
```

Do not delete resources blindly. Capture events/logs first; use the rollback runbook if the new version is unhealthy.

## 7. Record the incident

Record impact, start/end time, symptoms, evidence, root cause, mitigation, corrective action, and a test that would catch recurrence. Redact credentials and customer data.
