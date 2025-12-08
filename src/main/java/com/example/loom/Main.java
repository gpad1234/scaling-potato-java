package com.example.loom;

import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point demonstrating Project Loom features with virtual threads.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Starting Project Loom demonstration application");
        logger.info("Java Version: {}", System.getProperty("java.version"));

        try {
            // Run virtual thread examples
            VirtualThreadExamples.demonstrateVirtualThreads();

            // Run thread pool examples
            ThreadPoolExamples.demonstrateThreadPooling();

        } catch (Exception e) {
            logger.error("Error during execution", e);
        }

        logger.info("Application completed successfully");
    }
}
