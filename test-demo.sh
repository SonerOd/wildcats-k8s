#!/bin/bash

echo "🎬 AppDynamics Demo Test Script"
echo "================================"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Set up port forwards
echo -e "${YELLOW}Setting up port forwards...${NC}"
kubectl port-forward -n appdynamics-demo svc/auth-service 3000:3000 > /dev/null 2>&1 &
kubectl port-forward -n appdynamics-demo svc/dummy-account-service 8081:8081 > /dev/null 2>&1 &
kubectl port-forward -n appdynamics-demo svc/credit-check-service 8082:8082 > /dev/null 2>&1 &
kubectl port-forward -n appdynamics-demo svc/loan-service 8083:8083 > /dev/null 2>&1 &
kubectl port-forward -n appdynamics-demo svc/transaction-service 8084:8084 > /dev/null 2>&1 &
kubectl port-forward -n appdynamics-demo svc/report-service 8085:8085 > /dev/null 2>&1 &

sleep 5

echo ""
echo "📝 Test 1: Auth Service"
echo "------------------------"
curl -s -X POST http://localhost:3000/api/auth/login | jq '.' || echo "Auth service not ready"

echo ""
echo "📝 Test 2: Create Account"
echo "------------------------"
curl -s -X POST http://localhost:8081/api/accounts/create | jq '.' || echo "Account service not ready"

echo ""
echo "📝 Test 3: Credit Check"
echo "------------------------"
curl -s -X POST http://localhost:8082/api/credit/check | jq '.' || echo "Credit service not ready"

echo ""
echo "📝 Test 4: Loan Application"
echo "------------------------"
curl -s -X POST http://localhost:8083/api/loans/apply | jq '.' || echo "Loan service not ready"

echo ""
echo "📝 Test 5: Transaction List"
echo "------------------------"
curl -s http://localhost:8084/api/transactions/list | jq '.' || echo "Transaction service not ready"

echo ""
echo "📝 Test 6: Report Stats"
echo "------------------------"
curl -s http://localhost:8085/api/reports/stats | jq '.' || echo "Report service not ready"

echo ""
echo -e "${GREEN}✅ Basic tests completed!${NC}"

echo ""
echo "🔄 Running load test (50 requests)..."
for i in {1..50}; do
    curl -s -X POST http://localhost:8083/api/loans/apply > /dev/null &
    curl -s -X POST http://localhost:8082/api/credit/check > /dev/null &
done

wait

echo -e "${GREEN}✅ Load test completed!${NC}"

echo ""
echo "📊 Check AppDynamics for:"
echo "  • Application Flow Map"
echo "  • Business Transactions"
echo "  • Database calls"
echo "  • Error rates"
echo "  • Response times"

# Clean up port forwards
echo ""
echo "Cleaning up port forwards..."
pkill -f "kubectl port-forward" 2>/dev/null || true

echo -e "${GREEN}✅ Test script completed!${NC}"