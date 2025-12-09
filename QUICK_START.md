# Scaling Potato - Quick Start Guide

Get the application running in 5 minutes with real agent mode enabled.

## Prerequisites

- ✅ Java 25.0.1 installed
- ✅ Maven 3.8+ installed
- ✅ OpenAI API key (in `.env` file)

## 1. Verify Your API Key

```bash
cat .env | grep OPENAI_API_KEY
```

**Expected output:**
```
OPENAI_API_KEY=sk-proj-xxxxx...
```

If empty, add your key:
```bash
echo "OPENAI_API_KEY=sk-your-key-here" >> .env
```

## 2. Build the Application

```bash
cd /Users/gp/java-code/scaling-potato-java
mvn clean package -DskipTests=true
```

**Expected:** `BUILD SUCCESS`

## 3. Start the Server

```bash
mvn exec:java -Dexec.mainClass="com.example.nlp.ScalingPotatoApp"
```

**Expected startup messages:**
```
✅ OpenAI API configured: true
✅ Initializing MCP (Model Context Protocol) servers...
✅ DatabaseMCPServer initialized with 5 tools
✅ AgentNLPService initialized with 1 MCP servers
✅ Server started successfully!
✅ Open http://localhost:8080 in your browser
```

## 4. Test in Browser

Open: **http://localhost:8080**

You should see:
- A text input for queries
- A button to send queries
- Results displayed below

Example queries:
- "How to grow potatoes in Montana?"
- "What's the best potato variety for cold climates?"
- "How to prevent potato blight?"

## 5. Test via API (Terminal)

In a new terminal:

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"query":"How to grow potatoes?"}'
```

### Real Mode Response (with API key)

```json
{
  "response": "Based on our database showing 42 total queries with average processing time of 187.3ms... [Agent Execution Summary] Tools Used: query_history, get_stats Iterations: 2 Processing Time: 234ms",
  "processingTime": 234,
  "mode": "agent"
}
```

**Indicators of real mode:**
- ✅ Processing time: 200-500ms (includes OpenAI API calls)
- ✅ Response includes specific data ("42 total queries", "187.3ms")
- ✅ Shows actual tool names used
- ✅ `"mode": "agent"`

### Mock Mode Response (without API key)

```json
{
  "response": "Plant potatoes in well-draining soil, maintain 55-75°F temperature, provide 1-2 inches of water weekly. [Agent Execution Summary] Tools Used: None Iterations: 0 Processing Time: 45ms",
  "processingTime": 45,
  "mode": "agent"
}
```

**Indicators of mock mode:**
- Generic potato advice (no specific database data)
- Processing time: < 150ms
- No actual tools executed

## 6. Test Stats Endpoint

```bash
curl -s http://localhost:8080/api/stats | jq .
```

Expected response:
```json
{
  "total_queries": 1,
  "average_processing_time_ms": 234.5,
  "total_api_calls": 1
}
```

## 7. Stop the Server

Press `Ctrl+C` in the terminal where the app is running.

**Expected:**
```
Shutting down...
MCP servers shutdown
Query Server stopped
HTTP Server stopped
```

---

## Troubleshooting

### Server won't start

**Check Java version:**
```bash
java -version
```
Should be `25.0.1` or similar Java 25

**Check Maven:**
```bash
mvn -version
```
Should be 3.8 or higher

### Getting mock mode responses when you have an API key

1. **Restart the application** (it needs to reload `.env`)
2. **Verify API key format:**
   ```bash
   grep OPENAI_API_KEY .env
   # Should be: OPENAI_API_KEY=sk-xxxxx (no spaces!)
   ```
3. **Check if API key is valid:**
   ```bash
   curl https://api.openai.com/v1/models \
     -H "Authorization: Bearer sk-your-key"
   ```
   Should return list of models, not 401 error

### Port already in use

```bash
# Kill processes on ports 8080 and 9999
lsof -ti:8080,9999 | xargs kill -9

# Then restart
mvn exec:java -Dexec.mainClass="com.example.nlp.ScalingPotatoApp"
```

### Connection refused

Wait 5 seconds after starting - server takes a moment to initialize MCP servers.

---

## What You Can Do Now

### Browser Interface
- Submit queries in the web interface
- See real-time responses
- View response execution details

### API Calls
- Use curl or any HTTP client
- Get JSON responses
- Integrate with other systems

### Advanced
- Add new MCP servers (see `MCP_IMPLEMENTATION_GUIDE.md`)
- Customize agent behavior
- Extend with new tools

---

## Next Steps

- 📚 Read `MCP_IMPLEMENTATION_GUIDE.md` for deep dive
- 🏗️ Check `INTERACTION_DIAGRAM.md` for architecture
- 🔧 See `MCP_AND_AGENTIC_ARCHITECTURE.md` for future plans
- 📖 Review `README_SCALING_POTATO.md` for full documentation

---

## Key Features at a Glance

| Feature | What It Does |
|---------|------------|
| **Agent** | AI that reasons through queries using tools |
| **MCP Servers** | Provide database access + statistics |
| **Real Mode** | Uses OpenAI API for intelligent responses |
| **Mock Mode** | Works without API key (generic responses) |
| **Web UI** | Browser interface at http://localhost:8080 |
| **REST API** | POST to http://localhost:8080/api/query |
| **Virtual Threads** | Handles 10,000+ concurrent connections |

---

**Status:** ✅ Ready to use
**API Key:** ✅ Loaded from `.env`
**Servers:** ✅ Running (HTTP + Socket)
**Agent Mode:** ✅ Real (with LLM integration)
