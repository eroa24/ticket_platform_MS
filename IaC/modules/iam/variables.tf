variable "project" {
  description = "Project name"
  type        = string
}

variable "environment" {
  description = "Deployment environment (dev, qc, pdn)"
  type        = string
}

variable "dynamodb_table_arns" {
  description = "ARNs of DynamoDB tables the task role must access (includes GSI ARNs via /index/*)"
  type        = list(string)
}

variable "sqs_queue_arns" {
  description = "ARNs of SQS queues (main + DLQ) the task role must access"
  type        = list(string)
}

variable "ssm_parameter_arns" {
  description = "ARNs of SSM Parameter Store parameters the task role may read at startup"
  type        = list(string)
}

variable "ecr_repository_arn" {
  description = "ARN of the ECR repository (grants execution role pull access)"
  type        = string
}
