output "rate_limit_parameter_arn" {
  description = "ARN of the rate-limit SSM parameter (used in ECS secrets and IAM policy)"
  value       = aws_ssm_parameter.rate_limit.arn
}

output "rate_limit_parameter_name" {
  description = "Name of the rate-limit SSM parameter"
  value       = aws_ssm_parameter.rate_limit.name
}

output "reservation_timeout_parameter_arn" {
  description = "ARN of the reservation-timeout SSM parameter"
  value       = aws_ssm_parameter.reservation_timeout.arn
}

output "reservation_timeout_parameter_name" {
  description = "Name of the reservation-timeout SSM parameter"
  value       = aws_ssm_parameter.reservation_timeout.name
}
