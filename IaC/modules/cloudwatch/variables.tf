variable "project" {
  description = "Project name"
  type        = string
}

variable "environment" {
  description = "Deployment environment (dev, qc, pdn)"
  type        = string
}

variable "log_retention_days" {
  description = "CloudWatch log retention period in days"
  type        = number
  default     = 30
}

variable "ecs_cluster_name" {
  description = "ECS cluster name (for metric dimensions)"
  type        = string
}

variable "ecs_service_name" {
  description = "ECS service name (for metric dimensions)"
  type        = string
}

variable "sqs_dlq_name" {
  description = "SQS DLQ name (for DLQ depth alarm)"
  type        = string
}

variable "alb_arn_suffix" {
  description = "ALB ARN suffix (for ALB metric dimensions)"
  type        = string
}

variable "target_group_arn_suffix" {
  description = "Target group ARN suffix (for ALB metric dimensions)"
  type        = string
}

variable "dynamodb_events_table" {
  description = "DynamoDB events table name (for throttle alarm)"
  type        = string
}

variable "dynamodb_orders_table" {
  description = "DynamoDB orders table name (for throttle alarm)"
  type        = string
}

variable "alarm_actions" {
  description = "List of SNS topic ARNs to notify on alarm state changes. Leave empty to skip notifications."
  type        = list(string)
  default     = []
}
