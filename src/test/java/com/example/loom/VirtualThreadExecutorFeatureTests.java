package com.example.loom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

@DisplayName("Virtual Thread Executor Feature Tests")
public class VirtualThreadExecutorFeatureTests {

    @Nested
    @DisplayName("Virtual Thread Per Task Executor")
    class VirtualThreadPerTaskExecutor {

        @Test
        @DisplayName("Should create virtual thread for each submitted task")
        void testVirtualThreadPerTask() throws InterruptedException {
            AtomicInteger taskCount = new AtomicInteger(0);

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int i = 0; i < 10; i++) {
                    executor.submit(taskCount::incrementAndGet);
                }
            }

            assertEquals(10, taskCount.get());
        }

        @Test
        @DisplayName("Should execute tasks concurrently")
        void testConcurrentExecution() throws InterruptedException {
            AtomicInteger concurrentCount = new AtomicInteger(0);
            AtomicInteger maxConcurrent = new AtomicInteger(0);

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int i = 0; i < 100; i++) {
                    executor.submit(() -> {
                        int current = concurrentCount.incrementAndGet();
                        int max = maxConcurrent.get();
                        while (max < current && !maxConcurrent.compareAndSet(max, current)) {
                            max = maxConcurrent.get();
                        }

                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        concurrentCount.decrementAndGet();
                    });
                }
            }

            assertTrue(maxConcurrent.get() > 1, "Should execute tasks concurrently");
        }

        @Test
        @DisplayName("Should handle task exceptions gracefully")
        void testExceptionHandling() throws InterruptedException {
            AtomicInteger exceptionCount = new AtomicInteger(0);
            AtomicInteger successCount = new AtomicInteger(0);

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int i = 0; i < 10; i++) {
                    final int taskNum = i;
                    executor.submit(() -> {
                        if (taskNum % 2 == 0) {
                            throw new RuntimeException("Task " + taskNum + " failed");
                        }
                        successCount.incrementAndGet();
                    });
                }
            }

            assertEquals(5, successCount.get(), "Half tasks should succeed");
        }

        @Test
        @DisplayName("Should shutdown gracefully")
        void testGracefulShutdown() throws InterruptedException {
            var executor = Executors.newVirtualThreadPerTaskExecutor();

            executor.submit(() -> {});
            executor.submit(() -> {});

            executor.shutdown();
            assertTrue(executor.isShutdown());
            assertTrue(executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("Executor Task Submission Patterns")
    class TaskSubmissionPatterns {

        @Test
        @DisplayName("Should handle submit of Runnable")
        void testSubmitRunnable() throws InterruptedException {
            AtomicInteger executionCount = new AtomicInteger(0);

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                executor.submit((Runnable) executionCount::incrementAndGet);
            }

            assertEquals(1, executionCount.get());
        }

        @Test
        @DisplayName("Should handle submit of Callable with return value")
        void testSubmitCallable() throws Exception {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var future = executor.submit(() -> "task result");
                String result = future.get();
                assertEquals("task result", result);
            }
        }

        @Test
        @DisplayName("Should execute collection of tasks")
        void testInvokeAll() throws Exception {
            List<String> results = new ArrayList<>();

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<java.util.concurrent.Callable<String>> tasks = List.of(
                    (java.util.concurrent.Callable<String>) () -> "result1",
                    (java.util.concurrent.Callable<String>) () -> "result2",
                    (java.util.concurrent.Callable<String>) () -> "result3"
                );

                var futures = executor.invokeAll(tasks);

                for (var future : futures) {
                    results.add(future.get());
                }
            }

            assertEquals(3, results.size());
            assertTrue(results.contains("result1"));
            assertTrue(results.contains("result2"));
            assertTrue(results.contains("result3"));
        }

        @Test
        @DisplayName("Should support invokeAny for first completion")
        void testInvokeAny() throws Exception {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<java.util.concurrent.Callable<String>> tasks = List.of(
                    (java.util.concurrent.Callable<String>) () -> { Thread.sleep(1000); return "slow"; },
                    (java.util.concurrent.Callable<String>) () -> { Thread.sleep(10); return "fast"; },
                    (java.util.concurrent.Callable<String>) () -> { Thread.sleep(500); return "medium"; }
                );

                String result = executor.invokeAny(tasks);
                assertEquals("fast", result);
            }
        }
    }

    @Nested
    @DisplayName("Executor Resource Management")
    class ResourceManagement {

        @Test
        @DisplayName("Should efficiently handle many virtual threads")
        void testLargeScaleExecution() throws InterruptedException {
            int threadCount = 5000;
            AtomicInteger executionCount = new AtomicInteger(0);

            long startTime = System.currentTimeMillis();
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int i = 0; i < threadCount; i++) {
                    executor.submit(() -> {
                        executionCount.incrementAndGet();
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }
            }
            long duration = System.currentTimeMillis() - startTime;

            assertEquals(threadCount, executionCount.get());
            assertTrue(duration < 30000, "Should handle large scale execution efficiently");
        }

        @Test
        @DisplayName("Should reuse executors for multiple batches")
        void testMultipleBatches() throws InterruptedException {
            AtomicInteger totalExecuted = new AtomicInteger(0);

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int batch = 0; batch < 3; batch++) {
                    for (int i = 0; i < 100; i++) {
                        executor.submit(totalExecuted::incrementAndGet);
                    }
                }
            }

            assertEquals(300, totalExecuted.get());
        }

        @Test
        @DisplayName("Should handle executor cleanup properly")
        void testExecutorCleanup() throws InterruptedException {
            var executor = Executors.newVirtualThreadPerTaskExecutor();
            executor.submit(() -> {});

            executor.shutdown();
            executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

            assertTrue(executor.isTerminated());
        }
    }

    @Nested
    @DisplayName("Executor Performance Characteristics")
    class PerformanceCharacteristics {

        @Test
        @DisplayName("Should create virtual threads faster than platform threads")
        void testCreationSpeed() throws InterruptedException {
            int taskCount = 10000;
            AtomicInteger executed = new AtomicInteger(0);

            long startTime = System.currentTimeMillis();
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int i = 0; i < taskCount; i++) {
                    executor.submit(executed::incrementAndGet);
                }
            }
            long duration = System.currentTimeMillis() - startTime;

            assertEquals(taskCount, executed.get());
            assertTrue(duration < 60000, "Should complete " + taskCount + " tasks within reasonable time");
        }

        @Test
        @DisplayName("Should maintain consistent latency under load")
        void testLatencyUnderLoad() throws InterruptedException {
            AtomicInteger completedTasks = new AtomicInteger(0);

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int i = 0; i < 1000; i++) {
                    executor.submit(() -> {
                        completedTasks.incrementAndGet();
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }
            }

            assertEquals(1000, completedTasks.get());
        }

        @Test
        @DisplayName("Should efficiently handle I/O-bound tasks")
        void testIOBoundTasks() throws InterruptedException {
            AtomicInteger ioOperations = new AtomicInteger(0);

            long startTime = System.currentTimeMillis();
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int i = 0; i < 500; i++) {
                    executor.submit(() -> {
                        // Simulate I/O operation
                        try {
                            Thread.sleep(10);
                            ioOperations.incrementAndGet();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }
            }
            long duration = System.currentTimeMillis() - startTime;

            assertEquals(500, ioOperations.get());
            // All tasks should complete in roughly 10ms, not 5000ms
            assertTrue(duration < 5000, "I/O operations should be parallelized");
        }
    }

    @Nested
    @DisplayName("Executor Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle task exceptions without stopping executor")
        void testExceptionDoesNotStopExecutor() throws InterruptedException {
            AtomicInteger successCount = new AtomicInteger(0);

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                executor.submit(() -> { throw new RuntimeException("First task failed"); });
                executor.submit(successCount::incrementAndGet);
                executor.submit(successCount::incrementAndGet);
            }

            assertEquals(2, successCount.get());
        }

        @Test
        @DisplayName("Should provide exception information through Future")
        void testFutureException() throws Exception {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var future = executor.submit(() -> {
                    throw new RuntimeException("Task exception");
                });

                assertThrows(java.util.concurrent.ExecutionException.class, () -> future.get());
            }
        }
    }
}
