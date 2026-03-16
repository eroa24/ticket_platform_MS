variable "project" {
  description = "Project name"
  type        = string
}

variable "environment" {
  description = "Deployment environment (dev, qc, pdn)"
  type        = string
}

variable "billing_mode" {
  description = "DynamoDB billing mode: PAY_PER_REQUEST or PROVISIONED"
  type        = string
  default     = "PAY_PER_REQUEST"
}

variable "point_in_time_recovery" {
  description = "Enable Point-In-Time Recovery (recommended for pdn)"
  type        = bool
  default     = false
}

variable "deletion_protection" {
  description = "Prevent accidental table deletion (enable in pdn)"
  type        = bool
  default     = false
}
