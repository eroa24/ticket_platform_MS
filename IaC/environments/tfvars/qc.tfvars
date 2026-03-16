project     = "ticket-platform"
environment = "qc"
aws_region  = "us-east-1"

availability_zones   = ["us-east-1a", "us-east-1b"]
vpc_cidr             = "10.1.0.0/16"
public_subnet_cidrs  = ["10.1.1.0/24", "10.1.2.0/24"]
private_subnet_cidrs = ["10.1.11.0/24", "10.1.12.0/24"]

# ECS
image_tag     = "latest"
task_cpu      = "256"
task_memory   = "512"
desired_count = 1
min_capacity  = 1
max_capacity  = 2

# DynamoDB (PITR activado para comportamiento similar a pdn)
dynamodb_point_in_time_recovery = true
dynamodb_deletion_protection    = false

# App config (throttle más conservador que dev para validar comportamiento)
rate_limit_requests_per_minute = 200
reservation_timeout_minutes    = 10

# ALB: HTTP only (agregar ARN si se dispone de certificado)
acm_certificate_arn = ""

# CloudWatch: agregar ARN de SNS si se quieren alertas en QC
alarm_actions = []
