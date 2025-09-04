#!/bin/bash

echo "🚀 AppDynamics Demo Deployment Starting..."

# Create namespace
echo "Creating namespace..."
kubectl create namespace appdynamics-demo 2>/dev/null || true

# Deploy infrastructure
echo "📦 Deploying infrastructure (MongoDB, Redis, Kafka, RabbitMQ)..."
kubectl apply -f 1-infrastructure.yaml

# Wait for infrastructure
echo "⏳ Waiting for infrastructure to be ready (30s)..."
sleep 30

# Deploy Java services
echo "☕ Deploying Java services..."
kubectl apply -f 2-java-services.yaml

# Deploy Node.js and Python services
echo "🐍 Deploying Node.js and Python services..."
kubectl apply -f 3-nodejs-python.yaml

# Check status
echo ""
echo "✅ Deployment complete! Checking pod status..."
kubectl get pods -n appdynamics-demo

echo ""
echo "📊 Services:"
kubectl get svc -n appdynamics-demo

echo ""
echo "🔗 Port forwarding commands:"
echo "kubectl port-forward -n appdynamics-demo svc/auth-service 3000:3000 &"
echo "kubectl port-forward -n appdynamics-demo svc/dummy-account-service 8081:8081 &"
echo "kubectl port-forward -n appdynamics-demo svc/credit-check-service 8082:8082 &"
echo "kubectl port-forward -n appdynamics-demo svc/loan-service 8083:8083 &"
echo "kubectl port-forward -n appdynamics-demo svc/transaction-service 8084:8084 &"
echo "kubectl port-forward -n appdynamics-demo svc/report-service 8085:8085 &"
echo "kubectl port-forward -n appdynamics-demo svc/rabbitmq 15672:15672 &"

echo ""
echo "🧪 Test commands:"
echo "# Login test:"
echo "curl -X POST http://localhost:3000/api/auth/login"
echo ""
echo "# Loan application test:"
echo "curl -X POST http://localhost:8083/api/loans/apply"
echo ""
echo "# Transaction list:"
echo "curl http://localhost:8084/api/transactions/list"
echo ""
echo "# Report stats:"
echo "curl http://localhost:8085/api/reports/stats"