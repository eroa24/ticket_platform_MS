variable "project" {
  description = "Project name"
  type        = string
}

variable "environment" {
  description = "Deployment environment (dev, qc, pdn)"
  type        = string
}

variable "aws_region" {
  description = "AWS region (injected as env var into the container)"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID for the ECS service"
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs where ECS tasks run"
  type        = list(string)
}

variable "security_group_id" {
  description = "Security group ID for ECS tasks"
  type        = string
}

variable "target_group_arn" {
  description = "ALB target group ARN to register tasks with"
  type        = string
}

variable "execution_role_arn" {
  description = "ECS task execution role ARN (ECR pull, CloudWatch logs, SSM secrets)"
  type        = string
}

variable "task_role_arn" {
  description = "ECS task role ARN (DynamoDB, SQS, SSM access by the app)"
  type        = string
}

variable "ecr_repository_url" {
  description = "ECR repository URL (image URI without tag)"
  type        = string
}

variable "image_tag" {
  description = "Docker image tag to deploy"
  type        = string
  default     = "latest"
}

variable "container_port" {
  description = "Port the application listens on inside the container"
  type        = number
  default     = 8080
}

variable "task_cpu" {
  description = "CPU units for the Fargate task (256, 512, 1024, 2048, 4096)"
  type        = string
}

variable "task_memory" {
  description = "Memory (MB) for the Fargate task"
  type        = string
}

variable "desired_count" {
  description = "Initial number of running task replicas"
  type        = number
  default     = 1
}

variable "min_capacity" {
  description = "Minimum number of tasks for auto-scaling"
  type        = number
  default     = 1
}

variable "max_capacity" {
  description = "Maximum number of tasks for auto-scaling"
  type        = number
  default     = 3
}

variable "autoscaling_cpu_target" {
  description = "Target CPU utilisation (%) for auto-scaling policy"
  type        = number
  default     = 70
}

variable "autoscaling_memory_target" {
  description = "Target memory utilisation (%) for auto-scaling policy"
  type        = number
  default     = 70
}

variable "log_group_name" {
  description = "CloudWatch log group name for the ECS tasks"
  type        = string
}

# Application-specific environment variables
variable "dynamodb_events_table" {
  description = "DynamoDB events table name"
  type        = string
}

variable "dynamodb_orders_table" {
  description = "DynamoDB orders table name"
  type        = string
}

variable "sqs_queue_name" {
  description = "SQS queue name for purchase orders"
  type        = string
}

variable "ssm_rate_limit_param_arn" {
  description = "ARN of the SSM parameter for RATE_LIMIT_REQUESTS_PER_MINUTE"
  type        = string
}

variable "ssm_reservation_timeout_param_arn" {
  description = "ARN of the SSM parameter for RESERVATION_TIMEOUT_MINUTES"
  type        = string
}
