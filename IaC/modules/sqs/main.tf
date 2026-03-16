locals {
  name_prefix = "${var.environment}-${var.project}"
  queue_name  = "${local.name_prefix}-purchase-orders"
  common_tags = {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

# ── Dead-Letter Queue ─────────────────────────────────────────────────────────

resource "aws_sqs_queue" "dlq" {
  name                      = "${local.queue_name}-dlq"
  message_retention_seconds = var.dlq_retention_seconds

  sqs_managed_sse_enabled = true

  tags = merge(local.common_tags, { Name = "${local.queue_name}-dlq" })
}

# ── Main Purchase Orders Queue ────────────────────────────────────────────────
# Receives purchase requests from the API layer for async processing.

resource "aws_sqs_queue" "main" {
  name                       = local.queue_name
  visibility_timeout_seconds = var.visibility_timeout_seconds
  message_retention_seconds  = var.message_retention_seconds

  sqs_managed_sse_enabled = true

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq.arn
    maxReceiveCount     = var.max_receive_count
  })

  tags = merge(local.common_tags, { Name = local.queue_name })
}

# DLQ redrive allow policy — permits the main queue to use this DLQ
resource "aws_sqs_queue_redrive_allow_policy" "dlq" {
  queue_url = aws_sqs_queue.dlq.id

  redrive_allow_policy = jsonencode({
    redrivePermission = "byQueue"
    sourceQueueArns   = [aws_sqs_queue.main.arn]
  })
}
