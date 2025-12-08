package com.example.loom;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Examples demonstrating thread pooling with virtual threads.
 */
public class ThreadPoolExamples {
    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolExamples.class);

    public static void demonstrateThreadPooling() throws InterruptedException {
        logger.info("=== Thread Pooling Examples ===");

        // Example 1: Virtual thread per task executor
        demonstrateVirtualThreadPerTask();

        // Example 2: Comparing performance
        comparePerformance();
    }

    private static void demonstrateVirtualThreadPerTask() throws InterruptedException {
        logger.info("Example 1: Virtual thread per task executor");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                final int taskId = i;
                tasks.add(() -> {
                    try {
                        long startTime = System.currentTimeMillis();
                        simulateIOOperation(taskId);
                        long duration = System.currentTimeMillis() - startTime;
                        logger.info("Task {} completed in {} ms", taskId, duration);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            for (Runnable task : tasks) {
                executor.submit(task);
            }
        }
        logger.info("All tasks in virtual thread pool completed");
    }

    private static void comparePerformance() throws InterruptedException {
        logger.info("Example 2: Comparing platform threads vs virtual threads");

        final int taskCount = 1000;
        final long sleepTime = 10;

        // Simulate with virtual threads
        long virtualStart = System.currentTimeMillis();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < taskCount; i++) {
                executor.submit(() -> {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }
        long virtualDuration = System.currentTimeMillis() - virtualStart;
        logger.info("Virtual threads: {} tasks completed in {} ms", taskCount, virtualDuration);

        // Simulate with fixed thread pool (limited threads)
        long platformStart = System.currentTimeMillis();
        try (var executor = Executors.newFixedThreadPool(10)) {
            for (int i = 0; i < taskCount; i++) {
                executor.submit(() -> {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }
        long platformDuration = System.currentTimeMillis() - platformStart;
        logger.info("Platform threads (pool of 10): {} tasks completed in {} ms", taskCount, platformDuration);

        logger.info("Virtual threads are approximately {} times faster", platformDuration / Math.max(1, virtualDuration));
    }

    private static void simulateIOOperation(int taskId) throws InterruptedException {
        Thread.sleep(5 + (taskId % 10));
    }
}
