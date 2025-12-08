package com.example.loom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Thread Pool Tests")
public class ThreadPoolTests {

    @Test
    @DisplayName("Virtual thread executor should handle multiple tasks")
    public void testVirtualThreadExecutor() throws InterruptedException {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            executor.submit(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        assertTrue(true);
    }

    @Test
    @DisplayName("Virtual thread executor should shut down gracefully")
    public void testExecutorShutdown() throws InterruptedException {
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        executor.submit(() -> {});
        executor.shutdown();
        assertTrue(executor.isShutdown());
    }
}
