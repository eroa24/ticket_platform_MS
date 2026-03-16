# IaC — Ticket Platform (Terraform)

Infrastructure as Code for the Ticket Platform microservice on AWS.

## Architecture

```
Internet → ALB (HTTP/HTTPS) → ECS Fargate (private subnets)
↕ DynamoDB (events, orders)
↕ SQS (purchase-orders-queue + DLQ)
↕ SSM Parameter Store (rate-limit, timeout)
→ CloudWatch (logs, alarms)
ECR → (image pull) → ECS Fargate
IAM Task Role → DynamoDB + SQS + SSM
```

## Directory Structure

```
IaC/
├── modules/
│   ├── networking/       # VPC, subnets, IGW, NAT Gateway, security groups
│   ├── ecr/              # ECR repository + lifecycle policy
│   ├── alb/              # Application Load Balancer, target group, listeners
│   ├── dynamodb/         # DynamoDB tables (events, orders) with GSIs
│   ├── sqs/              # SQS queue + Dead-Letter Queue
│   ├── ssm/              # SSM Parameter Store parameters
│   ├── iam/              # ECS execution role + task role (least privilege)
│   ├── ecs/              # ECS cluster, task definition, service, auto-scaling
│   └── cloudwatch/       # Log groups + metric alarms
└── environments/
    ├── terraform.tf      # Provider + version constraints (shared)
    ├── main.tf           # Module orchestration (shared)
    ├── variables.tf      # Variable declarations (shared)
    ├── outputs.tf        # Outputs (shared)
    └── tfvars/
        ├── dev.tfvars    # Development values
        ├── qc.tfvars     # QC values
        └── pdn.tfvars    # Production values
```

## Prerequisites

- Terraform >= 1.7.0
- AWS CLI configured with appropriate credentials

## Usage

Todos los comandos se ejecutan desde la carpeta `environments/`.

```bash
cd environments/

# Inicializar
terraform init

# Previsualizar cambios para un ambiente
terraform plan -var-file="tfvars/dev.tfvars"
terraform plan -var-file="tfvars/qc.tfvars"
terraform plan -var-file="tfvars/pdn.tfvars"

# Aplicar
terraform apply -var-file="tfvars/dev.tfvars"

terraform destroy -var-file="tfvars/dev.tfvars"
```

## Deploying a New Image

After pushing a new Docker image to ECR, update the ECS service:

```bash
# Force a new ECS deployment with the latest image
aws ecs update-service \
  --cluster ticket-platform-pdn-cluster \
  --service ticket-platform-pdn-service \
  --force-new-deployment \
  --region us-east-1
```

Or update `image_tag` in the environment's `terraform.tfvars` and run `terraform apply`.