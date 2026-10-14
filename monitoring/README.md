# Monitoring with Prometheus and Grafana

This is an optional observability layer for Kubernetes. It requires cluster resources and should be installed only in a disposable lab first.

## What the application exposes

The Spring Boot backend includes Micrometer's Prometheus registry and exposes `/actuator/prometheus`. Health details remain hidden by default. Do not expose Actuator endpoints publicly through the Ingress.

## Install kube-prometheus-stack

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm upgrade --install monitoring prometheus-community/kube-prometheus-stack \
  --namespace monitoring --create-namespace
```

Wait for the monitoring Pods to become ready:

```bash
kubectl get pods -n monitoring
```

## Enable scraping for the backend

The Task Manager chart has an optional ServiceMonitor. Enable it and add the label expected by a typical kube-prometheus-stack installation whose Helm release is named `monitoring`:

```bash
helm upgrade --install todo ./helm/taskmanager \
  --namespace taskmanager \
  -f ./helm/taskmanager/values-killercoda.yaml \
  --set monitoring.serviceMonitor.enabled=true \
  --set monitoring.serviceMonitor.additionalLabels.release=monitoring
```

The image repository/tag values and the `taskmanager-db` Secret must already be set up as described in the deployment guide. The monitoring chart must be installed before enabling the ServiceMonitor CRD.

Verify scraping by port-forwarding Prometheus:

```bash
kubectl port-forward -n monitoring svc/monitoring-kube-prometheus-prometheus 9090:9090
```

Open the lab's web preview for port 9090 and check the Prometheus Targets page. The backend target should be up before you build dashboards or alerts.

## Grafana

```bash
kubectl port-forward -n monitoring svc/monitoring-grafana 3000:80
kubectl get secret -n monitoring monitoring-grafana -o jsonpath='{.data.admin-password}' | base64 -d; echo
```

Use username `admin` and the retrieved password. Treat the password as a secret and do not commit it.

## Practice exercises

1. Compare request/latency metrics before and after a deployment.
2. Scale the backend and observe Pod count and CPU utilization.
3. Cause a controlled readiness failure in a disposable environment and observe the effect on endpoints.
4. Create an alert for unavailable backend Pods and document how it was verified.
5. Remove monitoring resources when finished to preserve lab capacity.

The exact Prometheus service names can differ by chart release name and chart version; confirm with `kubectl get svc -n monitoring` if the sample names differ.
