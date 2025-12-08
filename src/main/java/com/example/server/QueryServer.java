package com.example.server;

import com.example.db.DatabaseService;
import com.example.nlp.NLPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Socket Server that handles NLP queries from clients
 * Uses a thread pool to process concurrent requests
 */
public class QueryServer {
    private static final Logger logger = LoggerFactory.getLogger(QueryServer.class);
    
    private final int port;
    private final int threadPoolSize;
    private final NLPService nlpService;
    private final DatabaseService dbService;
    private ExecutorService threadPool;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    
    public QueryServer(int port, int threadPoolSize, NLPService nlpService, DatabaseService dbService) {
        this.port = port;
        this.threadPoolSize = threadPoolSize;
        this.nlpService = nlpService;
        this.dbService = dbService;
    }
    
    /**
     * Start the server and begin accepting connections
     */
    public void start() {
        try {
            threadPool = Executors.newFixedThreadPool(threadPoolSize);
            serverSocket = new ServerSocket(port);
            running = true;
            
            logger.info("Query Server started on port {} with thread pool size {}", port, threadPoolSize);
            
            // Accept connections in a separate thread
            new Thread(this::acceptConnections).start();
            
        } catch (IOException e) {
            logger.error("Error starting server", e);
            running = false;
        }
    }
    
    /**
     * Accept incoming client connections
     */
    private void acceptConnections() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                Socket clientSocket = serverSocket.accept();
                logger.debug("New client connection from {}", clientSocket.getInetAddress());
                
                // Submit client handler to thread pool
                threadPool.submit(new ClientHandler(clientSocket, nlpService, dbService));
                
            } catch (IOException e) {
                if (running) {
                    logger.error("Error accepting client connection", e);
                }
            }
        }
    }
    
    /**
     * Stop the server and cleanup resources
     */
    public void stop() {
        running = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.error("Error closing server socket", e);
        }
        
        if (threadPool != null) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        logger.info("Query Server stopped");
    }
    
    public boolean isRunning() {
        return running;
    }
}
