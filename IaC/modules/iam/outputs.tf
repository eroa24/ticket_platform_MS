output "task_role_arn" {
  description = "ARN of the ECS task role (used by the application code)"
  value       = aws_iam_role.task.arn
}

output "task_role_name" {
  description = "Name of the ECS task role"
  value       = aws_iam_role.task.name
}

output "execution_role_arn" {
  description = "ARN of the ECS task execution role (used by the ECS agent)"
  value       = aws_iam_role.execution.arn
}

output "execution_role_name" {
  description = "Name of the ECS task execution role"
  value       = aws_iam_role.execution.name
}
