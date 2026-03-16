locals {
  param_prefix = "/${var.project}/${var.environment}"
  common_tags = {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

# Rate limit: max requests per minute per IP
resource "aws_ssm_parameter" "rate_limit" {
  name        = "${local.param_prefix}/rate-limit-requests-per-minute"
  description = "Max API requests per minute per client IP for ${var.environment}"
  type        = "String"
  value       = tostring(var.rate_limit_requests_per_minute)

  tags = merge(local.common_tags, { Name = "rate-limit-requests-per-minute" })
}

resource "aws_ssm_parameter" "reservation_timeout" {
  name        = "${local.param_prefix}/reservation-timeout-minutes"
  description = "Reservation expiry window in minutes for ${var.environment}"
  type        = "String"
  value       = tostring(var.reservation_timeout_minutes)

  tags = merge(local.common_tags, { Name = "reservation-timeout-minutes" })
}
