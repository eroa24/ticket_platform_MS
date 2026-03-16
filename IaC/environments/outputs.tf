output "alb_dns_name" {
  description = "DNS name of the ALB — use this to reach the API"
  value       = module.alb.alb_dns_name
}

output "ecr_repository_url" {
  description = "ECR repository URL — push images here before deploying"
  value       = module.ecr.repository_url
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = module.ecs.cluster_name
}

output "ecs_service_name" {
  description = "ECS service name"
  value       = module.ecs.service_name
}

output "dynamodb_events_table" {
  description = "DynamoDB events table name"
  value       = module.dynamodb.events_table_name
}

output "dynamodb_orders_table" {
  description = "DynamoDB orders table name"
  value       = module.dynamodb.orders_table_name
}

output "sqs_queue_url" {
  description = "SQS purchase orders queue URL"
  value       = module.sqs.queue_url
}

output "sqs_dlq_url" {
  description = "SQS Dead-Letter Queue URL"
  value       = module.sqs.dlq_url
}
