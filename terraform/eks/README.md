# Terraform: Disposable AWS EKS Lab

This directory is an infrastructure-as-code starting point for the later EKS phase. It provisions a VPC with public/private subnets, one NAT Gateway by default, an EKS control plane, and a small managed node group. It is not a zero-cost setup.

## Before running Terraform

- Confirm the EKS Kubernetes version is supported in your region.
- Install Terraform and AWS CLI; configure AWS credentials locally using an IAM identity with only the permissions needed for this lab.
- Never put AWS access keys in this repository or in `terraform.tfvars`.
- Replace the example `cluster_endpoint_public_access_cidrs` with your own public IP in CIDR form (usually `x.x.x.x/32`). Never leave the API open to `0.0.0.0/0`.
- Set an AWS Budget/cost alert before creating resources.
- Review every resource in the plan. EKS, EC2 nodes, NAT Gateway, EBS, load balancers, public IPv4 addresses, and data transfer may be billable.
- The example uses local Terraform state. Do not use local state for a shared/team environment; configure an encrypted, versioned S3 backend with appropriate locking before collaboration.
- The lab grants the EBS CSI managed policy to the node role so the EBS CSI add-on can provision the Helm chart's database volume. For production, replace this with a dedicated Pod Identity/IRSA role.

## Initialize and plan

From this directory:

```bash
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars and confirm the Kubernetes version supported by EKS.
terraform fmt -recursive
terraform init
terraform validate
terraform plan -out=tfplan
terraform show tfplan
```

Review the plan carefully before applying. Do not apply if you cannot explain the resources and their estimated cost.

## Create the lab

```bash
terraform apply tfplan
terraform output
aws eks update-kubeconfig --region ap-south-1 --name taskmanager-devops-lab
kubectl get nodes
```

Use the actual region and cluster name from your variables/outputs if you changed them. Then follow the repository's Helm deployment guide and the EKS values file. The EKS values also require AWS Load Balancer Controller for ALB Ingress and metrics-server for HPA metrics.

## Destroy after practice

Remove the Helm release and application namespace first, then destroy Terraform-managed infrastructure:

```bash
helm uninstall todo -n taskmanager
kubectl delete namespace taskmanager
terraform destroy
```

Review the destroy plan and confirm it targets only this lab. Afterwards, check the AWS console for leftover load balancers, EBS volumes, NAT gateways, elastic/public IPs, snapshots, and other billable resources.

## Production follow-up work

Before calling this a production platform, add remote state, policy checks, private endpoint/access restrictions, least-privilege IAM, secrets integration, encryption decisions, backups/restore tests, network policies, and a reviewed upgrade/patching strategy. Treat this configuration as a lab scaffold until it has been validated against current AWS and module documentation.
