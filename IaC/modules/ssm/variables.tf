variable "project" {
  description = "Project name"
  type        = string
}

variable "environment" {
  description = "Deployment environment (dev, qc, pdn)"
  type        = string
}

variable "rate_limit_requests_per_minute" {
  description = "Max API requests per minute per client IP (injected into the app via ECS secrets)"
  type        = number
}

variable "reservation_timeout_minutes" {
  description = "Minutes before a PENDING order reservation expires"
  type        = number
  default     = 10
}
