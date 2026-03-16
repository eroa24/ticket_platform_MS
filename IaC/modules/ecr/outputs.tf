output "repository_url" {
  description = "Full ECR repository URL (used in ECS task definition)"
  value       = aws_ecr_repository.app.repository_url
}

output "repository_arn" {
  description = "ARN of the ECR repository (used in IAM policies)"
  value       = aws_ecr_repository.app.arn
}

output "repository_name" {
  description = "Name of the ECR repository"
  value       = aws_ecr_repository.app.name
}
