package com.example.loom;

import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Examples demonstrating virtual thread features from Project Loom.
 */
public class VirtualThreadExamples {
    private static final Logger logger = LoggerFactory.getLogger(VirtualThreadExamples.class);

    public static void demonstrateVirtualThreads() throws InterruptedException {
        logger.info("=== Virtual Thread Examples ===");

        // Example 1: Creating virtual threads directly
        demonstrateBasicVirtualThreads();

        // Example 2: Virtual thread executor service
        demonstrateVirtualThreadExecutor();

        // Example 3: Massive virtual thread creation
        demonstrateMassiveThreadCreation();
    }

    private static void demonstrateBasicVirtualThreads() throws InterruptedException {
        logger.info("Example 1: Creating basic virtual threads");

        // Create a virtual thread
        Thread vthread = Thread.ofVirtual()
                .name("vthread-", 0)
                .unstarted(() -> {
                    logger.info("Hello from virtual thread: {}", Thread.currentThread().getName());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });

        vthread.start();
        vthread.join();
        logger.info("Virtual thread completed");
    }

    private static void demonstrateVirtualThreadExecutor() throws InterruptedException {
        logger.info("Example 2: Using virtual thread executor service");

        // Create an executor service with virtual threads
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 5; i++) {
                final int taskNum = i;
                executor.submit(() -> {
                    logger.info("Task {} running on {}", taskNum, Thread.currentThread().getName());
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }
        logger.info("All virtual thread tasks completed");
    }

    private static void demonstrateMassiveThreadCreation() throws InterruptedException {
        logger.info("Example 3: Creating massive number of virtual threads");

        int threadCount = 10000;
        long startTime = System.currentTimeMillis();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < threadCount; i++) {
                final int taskNum = i;
                executor.submit(() -> {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Created and executed {} virtual threads in {} ms", threadCount, duration);
    }
}
