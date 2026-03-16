# ─────────────────────────────────────────────────────────────────────────────
# Root module — shared across all environments.
# Select the environment by passing the corresponding tfvars file:
#   terraform apply -var-file="tfvars/dev.tfvars"
#   terraform apply -var-file="tfvars/qc.tfvars"
#   terraform apply -var-file="tfvars/pdn.tfvars"
# ─────────────────────────────────────────────────────────────────────────────

module "networking" {
  source = "../modules/networking"

  project              = var.project
  environment          = var.environment
  vpc_cidr             = var.vpc_cidr
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  availability_zones   = var.availability_zones
  container_port       = 8080
}

module "ecr" {
  source = "../modules/ecr"

  project              = var.project
  environment          = var.environment
  image_tag_mutability = "MUTABLE"
  scan_on_push         = true
  keep_last_n_images   = 5
}

module "dynamodb" {
  source = "../modules/dynamodb"

  project                = var.project
  environment            = var.environment
  billing_mode           = "PAY_PER_REQUEST"
  point_in_time_recovery = var.dynamodb_point_in_time_recovery
  deletion_protection    = var.dynamodb_deletion_protection
}

module "sqs" {
  source = "../modules/sqs"

  project                    = var.project
  environment                = var.environment
  visibility_timeout_seconds = 30
  message_retention_seconds  = 86400
  max_receive_count          = 3
  dlq_retention_seconds      = 1209600
}

module "ssm" {
  source = "../modules/ssm"

  project                        = var.project
  environment                    = var.environment
  rate_limit_requests_per_minute = var.rate_limit_requests_per_minute
  reservation_timeout_minutes    = var.reservation_timeout_minutes
}

module "iam" {
  source = "../modules/iam"

  project     = var.project
  environment = var.environment

  dynamodb_table_arns = [
    module.dynamodb.events_table_arn,
    module.dynamodb.orders_table_arn,
  ]

  sqs_queue_arns = [
    module.sqs.queue_arn,
    module.sqs.dlq_arn,
  ]

  ssm_parameter_arns = [
    module.ssm.rate_limit_parameter_arn,
    module.ssm.reservation_timeout_parameter_arn,
  ]

  ecr_repository_arn = module.ecr.repository_arn
}

module "alb" {
  source = "../modules/alb"

  project             = var.project
  environment         = var.environment
  vpc_id              = module.networking.vpc_id
  public_subnet_ids   = module.networking.public_subnet_ids
  security_group_id   = module.networking.alb_security_group_id
  container_port      = 8080
  health_check_path   = "/actuator/health"
  acm_certificate_arn = var.acm_certificate_arn
}

module "cloudwatch" {
  source = "../modules/cloudwatch"

  project                 = var.project
  environment             = var.environment
  log_retention_days      = 30
  ecs_cluster_name        = module.ecs.cluster_name
  ecs_service_name        = module.ecs.service_name
  sqs_dlq_name            = module.sqs.dlq_name
  alb_arn_suffix          = module.alb.alb_arn_suffix
  target_group_arn_suffix = module.alb.target_group_arn_suffix
  dynamodb_events_table   = module.dynamodb.events_table_name
  dynamodb_orders_table   = module.dynamodb.orders_table_name
  alarm_actions           = var.alarm_actions
}

module "ecs" {
  source = "../modules/ecs"

  project            = var.project
  environment        = var.environment
  aws_region         = var.aws_region
  vpc_id             = module.networking.vpc_id
  private_subnet_ids = module.networking.private_subnet_ids
  security_group_id  = module.networking.ecs_security_group_id
  target_group_arn   = module.alb.target_group_arn
  execution_role_arn = module.iam.execution_role_arn
  task_role_arn      = module.iam.task_role_arn
  ecr_repository_url = module.ecr.repository_url
  image_tag          = var.image_tag
  container_port     = 8080
  task_cpu           = var.task_cpu
  task_memory        = var.task_memory
  desired_count      = var.desired_count
  min_capacity       = var.min_capacity
  max_capacity       = var.max_capacity
  log_group_name     = module.cloudwatch.ecs_log_group_name

  dynamodb_events_table             = module.dynamodb.events_table_name
  dynamodb_orders_table             = module.dynamodb.orders_table_name
  sqs_queue_name                    = module.sqs.queue_name
  ssm_rate_limit_param_arn          = module.ssm.rate_limit_parameter_arn
  ssm_reservation_timeout_param_arn = module.ssm.reservation_timeout_parameter_arn
}
