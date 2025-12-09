# MCP and Agentic Implementation Guide

## Quick Navigation

### For Different Audiences

**🎯 Executive/Manager** → Read: [Executive Summary](#executive-summary) + [What Changed](#what-changed-for-users)

**👨‍💼 Team Lead** → Read: [Architecture Integration](#architecture-integration) + [File Structure](#file-structure)

**👨‍💻 Developer** → Read: [What Was Implemented](#what-was-implemented) + [Extending the System](#extending-the-system)

**🧪 QA/Tester** → Read: [Testing the Agent](#testing-the-agent) + [Example Execution Flow](#example-agent-execution-flow)

**📚 Documentation** → Read: [Contents Overview](#contents-overview) + all sections

---

## Executive Summary

**What**: The application now uses AI agents that can reason through complex queries by automatically using tools and databases.

**Why**: Responses are now grounded in real data, more accurate, and can handle multi-step reasoning.

**How**: Agents analyze queries, plan which tools to use, execute them, and synthesize intelligent responses.

**Impact**: Users get better answers. Developers can easily add new data sources as tools.

**Status**: ✅ Fully implemented and tested. Ready for deployment.

---

## What Changed for Users

| Aspect | Before | After |
|--------|--------|-------|
| **Query Processing** | Single API call to LLM | Agent uses tools + reasoning |
| **Data Accuracy** | Text-based responses | Grounded in real database data |
| **Complex Queries** | Single response attempt | Multi-step reasoning (up to 10 steps) |
| **Response Quality** | Generic answers | Context-aware, tool-backed answers |
| **API Calls** | 1 per query | 2-4 per query (agent decides) |
| **User Interface** | Same endpoint | Same endpoint, but smarter responses |

---

## Contents Overview

```
SECTION A: Understanding the System
├─ Executive Summary (↑ you are here)
├─ What Changed for Users
├─ What Was Implemented
└─ How It Works

SECTION B: Architecture & Integration
├─ Architecture Integration
├─ Code Integration Points
└─ File Structure

SECTION C: Using & Extending
├─ Example Execution Flow
├─ Extending the System
└─ Quick Reference

SECTION D: Operations
├─ Testing the Agent
├─ Performance Considerations
├─ Fallback and Error Handling
└─ Future Enhancements
```

---

## Overview

The Scaling Potato application now implements **Model Context Protocol (MCP)** servers and **agentic reasoning** capabilities. This document describes the implementation, architecture, and how to extend it.

---

## How It Works (Simple Version)

### The Agent's Thinking Process

```
User: "How to grow potatoes in cold climates?"
         ↓
      Agent thinks: "I need to find information about cold-climate potato growing"
         ↓
      Agent decides: "I should search my database for similar queries and get stats"
         ↓
      Agent acts: Uses tools to fetch data
         ├─ Tool 1: Search for "cold climate potatoes" → Returns 3 relevant articles
         └─ Tool 2: Get statistics on potato growing → Returns yield data
         ↓
      Agent synthesizes: "Based on the data I found..."
         ↓
      Returns: Smart answer with sources and data
```

### What Makes It Better

| What the Agent Can Do | Benefit |
|------|---------|
| **Remember past queries** | Learns from history, finds similar problems |
| **Look up real statistics** | Grounds answers in actual data, not just training data |
| **Plan multiple steps** | Handles complex "how-to" questions with reasoning |
| **Explain its work** | Shows which tools/sources were used |

---

## What Was Implemented

### 1. MCPServer Abstract Base Class
**Location**: `src/main/java/com/example/mcp/MCPServer.java`

The foundation for all MCP servers. Provides:
- Tool registration mechanism
- Tool execution framework
- Lifecycle management (startup/shutdown)
- Logging infrastructure

```java
// Example: Register a tool
protected void registerTool("tool_name", params -> {
    // Execute tool logic
    return result;
});

// Example: Execute a tool
MCPToolResult result = server.executeTool("tool_name", params);
```

### 2. DatabaseMCPServer
**Location**: `src/main/java/com/example/mcp/DatabaseMCPServer.java`

Provides 5 database tools:

| Tool | Purpose | Parameters |
|------|---------|-----------|
| `query_history` | Retrieve past queries | `limit`, `pattern` |
| `get_stats` | Get database statistics | `time_range` |
| `save_query` | Persist query results | `query`, `response`, `processing_time` |
| `search_similar` | Find similar queries | `query_text`, `similarity_threshold` |
| `get_query_context` | Get query metadata | `query_id` |

### 3. MCPServerManager
**Location**: `src/main/java/com/example/mcp/MCPServerManager.java`

Orchestrates multiple MCP servers:
- Register/unregister servers
- Execute tools across all servers
- Discover available tools
- Manage server lifecycle
- Get unified status

```java
MCPServerManager mcpManager = new MCPServerManager();
mcpManager.registerServer(new DatabaseMCPServer(dbService));
mcpManager.startAll();

// Execute a tool
MCPToolResult result = mcpManager.executeTool("query_history", params);
```

### 4. AgentNLPService
**Location**: `src/main/java/com/example/nlp/AgentNLPService.java`

Implements agentic reasoning with tool orchestration:

#### Agentic Loop
```
User Query
    ↓
Analyze Intent
    ↓
Decide if Tools Needed
    ↓
Execute Agent Loop:
  ├─ Call LLM with context
  ├─ Extract tool names from response
  ├─ Execute tools
  ├─ Gather results
  ├─ Build new context
  └─ Repeat until done (max 10 iterations)
    ↓
Format Agent Response
    ↓
Return to User
```

#### Key Methods

**`processQueryWithAgent(userQuery)`**
- Main entry point for agentic processing
- Returns response with agent execution summary
- Includes tools used, iterations, processing time

**`analyzeQueryAndDecidTools(userQuery, context)`**
- Step 1: Agent analyzes if tools are needed
- Indicates tool requirement with `[TOOLS_NEEDED]` marker

**`agentExecutionLoop(userQuery, context)`**
- Steps 2-4: Execution loop with tool orchestration
- Iteratively calls LLM → extract tools → execute → update context
- Prevents infinite loops with max iterations

**`buildContextMessage(userQuery, context)`**
- Constructs message for next LLM call
- Includes all tool results and execution context

**`extractToolNames(response)`**
- Parses LLM response for tool references
- Validates against available MCP tools
- Returns list of tools to execute

## Architecture Integration

### Before Integration
```
User Query → NLPService → OpenAI API → Response
```

### After Integration (Agent Mode)
```
User Query
    ↓
ScalingPotatoApp initializes:
  ├─ DatabaseMCPServer
  ├─ MCPServerManager (orchestrates servers)
  └─ AgentNLPService (with MCP integration)
    ↓
WebServer (Agent Mode)
    ↓
AgentNLPService.processQueryWithAgent()
    ├─ Analyzes query intent
    ├─ Plans tool usage
    ├─ Executes tools via MCPServerManager
    │   ├─ DatabaseMCPServer
    │   └─ [Other MCP servers can be added]
    ├─ Synthesizes response with context
    └─ Returns grounded response
```

### Code Integration Points

**ScalingPotatoApp.java** (lines 50-65)
```java
// Initialize MCP Manager
MCPServerManager mcpManager = new MCPServerManager();
mcpManager.registerServer(new DatabaseMCPServer(dbService));
mcpManager.startAll();

// Create agent service
AgentNLPService agentService = new AgentNLPService(
    openaiApiKey,
    "gpt-3.5-turbo",
    mcpManager
);

// Pass to WebServer in agent mode
WebServer webServer = new WebServer(8080, agentService, dbService);
```

**WebServer.java** (lines 25-35)
```java
// Support both modes
public WebServer(int port, NLPService nlpService, DatabaseService dbService) {
    // Traditional mode
    this.useAgentMode = false;
}

public WebServer(int port, AgentNLPService agentService, DatabaseService dbService) {
    // Agent mode (new)
    this.useAgentMode = true;
}
```

## Example: Agent Execution Flow

### User Query
```
"How to grow potatoes in Montana with limited water?"
```

### Agent Steps

**Step 1: Intent Analysis**
```
Agent analyzes: Growing advice query with constraints (location, water)
Decision: Tools needed
Tools: [get_growing_conditions, search_similar]
```

**Step 2: Agent Execution Loop (Iteration 1)**
```
LLM Call: "What should the user do for potato growing in Montana?"
Response: "I should look up growing conditions and search for similar queries"
Extracted Tools: [get_growing_conditions, search_similar]
```

**Step 3: Tool Execution**
```
Tool 1: get_stats() → Returns: {
  "total_queries": 42,
  "average_processing_time_ms": 187.3,
  "total_api_calls": 35
}

Tool 2: search_similar(
  query_text="potato water requirements",
  similarity_threshold=0.7
) → Returns: [{
  "query": "How to grow potatoes in dry climate?",
  "similarity": 0.88,
  "response": "Drip irrigation recommended..."
}]
```

**Step 4: Context Building**
```
New context includes tool results and proceeds to synthesis
```

**Step 5: Synthesis**
```
Agent synthesizes final answer combining:
- Original query understanding
- Tool results
- LLM reasoning
- Historical context

Final Response:
"Based on Montana's climate and limited water conditions:
1. Choose drought-resistant varieties (from tool data)
2. Use drip irrigation (from similar query)
3. Apply mulching for moisture retention
...
[Agent Execution Summary]
Tools Used: get_stats, search_similar
Iterations: 2
Processing Time: 234ms
```

## Extending the System

### Adding a New MCP Server

```java
public class PotatoDomainMCPServer extends MCPServer {
    public PotatoDomainMCPServer() {
        super("potato-domain-mcp");
        initializeTools();
    }

    private void initializeTools() {
        // Tool 1: Get potato varieties
        registerTool("get_varieties", params -> {
            String climate = params.get("climate");
            // Logic to return varieties for climate
            return varieties;
        });

        // Tool 2: Get growing requirements
        registerTool("get_requirements", params -> {
            String varietyName = params.get("variety");
            // Logic to return requirements
            return requirements;
        });

        logger.info("PotatoDomainMCPServer initialized");
    }
}
```

Then register it:
```java
// In ScalingPotatoApp
mcpManager.registerServer(new PotatoDomainMCPServer());
```

### Customizing Agent Behavior

The agent behavior can be customized by modifying these in `AgentNLPService`:

1. **Intent Analysis Prompt** (line 200)
   - Change how agent decides if tools are needed
   - Add domain-specific keywords

2. **Tool Extraction Logic** (line 280)
   - Modify regex pattern for tool name extraction
   - Add tool name variations

3. **Max Iterations** (line 26)
   - Increase for more complex queries
   - Decrease for faster responses

4. **Tool Result Processing** (line 230)
   - Customize how results are formatted
   - Add result filtering logic

## Testing the Agent

### Real vs Mock Mode

**How to Tell What Mode You're In:**

| Indicator | Real Mode (Agent) | Mock Mode |
|-----------|------------------|-----------|
| **Has OpenAI API Key** | Yes (in `.env`) | No key or empty |
| **Response includes** | Real tool data + synthesis | Hardcoded potato advice |
| **JSON response** | `"mode": "agent"` | `"mode": "agent"` (same!) |
| **Tools Used** | Actual tools executed | Mock tools from hardcoded logic |
| **Processing Time** | 200-500ms (API calls) | 50-150ms (instant response) |
| **Tool Results** | Real data from database | Simulated/example data |
| **Response Quality** | Context-aware, specific | Generic, templated |

**Key Difference**: Check the **tool results** and **response content**, not the mode flag!

### How the Mock Mode Works

When `.env` has no `OPENAI_API_KEY`:

```java
// In AgentNLPService.callLLM()
if (apiKey == null || apiKey.isEmpty()) {
    return generateMockAgentResponse(userMessage);  // ← Uses hardcoded responses
}
```

Mock responses are **realistic but generic**:
```
User: "How to grow potatoes in cold climates?"

Mock Response:
"[TOOLS_NEEDED]
query_history, get_stats

Based on available tools, I recommend: Plant potatoes in 
well-draining soil, maintain 55-75°F temperature, provide 
1-2 inches of water weekly."
```

### Enable Real Agent Mode

**Step 1: Add your OpenAI API key to `.env`**
```bash
echo "OPENAI_API_KEY=sk-your-actual-key-here" >> .env
```

**Step 2: Restart the application**
```bash
mvn exec:java -Dexec.mainClass="com.example.nlp.ScalingPotatoApp"
```

**Step 3: Test and verify**
- Real mode will make HTTP calls to `api.openai.com`
- Processing time will be 200-500ms
- Responses will be more context-specific
- Check terminal logs for API calls

### Verify Real Mode is Working

**Check the Logs:**
```
[AgentNLPService] INFO: Starting agentic processing for: How to grow potatoes?
[AgentNLPService] INFO: Agent executing tools: get_stats, query_history
[AgentNLPService] DEBUG: Query processed with AgentNLPService
```

**Check the Response:**
```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"query":"How to grow potatoes in Montana?"}' | jq .
```

Real response shows:
- Actual tool names that were executed
- Real tool results embedded in response
- Specific, context-aware answer
- Processing time > 150ms

Mock response shows:
- Hardcoded tool suggestions
- Generic answer about potato fundamentals
- No actual database queries
- Processing time < 150ms

### Testing Both Modes

**Test 1: Check if API Key Exists**
```bash
cat .env | grep OPENAI_API_KEY
```
If it shows `OPENAI_API_KEY=sk-...` → Real mode ready
If blank or not present → Mock mode will be used

**Test 2: Compare Responses**

Run the same query twice:
1. **Without API Key** (Mock):
   ```bash
   mv .env .env.backup
   mvn exec:java ...
   # Response: Generic potato advice
   ```

2. **With API Key** (Real):
   ```bash
   mv .env.backup .env
   mvn exec:java ...
   # Response: Specific to your exact query
   ```

**Test 3: Monitor Processing Time**

Real mode with tools: `"processingTime": 234`
Mock mode: `"processingTime": 45`

### Local Testing

1. **Start the application**:
   ```bash
   mvn exec:java -Dexec.mainClass="com.example.nlp.ScalingPotatoApp"
   ```

2. **Open browser**:
   ```
   http://localhost:8080
   ```

3. **Query examples that trigger agent mode**:
   - "What potatoes should I grow in Montana?"
   - "How to prevent potato blight?"
   - "What's the yield for russet potatoes?"

4. **Check response**:
   - Response includes tool names used
   - Shows number of iterations
   - Displays processing time
   - Contains `"mode": "agent"` in JSON

### Curl Testing

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"query":"How to grow potatoes?"}'
```

Response:
```json
{
  "response": "Based on tool data, potatoes should be... [Agent Execution Summary] Tools Used: get_stats, query_history...",
  "processingTime": 340,
  "mode": "agent"
}
```

## Performance Considerations

### Agent Iterations
- Default: 10 max iterations
- Typical queries: 1-3 iterations
- Complex queries: 3-5 iterations
- Add more MCP servers to reduce iterations needed

### Tool Execution Cost
- Each tool call adds ~50-100ms
- Parallel tool execution not yet implemented
- Consider caching frequent queries

### LLM API Calls
- One call for intent analysis
- One call per iteration (usually 1-2 more)
- Total: 2-4 API calls per query
- Mock mode (no API key) uses local generation

## Fallback and Error Handling

### When API Key Missing
- Agent uses mock mode
- Provides reasonable default responses
- Tools still available (in-memory implementations)
- No external API calls

### When Tool Fails
- Agent continues with next tool
- Failed tool excluded from context
- Still produces response with available data

### Max Iterations Reached
- Returns "Max iterations reached" message
- Prevents infinite loops
- Logs warning for investigation

---

## Troubleshooting: Is It Real or Mock?

### Problem: Not Sure If Agent Is Using Real Tools

**Quick Diagnostic:**

1. **Check API Key**
   ```bash
   grep OPENAI_API_KEY .env
   ```
   - Has value starting with `sk-` → Real mode capable
   - Empty or not present → Mock mode only

2. **Check Processing Time**
   ```bash
   # Real mode (with API calls): 200-500ms
   # Mock mode (instant): 50-150ms
   ```

3. **Check Response Specificity**
   ```
   Real: "Based on our database of 42 queries with avg processing 187ms..."
   Mock: "Plant potatoes in well-draining soil, maintain 55-75°F..."
   ```

4. **Check Application Logs**
   ```
   Real: [AgentNLPService] INFO: Agent executing tools: query_history, get_stats
   Mock: [NLPService] INFO: Using mock response (no API key)
   ```

### Problem: Getting Mock Responses When I Have an API Key

**Possible Causes:**

1. **API Key not loaded**
   ```bash
   # Make sure .env exists in project root
   ls -la .env
   
   # Make sure it has the key
   cat .env | grep OPENAI_API_KEY
   ```

2. **API Key format wrong**
   ```bash
   # Should be exactly: OPENAI_API_KEY=sk-xxxxx
   # NOT: OPENAI_API_KEY = sk-xxxxx (no spaces!)
   ```

3. **Application not restarted**
   - Kill the running app
   - Restart it (new process reads `.env`)

4. **API Key invalid/expired**
   - Test key with direct API call:
   ```bash
   curl https://api.openai.com/v1/models \
     -H "Authorization: Bearer sk-your-key"
   ```
   - Should return list of models (not 401 error)

### Problem: Getting Different Responses Each Time

**This is Normal!**

Real mode responses vary because:
- LLM generates new response each call
- Tool results might differ slightly
- Agent iterations might differ
- Temperature in prompt allows variance

**To Get Consistent Results:**
- Use mock mode (always same hardcoded response)
- Add `temperature: 0` in API call (deterministic)
- Same query usually yields similar (not identical) responses

## Future Enhancements

### Immediate (Phase 1)
- ✅ DatabaseMCPServer (implemented)
- ✅ AgentNLPService (implemented)
- 🔲 Knowledge Base MCP Server (RAG integration)
- 🔲 Parallel tool execution

### Medium Term (Phase 2)
- 🔲 Potato Domain MCP Server
- 🔲 External API MCP Server (weather, market data)
- 🔲 Tool use streaming for real-time responses
- 🔲 Agent personality/behavior configuration

### Long Term (Phase 3)
- 🔲 Multi-agent orchestration
- 🔲 Specialized agents for different domains
- 🔲 Agent memory and learning
- 🔲 User feedback integration

## Related Documentation

- **Architecture**: See `INTERACTION_DIAGRAM.md` for system flows
- **Future Planning**: See `MCP_AND_AGENTIC_ARCHITECTURE.md` for architectural vision
- **Deployment**: See `README_SCALING_POTATO.md` for running instructions
- **Codebase**: See individual Java files for implementation details

## File Structure

```
src/main/java/com/example/
├── mcp/                           ← MCP Infrastructure
│   ├── MCPServer.java             ← Base class
│   ├── DatabaseMCPServer.java     ← Database tools
│   └── MCPServerManager.java      ← Orchestration
│
├── nlp/
│   ├── ScalingPotatoApp.java      ← Updated with MCP init
│   ├── NLPService.java            ← Original service
│   └── AgentNLPService.java       ← New agent service
│
└── server/
    └── WebServer.java             ← Updated with agent support
```

## Quick Reference

### Starting Agent Mode
```java
// In ScalingPotatoApp.main()
MCPServerManager mcpManager = new MCPServerManager();
mcpManager.registerServer(new DatabaseMCPServer(dbService));
mcpManager.startAll();

AgentNLPService agentService = new AgentNLPService(
    apiKey,
    "gpt-3.5-turbo",
    mcpManager
);

WebServer webServer = new WebServer(8080, agentService, dbService);
```

### Using Agentic Processing
```java
String response = agentService.processQueryWithAgent(userQuery);
// Response includes tool execution summary
```

### Adding Tools
```java
// In any MCPServer subclass
registerTool("tool_name", params -> {
    // Your tool logic
    return result;
});
```

### Managing MCP Servers
```java
mcpManager.registerServer(newServer);
mcpManager.executeTool("tool_name", params);
mcpManager.getAllAvailableTools();
mcpManager.getStatus();
mcpManager.shutdownAll();
```

---

## Key Takeaways

### For Everyone
1. **Agents are smarter**: They reason through problems using tools
2. **Tools provide data**: Real information, not hallucinations
3. **It's extensible**: Adding new tools is straightforward
4. **It's compatible**: Old code still works alongside new agent mode

### For Managers
- ✅ Improved response quality without changing user interface
- ✅ Data-grounded answers reduce errors
- ✅ Scalable architecture for future enhancements
- ✅ No breaking changes to existing systems

### For Developers
- ✅ 3 new classes to understand (MCPServer, DatabaseMCPServer, AgentNLPService)
- ✅ Clear patterns for extending with new tools
- ✅ Well-documented example implementation
- ✅ Easy to test individual components

### For QA
- ✅ Responses include execution metadata (tools used, iterations)
- ✅ Test scenarios in section "Testing the Agent"
- ✅ Agent mode can be enabled/disabled
- ✅ Fallback mode (mock) works without API keys

---

**Last Updated**: December 8, 2025
**Implementation Commit**: 22154d5
