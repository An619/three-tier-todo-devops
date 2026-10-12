output "cluster_name" {
  description = "EKS cluster name."
  value       = module.eks.cluster_name
}

output "cluster_endpoint" {
  description = "EKS Kubernetes API endpoint."
  value       = module.eks.cluster_endpoint
}

output "aws_region" {
  description = "AWS region containing the cluster."
  value       = var.aws_region
}

output "update_kubeconfig_command" {
  description = "Command to configure kubectl access for the creating IAM principal."
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.eks.cluster_name}"
}
