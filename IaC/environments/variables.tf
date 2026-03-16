variable "project" {
  description = "Project name prefix for all resources"
  type        = string
}

variable "environment" {
  description = "Deployment environment (dev, qc, pdn)"
  type        = string
}

variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
}

variable "availability_zones" {
  description = "List of 2 availability zones"
  type        = list(string)
}

# ── Networking ────────────────────────────────────────────────────────────────

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
}

variable "public_subnet_cidrs" {
  description = "CIDRs for public subnets (ALB)"
  type        = list(string)
}

variable "private_subnet_cidrs" {
  description = "CIDRs for private subnets (ECS)"
  type        = list(string)
}

# ── ECR / ECS ─────────────────────────────────────────────────────────────────

variable "image_tag" {
  description = "Docker image tag to deploy"
  type        = string
  default     = "latest"
}

variable "task_cpu" {
  description = "Fargate task CPU units (256, 512, 1024, 2048, 4096)"
  type        = string
}

variable "task_memory" {
  description = "Fargate task memory in MB"
  type        = string
}

variable "desired_count" {
  description = "Initial number of ECS tasks"
  type        = number
}

variable "min_capacity" {
  description = "Minimum ECS tasks for auto-scaling"
  type        = number
}

variable "max_capacity" {
  description = "Maximum ECS tasks for auto-scaling"
  type        = number
}

# ── ALB ───────────────────────────────────────────────────────────────────────

variable "acm_certificate_arn" {
  description = "ACM certificate ARN for HTTPS. Leave empty for HTTP-only (dev/qc)."
  type        = string
  default     = ""
}

# ── DynamoDB ──────────────────────────────────────────────────────────────────

variable "dynamodb_point_in_time_recovery" {
  description = "Enable DynamoDB Point-In-Time Recovery"
  type        = bool
}

variable "dynamodb_deletion_protection" {
  description = "Prevent accidental DynamoDB table deletion"
  type        = bool
}

# ── SSM / App config ──────────────────────────────────────────────────────────

variable "rate_limit_requests_per_minute" {
  description = "Max API requests per minute per client IP"
  type        = number
}

variable "reservation_timeout_minutes" {
  description = "Minutes before a PENDING order reservation expires"
  type        = number
}

# ── CloudWatch ────────────────────────────────────────────────────────────────

variable "alarm_actions" {
  description = "SNS topic ARNs for CloudWatch alarm notifications"
  type        = list(string)
  default     = []
}
