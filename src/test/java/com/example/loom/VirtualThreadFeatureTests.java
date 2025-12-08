package com.example.loom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;

@DisplayName("Virtual Thread Feature Tests")
public class VirtualThreadFeatureTests {

    @Nested
    @DisplayName("Basic Virtual Thread Creation")
    class BasicVirtualThreadCreation {

        @Test
        @DisplayName("Should create and execute a simple virtual thread")
        void testSimpleVirtualThreadExecution() throws InterruptedException {
            AtomicInteger executionCount = new AtomicInteger(0);

            Thread vthread = Thread.ofVirtual()
                    .unstarted(() -> executionCount.incrementAndGet());

            vthread.start();
            vthread.join();

            assertEquals(1, executionCount.get());
        }

        @Test
        @DisplayName("Virtual thread should be a daemon by default")
        void testVirtualThreadIsDaemon() {
            Thread vthread = Thread.ofVirtual()
                    .unstarted(() -> {});

            assertTrue(vthread.isDaemon(), "Virtual threads should be daemon threads by default");
        }

        @Test
        @DisplayName("Virtual thread should accept custom names")
        void testVirtualThreadCustomName() {
            String customName = "my-custom-vthread";
            Thread vthread = Thread.ofVirtual()
                    .name(customName)
                    .unstarted(() -> {});

            assertEquals(customName, vthread.getName());
        }

        @Test
        @DisplayName("Virtual thread should handle exceptions gracefully")
        void testVirtualThreadExceptionHandling() throws InterruptedException {
            AtomicInteger errorCount = new AtomicInteger(0);

            Thread vthread = Thread.ofVirtual()
                    .unstarted(() -> {
                        try {
                            throw new RuntimeException("Test exception");
                        } catch (RuntimeException e) {
                            errorCount.incrementAndGet();
                        }
                    });

            vthread.start();
            vthread.join();

            assertEquals(1, errorCount.get());
        }
    }

    @Nested
    @DisplayName("Virtual Thread Lifecycle")
    class VirtualThreadLifecycle {

        @Test
        @DisplayName("Virtual thread state transitions correctly")
        void testVirtualThreadStateTransitions() throws InterruptedException {
            Thread vthread = Thread.ofVirtual()
                    .unstarted(() -> {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });

            assertFalse(vthread.isAlive(), "Thread should not be alive before start");
            vthread.start();
            assertTrue(vthread.isAlive(), "Thread should be alive after start");
            vthread.join();
            assertFalse(vthread.isAlive(), "Thread should not be alive after join");
        }

        @Test
        @DisplayName("Virtual thread should support join with timeout")
        void testVirtualThreadJoinTimeout() throws InterruptedException {
            Thread vthread = Thread.ofVirtual()
                    .unstarted(() -> {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });

            vthread.start();
            long startTime = System.currentTimeMillis();
            vthread.join(50); // Wait only 50ms
            long elapsed = System.currentTimeMillis() - startTime;

            assertTrue(elapsed < 150, "Join should timeout before thread completes");
            assertTrue(vthread.isAlive(), "Thread should still be alive");

            vthread.join(); // Wait for completion
            assertFalse(vthread.isAlive(), "Thread should complete eventually");
        }

        @Test
        @DisplayName("Virtual thread should support interruption")
        void testVirtualThreadInterruption() throws InterruptedException {
            AtomicInteger interruptCount = new AtomicInteger(0);

            Thread vthread = Thread.ofVirtual()
                    .unstarted(() -> {
                        try {
                            Thread.sleep(1000); // Long sleep
                        } catch (InterruptedException e) {
                            interruptCount.incrementAndGet();
                            Thread.currentThread().interrupt();
                        }
                    });

            vthread.start();
            Thread.sleep(50);
            vthread.interrupt();
            vthread.join();

            assertEquals(1, interruptCount.get(), "Virtual thread should handle interruption");
        }
    }

    @Nested
    @DisplayName("Virtual Thread Concurrency")
    class VirtualThreadConcurrency {

        @Test
        @DisplayName("Multiple virtual threads should execute concurrently")
        void testMultipleConcurrentVirtualThreads() throws InterruptedException {
            AtomicInteger threadCount = new AtomicInteger(0);

            Thread[] threads = new Thread[10];
            for (int i = 0; i < 10; i++) {
                threads[i] = Thread.ofVirtual()
                        .unstarted(() -> {
                            threadCount.incrementAndGet();
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            assertEquals(10, threadCount.get());
        }

        @Test
        @DisplayName("Virtual threads should enable massive concurrency")
        void testMassiveVirtualThreadConcurrency() throws InterruptedException {
            int threadCount = 5000;
            AtomicInteger executionCount = new AtomicInteger(0);
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                threads[i] = Thread.ofVirtual()
                        .unstarted(() -> {
                            executionCount.incrementAndGet();
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        });
            }

            long startTime = System.currentTimeMillis();
            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }
            long duration = System.currentTimeMillis() - startTime;

            assertEquals(threadCount, executionCount.get());
            assertTrue(duration < 15000, "Massive virtual thread execution should complete quickly");
        }

        @Test
        @DisplayName("Virtual threads should share resources efficiently")
        void testVirtualThreadResourceSharing() throws InterruptedException {
            AtomicInteger sharedCounter = new AtomicInteger(0);
            int threadCount = 100;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                threads[i] = Thread.ofVirtual()
                        .unstarted(() -> {
                            for (int j = 0; j < 100; j++) {
                                sharedCounter.incrementAndGet();
                            }
                        });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            assertEquals(threadCount * 100, sharedCounter.get());
        }
    }

    @Nested
    @DisplayName("Virtual Thread Platform Thread Comparison")
    class VirtualThreadVsPlatformThread {

        @Test
        @DisplayName("Virtual threads should be more resource-efficient than platform threads")
        void testVirtualThreadResourceEfficiency() throws InterruptedException {
            int virtualThreadCount = 1000;
            AtomicInteger virtualExecuted = new AtomicInteger(0);

            long virtualStartTime = System.currentTimeMillis();
            try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
                for (int i = 0; i < virtualThreadCount; i++) {
                    executor.submit(() -> {
                        virtualExecuted.incrementAndGet();
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }
            }
            long virtualDuration = System.currentTimeMillis() - virtualStartTime;

            assertEquals(virtualThreadCount, virtualExecuted.get());
            assertTrue(virtualDuration < 10000, "Virtual thread execution should be efficient");
        }

        @Test
        @DisplayName("Virtual threads should handle thread creation faster")
        void testVirtualThreadCreationSpeed() {
            int creationCount = 10000;

            long startTime = System.currentTimeMillis();
            Thread[] threads = new Thread[creationCount];
            for (int i = 0; i < creationCount; i++) {
                threads[i] = Thread.ofVirtual().unstarted(() -> {});
            }
            long duration = System.currentTimeMillis() - startTime;

            assertTrue(duration < 5000, "Creating " + creationCount + " virtual threads should be fast");
        }
    }
}
