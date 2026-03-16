locals {
  name_prefix = "${var.environment}-${var.project}"
  common_tags = {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

# ── Events Table ──────────────────────────────────────────────────────────────

resource "aws_dynamodb_table" "events" {
  name             = "${local.name_prefix}-events"
  billing_mode     = var.billing_mode
  hash_key         = "id"
  deletion_protection_enabled = var.deletion_protection

  attribute {
    name = "id"
    type = "S"
  }

  point_in_time_recovery {
    enabled = var.point_in_time_recovery
  }

  server_side_encryption {
    enabled = true
  }

  tags = merge(local.common_tags, { Name = "${local.name_prefix}-events" })
}

# ── Orders Table ──────────────────────────────────────────────────────────────
# GSIs:
#   idempotencyKey-index  — lookup by idempotencyKey (duplicate order prevention)
#   status-createdAt-index — query expired reservations by status + creation time

resource "aws_dynamodb_table" "orders" {
  name             = "${local.name_prefix}-orders"
  billing_mode     = var.billing_mode
  hash_key         = "id"
  deletion_protection_enabled = var.deletion_protection

  attribute {
    name = "id"
    type = "S"
  }

  attribute {
    name = "idempotencyKey"
    type = "S"
  }

  attribute {
    name = "status"
    type = "S"
  }

  attribute {
    name = "createdAt"
    type = "S"
  }

  # Idempotency check: find existing order by client-provided key
  global_secondary_index {
    name            = "idempotencyKey-index"
    hash_key        = "idempotencyKey"
    projection_type = "ALL"
  }

  # Expired reservation scan: query PENDING orders older than timeout
  global_secondary_index {
    name            = "status-createdAt-index"
    hash_key        = "status"
    range_key       = "createdAt"
    projection_type = "ALL"
  }

  point_in_time_recovery {
    enabled = var.point_in_time_recovery
  }

  server_side_encryption {
    enabled = true
  }

  tags = merge(local.common_tags, { Name = "${local.name_prefix}-orders" })
}
