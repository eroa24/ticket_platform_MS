#!/bin/bash
# =====================================================
# Script de inicialización de LocalStack
# =====================================================
# Se ejecuta automáticamente cuando LocalStack está listo.
# Crea la cola SQS necesaria para el sistema de tickets.
# =====================================================

echo "========================================"
echo "  Inicializando LocalStack - SQS Queues"
echo "========================================"

# Crear la cola principal de órdenes de compra
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

echo "========================================"
echo "  LocalStack listo!"
echo "========================================"
