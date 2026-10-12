variable "aws_region" {
  description = "AWS region for the disposable EKS lab."
  type        = string
  default     = "ap-south-1"
}

variable "cluster_name" {
  description = "Unique name for this EKS lab cluster."
  type        = string
  default     = "taskmanager-devops-lab"
}

variable "kubernetes_version" {
  description = "Kubernetes version currently supported by EKS in the selected region. Verify before applying."
  type        = string
}

variable "enable_nat_gateway" {
  description = "NAT Gateway is useful for private nodes but has ongoing hourly and data-processing charges."
  type        = bool
  default     = true
}

variable "node_instance_type" {
  description = "Managed node group EC2 instance type."
  type        = string
  default     = "t3.medium"
}

variable "node_min_size" {
  type    = number
  default = 1
}

variable "node_desired_size" {
  type    = number
  default = 1
}

variable "node_max_size" {
  type    = number
  default = 2
}

variable "tags" {
  description = "Tags applied to resources."
  type        = map(string)
  default = {
    Project     = "three-tier-todo-devops"
    Environment = "lab"
    ManagedBy   = "Terraform"
  }
}

variable "cluster_endpoint_public_access_cidrs" {
  description = "Public IPv4 CIDR ranges allowed to reach the EKS API. Set this to your current public IP /32, not 0.0.0.0/0."
  type        = list(string)
}
