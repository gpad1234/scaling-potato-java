package com.example.loom;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Examples demonstrating structured patterns with virtual threads.
 */
public class StructuredConcurrencyExamples {
    private static final Logger logger = LoggerFactory.getLogger(StructuredConcurrencyExamples.class);

    public static void demonstrateStructuredConcurrency() throws Exception {
        logger.info("=== Structured Virtual Thread Patterns ===");

        // Example 1: Parallel execution with managed threads
        demonstrateParallelExecution();

        // Example 2: Pipeline pattern
        demonstratePipelinePattern();
    }

    private static void demonstrateParallelExecution() throws Exception {
        logger.info("Example 1: Parallel execution with virtual thread executor");

        List<Integer> results = new ArrayList<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Submit multiple tasks
            List<java.util.concurrent.Future<Integer>> futures = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                final int taskNum = i;
                java.util.concurrent.Future<Integer> future = executor.submit(() -> {
                    logger.info("Task {} executing", taskNum);
                    Thread.sleep(100 * taskNum);
                    return taskNum * 10;
                });
                futures.add(future);
            }

            // Collect results
            for (java.util.concurrent.Future<Integer> future : futures) {
                results.add(future.get());
            }
        }

        logger.info("Results: {}", results);
    }

    private static void demonstratePipelinePattern() throws Exception {
        logger.info("Example 2: Pipeline pattern with virtual threads");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Stage 1: Process data
            java.util.concurrent.Future<String> stage1 = executor.submit(() -> {
                logger.info("Stage 1: Processing data");
                Thread.sleep(100);
                return "data-processed";
            });

            // Stage 2: Transform result
            java.util.concurrent.Future<String> stage2 = executor.submit(() -> {
                String result = stage1.get();
                logger.info("Stage 2: Transforming {}", result);
                Thread.sleep(100);
                return result + "-transformed";
            });

            // Stage 3: Validate result
            java.util.concurrent.Future<String> stage3 = executor.submit(() -> {
                String result = stage2.get();
                logger.info("Stage 3: Validating {}", result);
                Thread.sleep(100);
                return result + "-validated";
            });

            String finalResult = stage3.get();
            logger.info("Final result: {}", finalResult);
        }
    }
}
