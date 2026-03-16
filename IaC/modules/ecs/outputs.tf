output "cluster_name" {
  description = "ECS cluster name (used in CloudWatch alarms)"
  value       = aws_ecs_cluster.main.name
}

output "cluster_arn" {
  description = "ECS cluster ARN"
  value       = aws_ecs_cluster.main.arn
}

output "service_name" {
  description = "ECS service name (used in CloudWatch alarms)"
  value       = aws_ecs_service.app.name
}

output "task_definition_arn" {
  description = "ARN of the latest task definition"
  value       = aws_ecs_task_definition.app.arn
}
