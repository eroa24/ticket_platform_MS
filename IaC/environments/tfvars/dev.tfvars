project     = "ticket-platform"
environment = "dev"
aws_region  = "us-east-1"

availability_zones   = ["us-east-1a", "us-east-1b"]
vpc_cidr             = "10.0.0.0/16"
public_subnet_cidrs  = ["10.0.1.0/24", "10.0.2.0/24"]
private_subnet_cidrs = ["10.0.11.0/24", "10.0.12.0/24"]

# ECS
image_tag     = "latest"
task_cpu      = "256"
task_memory   = "512"
desired_count = 1
min_capacity  = 1
max_capacity  = 1

# DynamoDB
dynamodb_point_in_time_recovery = false
dynamodb_deletion_protection    = false

# App config
rate_limit_requests_per_minute = 1000
reservation_timeout_minutes    = 10

# ALB: HTTP only (no certificate)
acm_certificate_arn = ""

# CloudWatch: no SNS alerts in dev
alarm_actions = []
