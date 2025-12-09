package com.example.server;

import com.example.db.DatabaseService;
import com.example.nlp.NLPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * HTTP Server that serves the frontend and provides REST API
 */
public class WebServer {
    private static final Logger logger = LoggerFactory.getLogger(WebServer.class);
    
    private final int port;
    private final NLPService nlpService;
    private final DatabaseService dbService;
    private HttpServer httpServer;
    
    public WebServer(int port, NLPService nlpService, DatabaseService dbService) {
        this.port = port;
        this.nlpService = nlpService;
        this.dbService = dbService;
    }
    
    /**
     * Start the HTTP server
     */
    public void start() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            
            // Register API endpoints FIRST (more specific paths must come first)
            httpServer.createContext("/api/query", new QueryHandler(nlpService, dbService));
            httpServer.createContext("/api/stats", new StatsHandler(dbService));
            
            // Serve frontend HTML as catch-all (less specific path comes last)
            httpServer.createContext("/", exchange -> serveFile(exchange, "index.html", "text/html"));
            
            httpServer.setExecutor(null); // Default executor
            httpServer.start();
            
            logger.info("HTTP Server started on port {}", port);
            
        } catch (IOException e) {
            logger.error("Error starting HTTP server", e);
        }
    }
    
    /**
     * Serve a static file
     */
    private void serveFile(HttpExchange exchange, String filename, String contentType) throws IOException {
        try {
            byte[] fileContent = Files.readAllBytes(
                Paths.get("src/main/resources/" + filename)
            );
            
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, fileContent.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileContent);
            }
        } catch (IOException e) {
            logger.error("Error serving file: {}", filename, e);
            String response = "404 Not Found";
            exchange.sendResponseHeaders(404, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    
    /**
     * Stop the HTTP server
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            logger.info("HTTP Server stopped");
        }
    }
    
    /**
     * Handler for query API
     */
    private static class QueryHandler implements HttpHandler {
        private final NLPService nlpService;
        private final DatabaseService dbService;
        
        QueryHandler(NLPService nlpService, DatabaseService dbService) {
            this.nlpService = nlpService;
            this.dbService = dbService;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Handle CORS preflight
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            
            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    // Read request body
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr);
                    StringBuilder body = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        body.append(line);
                    }
                    
                    // Simple JSON parsing (in production use proper JSON library)
                    String query = extractJsonField(body.toString(), "query");
                    
                    long startTime = System.currentTimeMillis();
                    String response = nlpService.processQuery(query);
                    long processingTime = System.currentTimeMillis() - startTime;
                    
                    try {
                        dbService.saveQueryResult(query, response, processingTime);
                    } catch (Exception e) {
                        logger.warn("Failed to save query result to database: {}", e.getMessage());
                        // Continue even if database save fails
                    }
                    
                    String jsonResponse = String.format(
                        "{\"response\": \"%s\", \"processingTime\": %d}",
                        escapeJson(response),
                        processingTime
                    );
                    
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, jsonResponse.length());
                    
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(jsonResponse.getBytes());
                    }
                } catch (Exception e) {
                    logger.error("Error handling query request", e);
                    String errorResponse = "{\"error\": \"Internal server error\"}";
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(500, errorResponse.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(errorResponse.getBytes());
                    }
                }
            } else {
                exchange.sendResponseHeaders(405, 0);
            }
        }
    }
    
    /**
     * Handler for stats API
     */
    private static class StatsHandler implements HttpHandler {
        private final DatabaseService dbService;
        
        StatsHandler(DatabaseService dbService) {
            this.dbService = dbService;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Handle CORS preflight
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            
            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    DatabaseService.DatabaseStats stats = dbService.getStats();
                    
                    String jsonResponse = String.format(
                        "{\"totalQueries\": %d, \"averageProcessingTimeMs\": %.2f}",
                        stats.totalQueries,
                        stats.averageProcessingTimeMs
                    );
                    
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, jsonResponse.length());
                    
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(jsonResponse.getBytes());
                    }
                } catch (Exception e) {
                    logger.error("Error handling stats request", e);
                    String errorResponse = "{\"error\": \"Internal server error\"}";
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(500, errorResponse.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(errorResponse.getBytes());
                    }
                }
            } else {
                exchange.sendResponseHeaders(405, 0);
            }
        }
    }
    
    /**
     * Extract field value from simple JSON
     */
    private static String extractJsonField(String json, String field) {
        String pattern = "\"" + field + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return "";
        
        start += pattern.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : "";
    }
    
    /**
     * Escape special characters for JSON
     */
    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f");
    }
}
