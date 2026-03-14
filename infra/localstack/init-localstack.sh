#!/bin/bash
# =====================================================
# Script de inicialización de LocalStack
# =====================================================
# Se ejecuta automáticamente cuando LocalStack está listo.
# Crea los recursos AWS necesarios para el sistema de tickets:
#   - Cola SQS principal + DLQ con redrive policy
#   - Secret con configuración centralizada de la aplicación
# =====================================================

echo "========================================"
echo "  Inicializando LocalStack"
echo "========================================"

# -----------------------------------------------
# SQS: Crear cola principal de órdenes de compra
# -----------------------------------------------
awslocal sqs create-queue \
    --queue-name purchase-orders-queue \
    --attributes '{
        "VisibilityTimeout": "30",
        "MessageRetentionPeriod": "86400",
        "ReceiveMessageWaitTimeSeconds": "10"
    }'

echo "✅ Cola 'purchase-orders-queue' creada exitosamente"

# Crear Dead Letter Queue (DLQ) para mensajes que fallan repetidamente
awslocal sqs create-queue \
    --queue-name purchase-orders-dlq \
    --attributes '{
        "MessageRetentionPeriod": "1209600"
    }'

echo "✅ DLQ 'purchase-orders-dlq' creada exitosamente"

# Configurar redrive policy (enviar a DLQ después de 3 intentos fallidos)
DLQ_ARN=$(awslocal sqs get-queue-attributes \
    --queue-url http://localhost:4566/000000000000/purchase-orders-dlq \
    --attribute-names QueueArn \
    --query 'Attributes.QueueArn' \
    --output text)

awslocal sqs set-queue-attributes \
    --queue-url http://localhost:4566/000000000000/purchase-orders-queue \
    --attributes "{\"RedrivePolicy\":\"{\\\"maxReceiveCount\\\":\\\"3\\\",\\\"deadLetterTargetArn\\\":\\\"${DLQ_ARN}\\\"}\"}"

echo "✅ Redrive policy configurada (max 3 reintentos → DLQ)"

# -----------------------------------------------
# Secrets Manager: Crear secret de configuración
# -----------------------------------------------
# Contiene configuración operacional que varía por entorno.
# La app lee estos valores al arrancar desde SecretsManagerAdapter.
#
# purchaseQueueName         → nombre de la cola SQS (evita hardcodear env vars)
# rateLimitRequestsPerMinute → límite de peticiones por IP por minuto
# -----------------------------------------------
awslocal secretsmanager create-secret \
    --name "/ticket-platform/config" \
    --description "Configuracion operacional del servicio de tickets" \
    --secret-string '{
        "purchaseQueueName": "purchase-orders-queue",
        "rateLimitRequestsPerMinute": 60
    }'

echo "✅ Secret '/ticket-platform/config' creado exitosamente"

echo "========================================"
echo "  LocalStack listo!"
echo "========================================"
