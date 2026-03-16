output "queue_url" {
  description = "URL of the main purchase orders SQS queue"
  value       = aws_sqs_queue.main.url
}

output "queue_arn" {
  description = "ARN of the main SQS queue"
  value       = aws_sqs_queue.main.arn
}

output "queue_name" {
  description = "Name of the main SQS queue (injected as env var into ECS)"
  value       = aws_sqs_queue.main.name
}

output "dlq_url" {
  description = "URL of the Dead-Letter Queue"
  value       = aws_sqs_queue.dlq.url
}

output "dlq_arn" {
  description = "ARN of the Dead-Letter Queue"
  value       = aws_sqs_queue.dlq.arn
}

output "dlq_name" {
  description = "Name of the DLQ (used in CloudWatch alarms)"
  value       = aws_sqs_queue.dlq.name
}
