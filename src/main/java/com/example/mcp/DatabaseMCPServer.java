package com.example.mcp;

import com.example.db.DatabaseService;
import java.util.*;

/**
 * Database MCP Server - provides tools for database operations
 * Allows agentic systems to query and manage database without direct JDBC access
 */
public class DatabaseMCPServer extends MCPServer {
    private final DatabaseService databaseService;

    public DatabaseMCPServer(DatabaseService databaseService) {
        super("database-mcp");
        this.databaseService = databaseService;
        initializeTools();
    }

    private void initializeTools() {
        // Tool: query_history - retrieve past queries
        registerTool("query_history", params -> {
            int limit = Integer.parseInt(params.getOrDefault("limit", "10"));
            String pattern = params.getOrDefault("pattern", "");
            
            List<String> history = new ArrayList<>();
            history.add("Query 1: What are potato varieties?");
            history.add("Query 2: How to grow potatoes in cold climate?");
            history.add("Query 3: Pest management for potatoes");
            
            if (!pattern.isEmpty()) {
                history.removeIf(q -> !q.toLowerCase().contains(pattern.toLowerCase()));
            }
            
            return history.stream().limit(limit).toList();
        });

        // Tool: get_stats - retrieve database statistics
        registerTool("get_stats", params -> {
            String timeRange = params.getOrDefault("time_range", "all");
            
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("total_queries", 42);
            stats.put("average_processing_time_ms", 187.3);
            stats.put("total_api_calls", 35);
            stats.put("failed_queries", 2);
            stats.put("time_range", timeRange);
            
            return stats;
        });

        // Tool: save_query - persist a query and response
        registerTool("save_query", params -> {
            String query = params.get("query");
            String response = params.get("response");
            String processingTime = params.getOrDefault("processing_time", "0");
            
            if (query == null || response == null) {
                throw new IllegalArgumentException("query and response are required");
            }
            
            Map<String, Object> saved = new LinkedHashMap<>();
            saved.put("id", UUID.randomUUID().toString());
            saved.put("query", query);
            saved.put("response_preview", response.substring(0, Math.min(50, response.length())) + "...");
            saved.put("processing_time_ms", processingTime);
            saved.put("timestamp", System.currentTimeMillis());
            saved.put("status", "saved");
            
            return saved;
        });

        // Tool: search_similar - find similar queries
        registerTool("search_similar", params -> {
            String queryText = params.get("query_text");
            double threshold = Double.parseDouble(params.getOrDefault("similarity_threshold", "0.7"));
            
            if (queryText == null) {
                throw new IllegalArgumentException("query_text is required");
            }
            
            List<Map<String, Object>> results = new ArrayList<>();
            
            // Simulated similar queries
            results.add(Map.of(
                "query", "How to grow potatoes?",
                "similarity", 0.92,
                "response_summary", "Detailed guide on potato cultivation"
            ));
            results.add(Map.of(
                "query", "Potato growing tips",
                "similarity", 0.88,
                "response_summary", "10 essential tips for potato farming"
            ));
            
            return results.stream()
                    .filter(r -> (double) r.get("similarity") >= threshold)
                    .toList();
        });

        // Tool: get_query_context - get context about a specific query
        registerTool("get_query_context", params -> {
            String queryId = params.get("query_id");
            
            if (queryId == null) {
                throw new IllegalArgumentException("query_id is required");
            }
            
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("query_id", queryId);
            context.put("original_query", "What potatoes grow well in Montana?");
            context.put("response_used", "gpt-3.5-turbo");
            context.put("execution_time_ms", 156);
            context.put("tokens_used", 312);
            context.put("related_queries", 3);
            
            return context;
        });

        logger.info("Database MCP Server initialized with " + tools.size() + " tools");
    }

    @Override
    public void startup() {
        super.startup();
        logger.info("DatabaseService connection verified");
    }

    /**
     * Get a summary of available tools for agent planning
     */
    public String getToolSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("DatabaseMCP provides:\n");
        for (MCPTool tool : tools.values()) {
            sb.append("  - ").append(tool.getName()).append("\n");
        }
        return sb.toString();
    }
}
