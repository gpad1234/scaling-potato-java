package com.example.server;

import com.example.db.DatabaseService;
import com.example.nlp.NLPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Handles individual client connections
 * Receives queries, processes them via NLP service, stores results in database
 */
public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    
    private final Socket socket;
    private final NLPService nlpService;
    private final DatabaseService dbService;
    
    public ClientHandler(Socket socket, NLPService nlpService, DatabaseService dbService) {
        this.socket = socket;
        this.nlpService = nlpService;
        this.dbService = dbService;
    }
    
    @Override
    public void run() {
        try {
            handleClient();
        } catch (Exception e) {
            logger.error("Error handling client: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
    }
    
    /**
     * Handle client communication
     */
    private void handleClient() throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            
            String query;
            while ((query = reader.readLine()) != null && !query.isEmpty()) {
                logger.debug("Received query: {}", query);
                
                // Check for special commands
                if (query.equalsIgnoreCase("STATS")) {
                    handleStatsRequest(writer);
                } else if (query.equalsIgnoreCase("EXIT")) {
                    writer.println("Goodbye!");
                    break;
                } else {
                    handleQueryRequest(query, writer);
                }
            }
            
        } catch (IOException e) {
            logger.error("Error in client communication", e);
        }
    }
    
    /**
     * Process a query request
     */
    private void handleQueryRequest(String query, PrintWriter writer) {
        long startTime = System.currentTimeMillis();
        
        // Process NLP query
        String response = nlpService.processQuery(query);
        
        long processingTime = System.currentTimeMillis() - startTime;
        
        // Store in database
        dbService.saveQueryResult(query, response, processingTime);
        
        // Send response to client
        writer.println("RESPONSE:" + response);
        writer.println("TIME:" + processingTime + "ms");
        writer.println("---");
        
        logger.info("Query processed in {} ms", processingTime);
    }
    
    /**
     * Handle statistics request
     */
    private void handleStatsRequest(PrintWriter writer) {
        DatabaseService.DatabaseStats stats = dbService.getStats();
        writer.println("STATS:");
        writer.println("Total queries: " + stats.totalQueries);
        writer.println("Average processing time: " + String.format("%.2f", stats.averageProcessingTimeMs) + "ms");
        writer.println("---");
    }
    
    /**
     * Close the client connection
     */
    private void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error("Error closing socket", e);
        }
    }
}
