# Microservices Helm chart

This chart deploys independent frontend, auth-service, and task-service Deployments and Services. Nginx routes API calls to auth-service:8081 and task-service:8080.

Before installing, publish immutable images for all three services; replace image repository/tag placeholders; provision separate auth and task databases; set the JDBC URLs; and create the external Secret named in values with keys auth-db-password, task-db-password, and jwt-secret. The auth and task services must use the same strong JWT secret. Never store secret values in Git.

This chart intentionally does not install MySQL inside Kubernetes. For production, use managed database services and private networking. Provision the external databases and Secret before Argo CD sync. No AWS resources are created by this chart.
