variable "project" {
  description = "Project name"
  type        = string
}

variable "environment" {
  description = "Deployment environment (dev, qc, pdn)"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID where the ALB is deployed"
  type        = string
}

variable "public_subnet_ids" {
  description = "Public subnet IDs for the ALB (minimum 2 for multi-AZ)"
  type        = list(string)
}

variable "security_group_id" {
  description = "Security group ID for the ALB"
  type        = string
}

variable "container_port" {
  description = "Port the ECS container listens on"
  type        = number
  default     = 8080
}

variable "health_check_path" {
  description = "Path for ALB health checks (Spring Boot Actuator)"
  type        = string
  default     = "/actuator/health"
}

variable "acm_certificate_arn" {
  description = "ACM certificate ARN for HTTPS listener. If empty, only HTTP (port 80) is created."
  type        = string
  default     = ""
}

variable "deregistration_delay" {
  description = "Seconds to wait before deregistering a target (allows in-flight requests to complete)"
  type        = number
  default     = 30
}
