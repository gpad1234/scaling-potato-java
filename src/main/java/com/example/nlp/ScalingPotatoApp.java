package com.example.nlp;

import com.example.db.DatabaseService;
import com.example.server.QueryServer;
import com.example.server.WebServer;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * Main entry point for Scaling Potato NLP Application
 * Starts the socket server, HTTP server, and manages NLP/Database services
 */
public class ScalingPotatoApp {
    private static final Logger logger = LoggerFactory.getLogger(ScalingPotatoApp.class);
    
    private static QueryServer server;
    private static WebServer webServer;
    
    public static void main(String[] args) {
        logger.info("=== Scaling Potato NLP Application ===");
        logger.info("Java Version: {}", System.getProperty("java.version"));
        
        try {
            // Load environment variables from .env file
            Dotenv dotenv = Dotenv.load();
            
            String openaiApiKey = dotenv.get("OPENAI_API_KEY");
            String dbUrl = dotenv.get("DATABASE_URL", "jdbc:h2:mem:scalingpotato");
            String dbUser = dotenv.get("DATABASE_USER", "sa");
            String dbPassword = dotenv.get("DATABASE_PASSWORD", "");
            int port = Integer.parseInt(dotenv.get("PORT", "9999"));
            int threadPoolSize = Integer.parseInt(dotenv.get("THREAD_POOL_SIZE", "50"));
            
            logger.info("Configuration loaded:");
            logger.info("  Port: {}", port);
            logger.info("  Thread Pool Size: {}", threadPoolSize);
            logger.info("  Database: {}", dbUrl);
            logger.info("  OpenAI API configured: {}", openaiApiKey != null && !openaiApiKey.isEmpty());
            
            // Initialize services
            NLPService nlpService = new NLPService(openaiApiKey, "gpt-3.5-turbo");
            DatabaseService dbService = new DatabaseService(dbUrl, dbUser, dbPassword);
            
            // Create and start socket server
            server = new QueryServer(port, threadPoolSize, nlpService, dbService);
            server.start();
            
            // Create and start HTTP/web server
            webServer = new WebServer(8080, nlpService, dbService);
            webServer.start();
            
            logger.info("Server started successfully!");
            logger.info("Open http://localhost:8080 in your browser");
            logger.info("Or connect to socket server on port {}", port);
            logger.info("Press 'q' to shutdown the servers");
            
            // Keep server running until user quits
            Scanner scanner = new Scanner(System.in);
            String input;
            while (true) {
                input = scanner.nextLine().trim().toLowerCase();
                if (input.equals("q") || input.equals("quit")) {
                    logger.info("Shutting down...");
                    server.stop();
                    webServer.stop();
                    break;
                }
            }
            
        } catch (Exception e) {
            logger.error("Fatal error", e);
            System.exit(1);
        }
    }
}
