package com.example.mcp;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCPServerManager - orchestrates multiple MCP servers
 * Provides unified interface for agents to discover and use tools across all servers
 */
public class MCPServerManager {
    private final Map<String, MCPServer> servers;
    private final MCPServer.Logger logger;
    private boolean isRunning;

    public MCPServerManager() {
        this.servers = new ConcurrentHashMap<>();
        this.logger = new MCPServer.Logger("MCPServerManager");
        this.isRunning = false;
    }

    /**
     * Register an MCP server
     */
    public void registerServer(MCPServer server) {
        servers.put(server.getName(), server);
        logger.info("Registered MCP server: " + server.getName());
    }

    /**
     * Start all registered servers
     */
    public void startAll() {
        servers.forEach((name, server) -> {
            try {
                server.startup();
            } catch (Exception e) {
                logger.error("Failed to start " + name + ": " + e.getMessage());
            }
        });
        isRunning = true;
        logger.info("All MCP servers started. Total: " + servers.size());
    }

    /**
     * Shutdown all registered servers
     */
    public void shutdownAll() {
        servers.forEach((name, server) -> {
            try {
                server.shutdown();
            } catch (Exception e) {
                logger.error("Failed to shutdown " + name + ": " + e.getMessage());
            }
        });
        isRunning = false;
        logger.info("All MCP servers shutdown");
    }

    /**
     * Execute a tool across all servers
     * Returns first successful result found
     */
    public MCPServer.MCPToolResult executeTool(String toolName, Map<String, String> params) {
        for (MCPServer server : servers.values()) {
            if (server.getTools().containsKey(toolName)) {
                MCPServer.MCPToolResult result = server.executeTool(toolName, params);
                if (result.success) {
                    return result;
                }
            }
        }
        
        return new MCPServer.MCPToolResult(
            false,
            "Tool not found in any MCP server: " + toolName,
            null
        );
    }

    /**
     * Find which server provides a tool
     */
    public MCPServer findServerForTool(String toolName) {
        for (MCPServer server : servers.values()) {
            if (server.getTools().containsKey(toolName)) {
                return server;
            }
        }
        return null;
    }

    /**
     * Get all available tools across all servers
     */
    public Map<String, ToolInfo> getAllAvailableTools() {
        Map<String, ToolInfo> allTools = new LinkedHashMap<>();
        
        for (MCPServer server : servers.values()) {
            for (MCPServer.MCPTool tool : server.getTools().values()) {
                allTools.put(
                    tool.getName(),
                    new ToolInfo(tool.getName(), server.getName())
                );
            }
        }
        
        return allTools;
    }

    /**
     * Get available tools from specific server
     */
    public List<String> getServerTools(String serverName) {
        MCPServer server = servers.get(serverName);
        if (server != null) {
            return new ArrayList<>(server.getTools().keySet());
        }
        return Collections.emptyList();
    }

    /**
     * Get all registered servers
     */
    public Collection<MCPServer> getAllServers() {
        return new ArrayList<>(servers.values());
    }

    /**
     * Check if manager is running
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Get status report
     */
    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("MCPServerManager Status:\n");
        sb.append("  Running: ").append(isRunning).append("\n");
        sb.append("  Servers: ").append(servers.size()).append("\n");
        
        for (MCPServer server : servers.values()) {
            sb.append("    - ").append(server.getName())
              .append(" (").append(server.getTools().size()).append(" tools)\n");
        }
        
        return sb.toString();
    }

    /**
     * Information about a tool
     */
    public static class ToolInfo {
        public final String name;
        public final String serverName;

        public ToolInfo(String name, String serverName) {
            this.name = name;
            this.serverName = serverName;
        }

        @Override
        public String toString() {
            return name + " (from " + serverName + ")";
        }
    }
}
