locals {
  name_prefix = "${var.project}-${var.environment}"
  common_tags = {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

# ── Log Groups ────────────────────────────────────────────────────────────────

resource "aws_cloudwatch_log_group" "ecs" {
  name              = "/ecs/${local.name_prefix}/ticket-platform"
  retention_in_days = var.log_retention_days

  tags = merge(local.common_tags, { Name = "${local.name_prefix}-ecs-logs" })
}

# ── ECS Alarms ────────────────────────────────────────────────────────────────

resource "aws_cloudwatch_metric_alarm" "ecs_cpu_high" {
  alarm_name          = "${local.name_prefix}-ecs-cpu-high"
  alarm_description   = "ECS task CPU utilisation above 80% — consider scaling up"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/ECS"
  period              = 60
  statistic           = "Average"
  threshold           = 80
  treat_missing_data  = "notBreaching"

  dimensions = {
    ClusterName = var.ecs_cluster_name
    ServiceName = var.ecs_service_name
  }

  alarm_actions = var.alarm_actions
  ok_actions    = var.alarm_actions

  tags = local.common_tags
}

resource "aws_cloudwatch_metric_alarm" "ecs_memory_high" {
  alarm_name          = "${local.name_prefix}-ecs-memory-high"
  alarm_description   = "ECS task memory utilisation above 80% — potential OOM risk"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "MemoryUtilization"
  namespace           = "AWS/ECS"
  period              = 60
  statistic           = "Average"
  threshold           = 80
  treat_missing_data  = "notBreaching"

  dimensions = {
    ClusterName = var.ecs_cluster_name
    ServiceName = var.ecs_service_name
  }

  alarm_actions = var.alarm_actions
  ok_actions    = var.alarm_actions

  tags = local.common_tags
}

resource "aws_cloudwatch_metric_alarm" "ecs_running_tasks_low" {
  alarm_name          = "${local.name_prefix}-ecs-running-tasks-low"
  alarm_description   = "No ECS tasks running — service may be down"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = 1
  metric_name         = "RunningTaskCount"
  namespace           = "ECS/ContainerInsights"
  period              = 60
  statistic           = "Average"
  threshold           = 1
  treat_missing_data  = "breaching"

  dimensions = {
    ClusterName = var.ecs_cluster_name
    ServiceName = var.ecs_service_name
  }

  alarm_actions = var.alarm_actions

  tags = local.common_tags
}

# ── ALB Alarms ────────────────────────────────────────────────────────────────

resource "aws_cloudwatch_metric_alarm" "alb_5xx_high" {
  alarm_name          = "${local.name_prefix}-alb-5xx-high"
  alarm_description   = "ALB 5xx error rate above 5% — possible application or infrastructure fault"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  threshold           = 5
  treat_missing_data  = "notBreaching"

  # HTTPCode_Target_5XX_Count / RequestCount * 100
  metric_query {
    id          = "error_rate"
    expression  = "errors / total * 100"
    label       = "5xx Error Rate (%)"
    return_data = true
  }

  metric_query {
    id = "errors"
    metric {
      metric_name = "HTTPCode_Target_5XX_Count"
      namespace   = "AWS/ApplicationELB"
      period      = 60
      stat        = "Sum"
      dimensions = {
        LoadBalancer = var.alb_arn_suffix
        TargetGroup  = var.target_group_arn_suffix
      }
    }
  }

  metric_query {
    id = "total"
    metric {
      metric_name = "RequestCount"
      namespace   = "AWS/ApplicationELB"
      period      = 60
      stat        = "Sum"
      dimensions = {
        LoadBalancer = var.alb_arn_suffix
        TargetGroup  = var.target_group_arn_suffix
      }
    }
  }

  alarm_actions = var.alarm_actions
  ok_actions    = var.alarm_actions

  tags = local.common_tags
}

resource "aws_cloudwatch_metric_alarm" "alb_target_unhealthy" {
  alarm_name          = "${local.name_prefix}-alb-unhealthy-hosts"
  alarm_description   = "ALB has unhealthy targets — requests may be failing"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "UnHealthyHostCount"
  namespace           = "AWS/ApplicationELB"
  period              = 60
  statistic           = "Maximum"
  threshold           = 0
  treat_missing_data  = "notBreaching"

  dimensions = {
    LoadBalancer = var.alb_arn_suffix
    TargetGroup  = var.target_group_arn_suffix
  }

  alarm_actions = var.alarm_actions
  ok_actions    = var.alarm_actions

  tags = local.common_tags
}

# ── SQS DLQ Alarm ─────────────────────────────────────────────────────────────
# Any message landing in the DLQ means a purchase failed after all retries.

resource "aws_cloudwatch_metric_alarm" "sqs_dlq_messages" {
  alarm_name          = "${local.name_prefix}-sqs-dlq-depth"
  alarm_description   = "Messages in DLQ — purchase orders failed after all retries"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = 60
  statistic           = "Sum"
  threshold           = 0
  treat_missing_data  = "notBreaching"

  dimensions = {
    QueueName = var.sqs_dlq_name
  }

  alarm_actions = var.alarm_actions

  tags = local.common_tags
}

# ── DynamoDB Alarms ───────────────────────────────────────────────────────────

resource "aws_cloudwatch_metric_alarm" "dynamodb_throttles" {
  for_each = {
    events = var.dynamodb_events_table
    orders = var.dynamodb_orders_table
  }

  alarm_name          = "${local.name_prefix}-dynamodb-${each.key}-throttles"
  alarm_description   = "DynamoDB ${each.key} table is being throttled — check capacity or request patterns"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  threshold           = 0
  treat_missing_data  = "notBreaching"

  metric_query {
    id          = "total_throttles"
    expression  = "read_throttles + write_throttles"
    label       = "Total Throttled Requests"
    return_data = true
  }

  metric_query {
    id = "read_throttles"
    metric {
      metric_name = "ReadThrottledRequests"
      namespace   = "AWS/DynamoDB"
      period      = 60
      stat        = "Sum"
      dimensions  = { TableName = each.value }
    }
  }

  metric_query {
    id = "write_throttles"
    metric {
      metric_name = "WriteThrottledRequests"
      namespace   = "AWS/DynamoDB"
      period      = 60
      stat        = "Sum"
      dimensions  = { TableName = each.value }
    }
  }

  alarm_actions = var.alarm_actions
  ok_actions    = var.alarm_actions

  tags = local.common_tags
}
