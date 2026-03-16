project     = "ticket-platform"
environment = "pdn"
aws_region  = "us-east-1"

availability_zones   = ["us-east-1a", "us-east-1b"]
vpc_cidr             = "10.2.0.0/16"
public_subnet_cidrs  = ["10.2.1.0/24", "10.2.2.0/24"]
private_subnet_cidrs = ["10.2.11.0/24", "10.2.12.0/24"]

# ECS (tasks más grandes para carga de producción)
image_tag     = "latest"
task_cpu      = "512"
task_memory   = "1024"
desired_count = 2
min_capacity  = 2
max_capacity  = 3

# DynamoDB (protección total en producción)
dynamodb_point_in_time_recovery = true
dynamodb_deletion_protection    = true

# App config (60 req/min = ~1 req/seg promedio)
rate_limit_requests_per_minute = 60
reservation_timeout_minutes    = 10

# ALB: HTTPS requerido en producción — reemplazar con ARN real del certificado ACM
acm_certificate_arn = "arn:aws:acm:us-east-1:ACCOUNT_ID:certificate/CERTIFICATE_ID"

# CloudWatch: reemplazar con ARN real del SNS topic de alertas
alarm_actions = ["arn:aws:sns:us-east-1:ACCOUNT_ID:ticket-platform-pdn-alerts"]
