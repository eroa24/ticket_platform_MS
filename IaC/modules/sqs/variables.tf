variable "project" {
  description = "Project name"
  type        = string
}

variable "environment" {
  description = "Deployment environment (dev, qc, pdn)"
  type        = string
}

variable "visibility_timeout_seconds" {
  description = "Message visibility timeout (should exceed max processing time)"
  type        = number
  default     = 30
}

variable "message_retention_seconds" {
  description = "How long SQS retains unprocessed messages (seconds)"
  type        = number
  default     = 86400 # 24 hours
}

variable "max_receive_count" {
  description = "Number of retries before a message is moved to the DLQ"
  type        = number
  default     = 3
}

variable "dlq_retention_seconds" {
  description = "How long messages are retained in the DLQ for inspection"
  type        = number
  default     = 1209600 # 14 days
}
