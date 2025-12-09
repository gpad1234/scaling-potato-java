package com.example.nlp;

import com.example.mcp.MCPServerManager;
import com.example.mcp.MCPServer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AgentNLPService - Enhanced NLP service with agentic reasoning
 * Uses tools from MCP servers to ground responses and execute multi-step queries
 *
 * Agentic Loop Pattern:
 * 1. Agent understands user intent
 * 2. Agent identifies relevant tools
 * 3. Agent plans tool execution
 * 4. Agent executes tools and processes results
 * 5. Agent synthesizes final response
 */
public class AgentNLPService {
    private final String apiKey;
    private final String model;
    private final MCPServerManager mcpManager;
    private final int maxAgentIterations;
    private final Logger logger;

    // Agent execution context
    private static class AgentContext {
        String userQuery;
        List<String> executedTools;
        List<Map<String, Object>> toolResults;
        int iterations;

        AgentContext(String userQuery) {
            this.userQuery = userQuery;
            this.executedTools = new ArrayList<>();
            this.toolResults = new ArrayList<>();
            this.iterations = 0;
        }
    }

    public AgentNLPService(String apiKey, String model, MCPServerManager mcpManager) {
        this.apiKey = apiKey;
        this.model = model != null ? model : "gpt-3.5-turbo";
        this.mcpManager = mcpManager;
        this.maxAgentIterations = 10;
        this.logger = new Logger("AgentNLPService");
    }

    /**
     * Process query with agentic reasoning
     * Agent can autonomously decide to use tools based on query intent
     */
    public String processQueryWithAgent(String userQuery) {
        long startTime = System.currentTimeMillis();
        
        try {
            AgentContext context = new AgentContext(userQuery);
            logger.info("Starting agentic processing for: " + userQuery);

            // Step 1: Agent analyzes intent and decides if tools are needed
            String initialResponse = analyzeQueryAndDecidTools(userQuery, context);
            
            if (initialResponse != null && shouldUseTools(initialResponse)) {
                // Step 2-4: Agent execution loop with tool use
                String finalResponse = agentExecutionLoop(userQuery, context);
                return formatAgentResponse(finalResponse, context, startTime);
            } else {
                // Direct response without tools
                return initialResponse;
            }
        } catch (Exception e) {
            logger.error("Error in agentic processing: " + e.getMessage());
            return "Agent error: " + e.getMessage();
        }
    }

    /**
     * Step 1: Analyze query and decide if tools are needed
     */
    private String analyzeQueryAndDecidTools(String userQuery, AgentContext context) {
        String systemPrompt = 
            "You are an agentic system helping with potato-related queries.\n" +
            "Analyze the user query and decide if you need to use tools.\n" +
            "Available tools:\n" +
            getMCPToolsDescription() + "\n\n" +
            "If tools would help, respond with:\n" +
            "[TOOLS_NEEDED]\n" +
            "tool_name1, tool_name2, ...\n" +
            "\n" +
            "Otherwise provide a direct answer.";

        String userMessage = "Query: " + userQuery;
        String response = callLLM(systemPrompt, userMessage, 3);
        
        context.iterations++;
        return response;
    }

    /**
     * Step 2-4: Execute agentic loop with tool orchestration
     */
    private String agentExecutionLoop(String userQuery, AgentContext context) {
        while (context.iterations < maxAgentIterations) {
            context.iterations++;
            
            // Get current context message with tool results
            String contextMessage = buildContextMessage(userQuery, context);
            
            // Call LLM with available tools
            String llmResponse = callLLMWithContext(contextMessage);
            
            // Check if LLM wants to execute tools
            List<String> toolsToExecute = extractToolNames(llmResponse);
            
            if (toolsToExecute.isEmpty()) {
                // No more tools to execute, agent reached conclusion
                logger.info("Agent concluded after " + context.iterations + " iterations");
                return llmResponse;
            }
            
            // Execute identified tools
            logger.info("Agent executing tools: " + toolsToExecute);
            for (String toolName : toolsToExecute) {
                MCPServer.MCPToolResult result = mcpManager.executeTool(
                    toolName,
                    extractToolParams(llmResponse, toolName)
                );
                
                if (result.success) {
                    context.executedTools.add(toolName);
                    context.toolResults.add(Map.of(
                        "tool", toolName,
                        "result", result.data,
                        "timestamp", System.currentTimeMillis()
                    ));
                    logger.info("Tool executed: " + toolName + " -> " + result.data);
                } else {
                    logger.error("Tool failed: " + toolName + " - " + result.message);
                }
            }
        }
        
        logger.warn("Agent reached max iterations (" + maxAgentIterations + ")");
        return "Agent max iterations reached";
    }

    /**
     * Build context message with tool results for next LLM call
     */
    private String buildContextMessage(String userQuery, AgentContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("User Query: ").append(userQuery).append("\n\n");
        
        if (!context.toolResults.isEmpty()) {
            sb.append("Tool Execution Results:\n");
            for (Map<String, Object> result : context.toolResults) {
                sb.append("- ").append(result.get("tool")).append(": ")
                  .append(result.get("result")).append("\n");
            }
            sb.append("\n");
        }
        
        sb.append("Now synthesize these results into a comprehensive answer.");
        return sb.toString();
    }

    /**
     * Determine if response indicates tools are needed
     */
    private boolean shouldUseTools(String response) {
        return response.contains("[TOOLS_NEEDED]") || 
               response.contains("tool") || 
               response.contains("query_");
    }

    /**
     * Extract tool names from LLM response
     */
    private List<String> extractToolNames(String response) {
        List<String> tools = new ArrayList<>();
        
        // Pattern: look for tool names like "query_history", "get_stats", etc.
        Pattern pattern = Pattern.compile("(\\w+_\\w+|get_\\w+|search_\\w+|save_\\w+)");
        Matcher matcher = pattern.matcher(response.toLowerCase());
        
        Map<String, MCPServerManager.ToolInfo> availableTools = mcpManager.getAllAvailableTools();
        
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (availableTools.containsKey(candidate)) {
                tools.add(candidate);
            }
        }
        
        return tools;
    }

    /**
     * Extract parameters for a specific tool from LLM response
     */
    private Map<String, String> extractToolParams(String response, String toolName) {
        Map<String, String> params = new HashMap<>();
        
        // Simple extraction - look for key=value patterns
        Pattern pattern = Pattern.compile(toolName + "\\s*\\(([^)]*)\\)");
        Matcher matcher = pattern.matcher(response);
        
        if (matcher.find()) {
            String paramsStr = matcher.group(1);
            for (String param : paramsStr.split(",")) {
                String[] kv = param.trim().split("=");
                if (kv.length == 2) {
                    params.put(kv[0].trim(), kv[1].trim());
                }
            }
        }
        
        return params;
    }

    /**
     * Get description of all MCP tools for the agent
     */
    private String getMCPToolsDescription() {
        StringBuilder sb = new StringBuilder();
        Map<String, MCPServerManager.ToolInfo> tools = mcpManager.getAllAvailableTools();
        
        if (tools.isEmpty()) {
            return "No MCP tools available";
        }
        
        for (MCPServerManager.ToolInfo tool : tools.values()) {
            sb.append("- ").append(tool.name).append(" (from ").append(tool.serverName).append(")\n");
        }
        
        return sb.toString();
    }

    /**
     * Call LLM with context about available tools
     */
    private String callLLMWithContext(String contextMessage) {
        String systemPrompt = 
            "You are an agentic system processing potato-related queries.\n" +
            "You have access to these tools:\n" +
            getMCPToolsDescription() + "\n" +
            "Based on the context and tool results, provide a comprehensive answer.\n" +
            "If you need more information, request additional tools.";
        
        return callLLM(systemPrompt, contextMessage, 3);
    }

    /**
     * Format final response with agent execution details
     */
    private String formatAgentResponse(String response, AgentContext context, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;
        
        StringBuilder sb = new StringBuilder();
        sb.append(response).append("\n\n");
        sb.append("---\n");
        sb.append("[Agent Execution Summary]\n");
        sb.append("Tools Used: ").append(context.executedTools.isEmpty() ? "None" : String.join(", ", context.executedTools)).append("\n");
        sb.append("Iterations: ").append(context.iterations).append("\n");
        sb.append("Processing Time: ").append(processingTime).append("ms\n");
        
        return sb.toString();
    }

    /**
     * Call OpenAI API (fallback to mock if no key)
     */
    private String callLLM(String systemPrompt, String userMessage, int maxRetries) {
        if (apiKey == null || apiKey.isEmpty()) {
            return generateMockAgentResponse(userMessage);
        }

        try {
            String requestBody = buildRequestBody(systemPrompt, userMessage);
            URL url = new URL("https://api.openai.com/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                return parseOpenAIResponse(conn.getInputStream());
            } else if (responseCode == 429 && maxRetries > 0) {
                Thread.sleep(1000);
                return callLLM(systemPrompt, userMessage, maxRetries - 1);
            } else {
                return "API Error: " + responseCode;
            }
        } catch (Exception e) {
            logger.error("OpenAI API error: " + e.getMessage());
            return generateMockAgentResponse(userMessage);
        }
    }

    /**
     * Build JSON request body for OpenAI API
     */
    private String buildRequestBody(String systemPrompt, String userMessage) {
        return "{\n" +
               "  \"model\": \"" + model + "\",\n" +
               "  \"messages\": [\n" +
               "    {\"role\": \"system\", \"content\": \"" + escapeJson(systemPrompt) + "\"},\n" +
               "    {\"role\": \"user\", \"content\": \"" + escapeJson(userMessage) + "\"}\n" +
               "  ],\n" +
               "  \"temperature\": 0.7,\n" +
               "  \"max_tokens\": 500\n" +
               "}";
    }

    /**
     * Parse OpenAI response
     */
    private String parseOpenAIResponse(InputStream is) throws Exception {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        // Extract content from JSON response
        Pattern pattern = Pattern.compile("\"content\": \"([^\"]+)\"");
        Matcher matcher = pattern.matcher(response.toString());
        if (matcher.find()) {
            return unescape(matcher.group(1));
        }
        
        return response.toString();
    }

    /**
     * Generate mock agent response (when API key unavailable)
     */
    private String generateMockAgentResponse(String userMessage) {
        String lowerQuery = userMessage.toLowerCase();
        
        if (lowerQuery.contains("grow") || lowerQuery.contains("planting")) {
            return "[TOOLS_NEEDED]\nget_growing_conditions, query_history\n\n" +
                   "Based on available tools, I recommend: Plant potatoes in well-draining soil, " +
                   "maintain 55-75°F temperature, provide 1-2 inches of water weekly.";
        } else if (lowerQuery.contains("pest") || lowerQuery.contains("disease")) {
            return "[TOOLS_NEEDED]\nquery_history, get_stats\n\n" +
                   "Common potato pests: Colorado beetles (manual removal or neem oil), " +
                   "aphids (insecticidal soap), blight (fungicide application). Check query history for solutions.";
        } else if (lowerQuery.contains("yield") || lowerQuery.contains("production")) {
            return "[TOOLS_NEEDED]\nget_stats\n\n" +
                   "Potato yields depend on variety, soil quality, water, and sunlight. " +
                   "Average yield: 10-20 tons per hectare. See historical data for benchmarks.";
        }
        
        return "[TOOLS_NEEDED]\nquery_history, get_stats\n\n" +
               "I found relevant information in the query history. " +
               "Based on similar queries: " +
               "Potatoes are versatile crops requiring cool weather, adequate water, and well-draining soil. " +
               "Most varieties mature in 60-90 days. Check system stats for detailed metrics.";
    }

    /**
     * Escape JSON special characters
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t")
                  .replace("\\", "\\\\");
    }

    /**
     * Unescape JSON special characters
     */
    private String unescape(String str) {
        return str.replace("\\n", "\n")
                  .replace("\\t", "\t")
                  .replace("\\\"", "\"")
                  .replace("\\\\", "\\");
    }

    /**
     * Logger implementation
     */
    private static class Logger {
        private final String component;

        public Logger(String component) {
            this.component = component;
        }

        public void info(String message) {
            System.out.println("[" + component + "] INFO: " + message);
        }

        public void error(String message) {
            System.err.println("[" + component + "] ERROR: " + message);
        }

        public void warn(String message) {
            System.out.println("[" + component + "] WARN: " + message);
        }
    }
}
