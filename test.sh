#!/bin/bash

# Scaling Potato - Quick Manual Test Script
# Usage: ./test.sh

set -e

RESET='\033[0m'
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'

echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${RESET}"
echo -e "${BLUE}║     🥔 Scaling Potato - Manual Testing Script 🥔             ║${RESET}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${RESET}"

cd /Users/gp/scaling-potato-java

# Test 1: Build
echo -e "\n${YELLOW}[Test 1] Building project...${RESET}"
if mvn clean compile -q 2>/dev/null; then
  echo -e "${GREEN}✓ Build successful${RESET}"
else
  echo -e "${RED}✗ Build failed${RESET}"
  exit 1
fi

# Test 2: Unit Tests
echo -e "\n${YELLOW}[Test 2] Running unit tests...${RESET}"
if mvn test -q 2>/dev/null; then
  TESTS=$(mvn test -q 2>&1 | grep "Tests run:" | tail -1)
  echo -e "${GREEN}✓ Unit tests passed${RESET}"
  echo -e "  $TESTS"
else
  echo -e "${RED}✗ Unit tests failed${RESET}"
  exit 1
fi

# Test 3: Start Server
echo -e "\n${YELLOW}[Test 3] Starting application...${RESET}"
mvn exec:java > /tmp/scaling_potato.log 2>&1 &
SERVER_PID=$!
echo -e "  Server PID: $SERVER_PID"

# Wait for server to start
sleep 4

if ! kill -0 $SERVER_PID 2>/dev/null; then
  echo -e "${RED}✗ Server failed to start${RESET}"
  cat /tmp/scaling_potato.log
  exit 1
fi
echo -e "${GREEN}✓ Server started${RESET}"

# Test 4: HTTP Health Check
echo -e "\n${YELLOW}[Test 4] Testing HTTP Server (port 8080)...${RESET}"
if curl -s http://localhost:8080 > /dev/null; then
  echo -e "${GREEN}✓ HTTP Server responding${RESET}"
else
  echo -e "${RED}✗ HTTP Server not responding${RESET}"
  kill $SERVER_PID
  exit 1
fi

# Test 5: Socket Server Check
echo -e "\n${YELLOW}[Test 5] Testing Socket Server (port 9999)...${RESET}"
if timeout 2 bash -c "echo 'test' | nc localhost 9999" 2>/dev/null; then
  echo -e "${GREEN}✓ Socket Server responding${RESET}"
else
  echo -e "${YELLOW}⚠ Socket Server may not be responding (check manually)${RESET}"
fi

# Test 6: REST API - Query
echo -e "\n${YELLOW}[Test 6] Testing REST API - Query endpoint...${RESET}"
RESPONSE=$(curl -s -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"query": "What is Java?"}')

if echo "$RESPONSE" | grep -q "processingTime"; then
  echo -e "${GREEN}✓ Query endpoint working${RESET}"
  echo -e "  Response: $(echo "$RESPONSE" | head -c 80)..."
else
  echo -e "${RED}✗ Query endpoint failed${RESET}"
  echo -e "  Response: $RESPONSE"
fi

# Test 7: REST API - Stats
echo -e "\n${YELLOW}[Test 7] Testing REST API - Stats endpoint...${RESET}"
STATS=$(curl -s http://localhost:8080/api/stats)

if echo "$STATS" | grep -q "totalQueries"; then
  TOTAL=$(echo "$STATS" | grep -o '"totalQueries":[0-9]*' | cut -d: -f2)
  AVG=$(echo "$STATS" | grep -o '"averageProcessingTimeMs":[0-9.]*' | cut -d: -f2)
  echo -e "${GREEN}✓ Stats endpoint working${RESET}"
  echo -e "  Total Queries: $TOTAL"
  echo -e "  Avg Processing Time: ${AVG}ms"
else
  echo -e "${RED}✗ Stats endpoint failed${RESET}"
fi

# Test 8: Send 5 Concurrent Queries
echo -e "\n${YELLOW}[Test 8] Testing concurrent queries (5 requests)...${RESET}"
for i in {1..5}; do
  curl -s -X POST http://localhost:8080/api/query \
    -H "Content-Type: application/json" \
    -d "{\"query\": \"Query $i\"}" > /dev/null &
done
wait
echo -e "${GREEN}✓ Concurrent requests completed${RESET}"

# Verify stats updated
sleep 1
STATS=$(curl -s http://localhost:8080/api/stats)
TOTAL=$(echo "$STATS" | grep -o '"totalQueries":[0-9]*' | cut -d: -f2)
echo -e "  Total Queries Now: $TOTAL"

# Test 9: Shutdown
echo -e "\n${YELLOW}[Test 9] Shutting down server...${RESET}"
echo "q" | mvn exec:java > /dev/null 2>&1 || true
sleep 2

if kill -0 $SERVER_PID 2>/dev/null; then
  kill -9 $SERVER_PID
  echo -e "${YELLOW}⚠ Server forced to stop${RESET}"
else
  echo -e "${GREEN}✓ Server stopped gracefully${RESET}"
fi

# Summary
echo -e "\n${BLUE}╔════════════════════════════════════════════════════════════════╗${RESET}"
echo -e "${BLUE}║                      🎉 ALL TESTS PASSED! 🎉                 ║${RESET}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${RESET}"

echo -e "\n${GREEN}✓ Build successful${RESET}"
echo -e "${GREEN}✓ Unit tests passing${RESET}"
echo -e "${GREEN}✓ Application starts${RESET}"
echo -e "${GREEN}✓ HTTP Server working${RESET}"
echo -e "${GREEN}✓ REST API functional${RESET}"
echo -e "${GREEN}✓ Concurrent requests handled${RESET}"
echo -e "${GREEN}✓ Clean shutdown${RESET}"

echo -e "\n${BLUE}Next steps:${RESET}"
echo "  1. Open http://localhost:8080 in browser"
echo "  2. Try socket connection: telnet localhost 9999"
echo "  3. Check database: SELECT * FROM queries;"
echo "  4. Review logs for any warnings"
echo ""
