package com.example.mcp;

import java.util.*;
import java.util.function.Function;

/**
 * Abstract base class for Model Context Protocol (MCP) servers.
 * MCP servers expose tools/resources that agentic systems can use.
 */
public abstract class MCPServer {
    protected final String name;
    protected final Map<String, MCPTool> tools;
    protected final Logger logger;

    public MCPServer(String name) {
        this.name = name;
        this.tools = new LinkedHashMap<>();
        this.logger = new Logger(this.getClass().getSimpleName());
    }

    /**
     * Register a tool that this MCP server provides
     */
    protected void registerTool(String toolName, MCPToolHandler handler) {
        tools.put(toolName, new MCPTool(toolName, handler));
        logger.info("Registered tool: " + toolName);
    }

    /**
     * Execute a tool by name with parameters
     */
    public MCPToolResult executeTool(String toolName, Map<String, String> params) {
        MCPTool tool = tools.get(toolName);
        if (tool == null) {
            return new MCPToolResult(false, "Tool not found: " + toolName, null);
        }

        try {
            Object result = tool.execute(params);
            return new MCPToolResult(true, "Success", result);
        } catch (Exception e) {
            logger.error("Error executing tool " + toolName + ": " + e.getMessage());
            return new MCPToolResult(false, e.getMessage(), null);
        }
    }

    /**
     * Get all available tools
     */
    public Map<String, MCPTool> getTools() {
        return new HashMap<>(tools);
    }

    /**
     * Get server name
     */
    public String getName() {
        return name;
    }

    /**
     * Lifecycle method - called when server starts
     */
    public void startup() {
        logger.info("MCP Server started: " + name);
    }

    /**
     * Lifecycle method - called when server shuts down
     */
    public void shutdown() {
        logger.info("MCP Server stopped: " + name);
    }

    // Inner classes for tool definitions

    public static class MCPTool {
        public final String name;
        private final MCPToolHandler handler;

        public MCPTool(String name, MCPToolHandler handler) {
            this.name = name;
            this.handler = handler;
        }

        public Object execute(Map<String, String> params) throws Exception {
            return handler.handle(params);
        }

        public String getName() {
            return name;
        }
    }

    public static class MCPToolResult {
        public final boolean success;
        public final String message;
        public final Object data;

        public MCPToolResult(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        @Override
        public String toString() {
            return "MCPToolResult{" +
                    "success=" + success +
                    ", message='" + message + '\'' +
                    ", data=" + data +
                    '}';
        }
    }

    @FunctionalInterface
    public interface MCPToolHandler {
        Object handle(Map<String, String> params) throws Exception;
    }

    // Simple logger implementation
    public static class Logger {
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

        public void debug(String message) {
            System.out.println("[" + component + "] DEBUG: " + message);
        }
    }
}
