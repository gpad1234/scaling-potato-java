# Manual Testing Guide - Scaling Potato NLP Application

## Quick Start

### Prerequisites
- Java 25+ installed
- Maven 3.8+ installed
- Port 8080 and 9999 available
- (Optional) OpenAI API key for real NLP responses

---

## Test 1: Build & Compilation

### Test Objective
Verify the project builds without errors

### Steps
```bash
cd /Users/gp/scaling-potato-java

# Clean build
mvn clean compile

# Expected Output
# [INFO] BUILD SUCCESS
```

### Success Criteria
✅ No compilation errors
✅ All dependencies downloaded
✅ `target/classes` directory created

---

## Test 2: Unit Tests

### Test Objective
Verify all 33 unit tests pass

### Steps
```bash
mvn test
```

### Expected Output
```
[INFO] Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Test Coverage
- VirtualThreadFeatureTests: 12 tests
- VirtualThreadExecutorFeatureTests: 17 tests
- ThreadPoolTests: 2 tests
- VirtualThreadTests: 3 tests

### Success Criteria
✅ All 33 tests pass
✅ 0 failures, 0 errors
✅ Execution time < 10 seconds

---

## Test 3: Application Startup

### Test Objective
Verify the application starts without errors

### Steps

**Terminal 1 - Start the server:**
```bash
cd /Users/gp/scaling-potato-java
mvn exec:java
```

**Expected Output:**
```
[INFO] === Scaling Potato NLP Application ===
[INFO] Java Version: 25...
[INFO] Configuration loaded:
[INFO]   Port: 9999
[INFO]   Thread Pool Size: 50
[INFO]   Database: jdbc:h2:mem:scalingpotato
[INFO]   OpenAI API configured: false
[INFO] Server started successfully!
[INFO] Open http://localhost:8080 in your browser
[INFO] Press 'q' to shutdown the servers
```

### Success Criteria
✅ HTTP Server starts on port 8080
✅ Socket Server starts on port 9999
✅ Database initializes without errors
✅ Application ready for queries

---

## Test 4: Web Interface (HTTP)

### Test Objective
Test the interactive web UI

### Steps

**Step 1: Open the web browser**
```
Open: http://localhost:8080
```

**Expected:** Interactive form with:
- Query input textarea
- "Send Query" button
- "View Stats" button
- 🥔 Scaling Potato branding

**Step 2: Submit a test query**
- Type: `What is machine learning?`
- Click: "Send Query"

**Expected Response:**
```
Response: (mock response or actual OpenAI response)
Processing Time: 150-300ms
```

**Step 3: View statistics**
- Click: "View Stats"

**Expected:**
```
Total Queries: 1
Average Processing Time: 150.00ms
```

### Success Criteria
✅ Page loads without errors
✅ Query submission works
✅ Response displays correctly
✅ Processing time is recorded
✅ Statistics update correctly

---

## Test 5: REST API - Query Endpoint

### Test Objective
Test the REST API endpoint for NLP queries

### Steps

**Terminal 2 - Send API request:**
```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"query": "Tell me about Java"}'
```

**Expected Response:**
```json
{
  "response": "Mock response for query: Tell me about Java",
  "processingTime": 145
}
```

### Test Multiple Queries
```bash
# Query 2
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"query": "What is Docker?"}'

# Query 3
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"query": "Explain virtual threads"}'
```

### Success Criteria
✅ HTTP 200 response
✅ Valid JSON response
✅ Response time < 500ms
✅ Multiple queries don't interfere
✅ Processing time increases with queries (threads handling load)

---

## Test 6: REST API - Statistics Endpoint

### Test Objective
Test the statistics API endpoint

### Steps

**After submitting 3+ queries, run:**
```bash
curl http://localhost:8080/api/stats
```

**Expected Response:**
```json
{
  "totalQueries": 3,
  "averageProcessingTimeMs": 147.33
}
```

### Verify Statistics Update
- Run more queries in web UI
- Check stats again - should increment totalQueries

### Success Criteria
✅ Returns valid JSON
✅ totalQueries matches query count
✅ averageProcessingTimeMs is reasonable
✅ Statistics persist across requests

---

## Test 7: Socket Server (Direct Connection)

### Test Objective
Test the socket server with telnet

### Steps

**Terminal 2 - Connect to socket server:**
```bash
telnet localhost 9999
```

**Step 1: Send a simple query**
```
Type: What is Python?
Press: Enter
```

**Expected Response:**
```
RESPONSE:{"response": "Mock response...", "timestamp": 1702041600000}
TIME:152ms
---
```

**Step 2: Request statistics**
```
Type: STATS
Press: Enter
```

**Expected Response:**
```
STATS:
Total queries: X
Average processing time: XXX.XXms
---
```

**Step 3: Test exit command**
```
Type: EXIT
Press: Enter
```

**Expected Response:**
```
Goodbye!
```

### Success Criteria
✅ Socket connection established
✅ Queries processed correctly
✅ Statistics available via STATS command
✅ Exit command closes connection gracefully

---

## Test 8: Database Query History

### Test Objective
Verify queries are stored in H2 database

### Steps

**Terminal 2 - Connect with H2 console (optional):**
```bash
# If H2 shell available:
java -cp target/classes:~/.m2/repository/com/h2database/h2/2.2.220/h2-2.2.220.jar \
  org.h2.tools.Shell

# Connect to: jdbc:h2:mem:scalingpotato
# Query: SELECT * FROM queries;
```

**Or verify via application logs - queries should show:**
```
[DEBUG] Query saved to database - Time: 145 ms
[DEBUG] Query saved to database - Time: 152 ms
```

### Success Criteria
✅ Queries are persisted in database
✅ Query history shows correct timing
✅ Database doesn't grow unbounded

---

## Test 9: Performance & Concurrency

### Test Objective
Test application handles multiple concurrent requests

### Steps

**Terminal 2 - Send concurrent requests:**
```bash
# Fire 10 concurrent requests
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/query \
    -H "Content-Type: application/json" \
    -d "{\"query\": \"Query number $i\"}" &
done
wait

# Check final stats
curl http://localhost:8080/api/stats
```

**Expected Output:**
```json
{
  "totalQueries": 10,
  "averageProcessingTimeMs": 145.50
}
```

### Monitor Server Performance
- Check Terminal 1 for any errors
- Verify all requests complete successfully
- No timeouts or connection resets

### Success Criteria
✅ All 10 requests processed successfully
✅ No errors in server logs
✅ Response time remains consistent
✅ Thread pool handles load efficiently

---

## Test 10: Environment Configuration

### Test Objective
Verify environment variables are loaded correctly

### Steps

**Step 1: Check current .env:**
```bash
cat /Users/gp/scaling-potato-java/.env
```

**Expected:**
```
OPENAI_API_KEY=your_openai_api_key_here
DATABASE_URL=jdbc:h2:mem:scalingpotato
DATABASE_USER=sa
DATABASE_PASSWORD=
PORT=9999
THREAD_POOL_SIZE=50
```

**Step 2: Modify THREAD_POOL_SIZE:**
```bash
# Edit .env
sed -i '' 's/THREAD_POOL_SIZE=50/THREAD_POOL_SIZE=100/' .env
```

**Step 3: Restart application (stop and restart Terminal 1)**
```
Press: q (to stop)
```

Then restart:
```bash
mvn exec:java
```

**Verify in logs:**
```
[INFO]   Thread Pool Size: 100
```

### Success Criteria
✅ .env file is read correctly
✅ Changes to .env take effect on restart
✅ Default values used if .env missing

---

## Test 11: Error Handling

### Test Objective
Verify graceful error handling

### Steps

**Step 1: Send malformed JSON:**
```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d 'invalid json'
```

**Expected:** HTTP error or graceful handling

**Step 2: Send empty query:**
```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"query": ""}'
```

**Expected:** Should process empty query or error gracefully

**Step 3: Connect to wrong port:**
```bash
telnet localhost 8081  # Wrong port
```

**Expected:** Connection refused (expected behavior)

### Success Criteria
✅ No server crashes
✅ Errors logged appropriately
✅ Server remains responsive

---

## Test 12: Shutdown & Cleanup

### Test Objective
Verify clean shutdown

### Steps

**Terminal 1 - Shutdown:**
```
Type: q
Press: Enter
```

**Expected Output:**
```
[INFO] Shutting down...
[INFO] Query Server stopped
[INFO] HTTP Server stopped
```

**Verify:**
```bash
# Check process is gone
ps aux | grep ScalingPotatoApp | grep -v grep
# Should return nothing
```

### Success Criteria
✅ Clean shutdown without errors
✅ All resources released
✅ No hanging threads
✅ Database connections closed

---

## Test Checklist

Use this checklist to track manual testing:

- [ ] **Build & Compilation** - Project builds successfully
- [ ] **Unit Tests** - All 33 tests pass
- [ ] **Application Startup** - No startup errors
- [ ] **Web Interface** - Page loads and works
- [ ] **Web Query** - Can submit queries via UI
- [ ] **Web Stats** - Statistics display correctly
- [ ] **REST API Query** - POST /api/query works
- [ ] **REST API Stats** - GET /api/stats works
- [ ] **Socket Server** - Can connect via telnet
- [ ] **Socket Query** - Queries work over socket
- [ ] **Socket Stats** - STATS command works
- [ ] **Socket Exit** - EXIT command works
- [ ] **Database** - Queries are persisted
- [ ] **Concurrency** - Handles 10+ concurrent requests
- [ ] **Configuration** - .env is read correctly
- [ ] **Error Handling** - Graceful error responses
- [ ] **Shutdown** - Clean shutdown process

---

## Quick Test Script

Save this as `manual_test.sh`:

```bash
#!/bin/bash

echo "=== Starting Scaling Potato Manual Test ==="
cd /Users/gp/scaling-potato-java

# Test 1: Build
echo "Test 1: Building project..."
mvn clean compile -q && echo "✓ Build successful" || echo "✗ Build failed"

# Test 2: Tests
echo "Test 2: Running unit tests..."
mvn test -q && echo "✓ All tests passed" || echo "✗ Tests failed"

# Test 3: Start server (background)
echo "Test 3: Starting server..."
mvn exec:java > /tmp/scaling_potato.log 2>&1 &
SERVER_PID=$!
sleep 3

# Test 4: Test API
echo "Test 4: Testing REST API..."
RESPONSE=$(curl -s -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"query": "test"}')

if echo "$RESPONSE" | grep -q "processingTime"; then
  echo "✓ API working"
else
  echo "✗ API failed"
fi

# Test 5: Test stats
echo "Test 5: Testing statistics..."
STATS=$(curl -s http://localhost:8080/api/stats)
if echo "$STATS" | grep -q "totalQueries"; then
  echo "✓ Stats working"
else
  echo "✗ Stats failed"
fi

# Cleanup
echo "Test 6: Shutting down..."
echo "q" | mvn exec:java > /dev/null 2>&1
sleep 1
kill $SERVER_PID 2>/dev/null

echo "=== Test Complete ==="
```

Run it:
```bash
chmod +x manual_test.sh
./manual_test.sh
```

---

## Troubleshooting

### Port Already in Use
```bash
lsof -ti:8080,9999 | xargs kill -9
```

### Java Version Issues
```bash
java --version
# Should be 25+
```

### Build Issues
```bash
mvn clean install
```

### Clear Maven Cache
```bash
rm -rf ~/.m2/repository
mvn clean install
```

### Check Server Logs
```bash
tail -f /tmp/scaling_potato.log
```

---

## Performance Benchmarks

Expected results on modern hardware:

| Test | Expected | Unit |
|------|----------|------|
| Build time | < 10 | seconds |
| Test execution | < 10 | seconds |
| Single query response | 100-300 | ms |
| 10 concurrent queries | 150-500 | ms |
| Startup time | < 5 | seconds |
| Memory usage | 200-500 | MB |
| Max concurrent connections | 10000+ | connections |

---

## Next Steps

1. Complete all tests above
2. Document any failures
3. Check logs for warnings
4. Validate performance meets expectations
5. Ready for deployment!
