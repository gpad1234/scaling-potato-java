package com.example.nlp;

import com.example.db.DatabaseService;
import com.example.mcp.DatabaseMCPServer;
import com.example.mcp.MCPServerManager;
import com.example.server.QueryServer;
import com.example.server.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Main entry point for Scaling Potato NLP Application
 * Starts the socket server, HTTP server, and manages NLP/Database services
 * Enhanced with MCP (Model Context Protocol) servers and agentic NLP service
 */
public class ScalingPotatoApp {
    private static final Logger logger = LoggerFactory.getLogger(ScalingPotatoApp.class);
    
    private static QueryServer server;
    private static WebServer webServer;
    private static MCPServerManager mcpManager;
    
    public static void main(String[] args) {
        logger.info("=== Scaling Potato NLP Application ===");
        logger.info("Java Version: {}", System.getProperty("java.version"));
        
        try {
            // Load environment variables from .env file
            Map<String, String> env = loadEnv();
            
            String openaiApiKey = env.getOrDefault("OPENAI_API_KEY", "");
            String dbUrl = env.getOrDefault("DATABASE_URL", "jdbc:h2:mem:scalingpotato");
            String dbUser = env.getOrDefault("DATABASE_USER", "sa");
            String dbPassword = env.getOrDefault("DATABASE_PASSWORD", "");
            int port = Integer.parseInt(env.getOrDefault("PORT", "9999"));
            int threadPoolSize = Integer.parseInt(env.getOrDefault("THREAD_POOL_SIZE", "50"));
            
            logger.info("Configuration loaded:");
            logger.info("  Port: {}", port);
            logger.info("  Thread Pool Size: {}", threadPoolSize);
            logger.info("  Database: {}", dbUrl);
            logger.info("  OpenAI API configured: {}", openaiApiKey != null && !openaiApiKey.isEmpty());
            
            // Initialize services
            NLPService nlpService = new NLPService(openaiApiKey, "gpt-3.5-turbo");
            DatabaseService dbService = new DatabaseService(dbUrl, dbUser, dbPassword);
            
            // Initialize MCP Manager with Database MCP Server
            logger.info("Initializing MCP (Model Context Protocol) servers...");
            mcpManager = new MCPServerManager();
            mcpManager.registerServer(new DatabaseMCPServer(dbService));
            mcpManager.startAll();
            
            // Create agent-based NLP service with tool capabilities
            AgentNLPService agentService = new AgentNLPService(openaiApiKey, "gpt-3.5-turbo", mcpManager);
            logger.info("AgentNLPService initialized with {} MCP servers", mcpManager.getAllServers().size());
            logger.info(mcpManager.getStatus());
            
            // Create and start socket server
            server = new QueryServer(port, threadPoolSize, nlpService, dbService);
            server.start();
            
            // Create and start HTTP/web server with AGENT mode
            webServer = new WebServer(8080, agentService, dbService);
            webServer.start();
            
            logger.info("Server started successfully!");
            logger.info("Open http://localhost:8080 in your browser");
            logger.info("Or connect to socket server on port {}", port);
            logger.info("Press Ctrl+C to shutdown the servers");
            
            // Add shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down...");
                if (mcpManager != null) mcpManager.shutdownAll();
                if (server != null) server.stop();
                if (webServer != null) webServer.stop();
            }));
            
            // Keep the main thread alive
            Object lock = new Object();
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    logger.info("Interrupted");
                    server.stop();
                    webServer.stop();
                }
            }
            
        } catch (Exception e) {
            logger.error("Fatal error", e);
            System.exit(1);
        }
    }
    
    /**
     * Load environment variables from .env file
     */
    private static Map<String, String> loadEnv() {
        Map<String, String> env = new HashMap<>();
        
        try {
            File envFile = new File(".env");
            if (envFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            String[] parts = line.split("=", 2);
                            if (parts.length == 2) {
                                env.put(parts[0].trim(), parts[1].trim());
                            }
                        }
                    }
                }
                logger.info("Loaded environment from .env file");
            } else {
                logger.warn(".env file not found, using defaults");
            }
        } catch (Exception e) {
            logger.warn("Error loading .env file: {}", e.getMessage());
        }
        
        return env;
    }
}
