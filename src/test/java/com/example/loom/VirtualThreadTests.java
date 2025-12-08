package com.example.loom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Virtual Thread Tests")
public class VirtualThreadTests {

    @Test
    @DisplayName("Virtual thread should start and complete")
    public void testVirtualThreadExecution() throws InterruptedException {
        Thread vthread = Thread.ofVirtual()
                .unstarted(() -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });

        assertFalse(vthread.isAlive());
        vthread.start();
        assertTrue(vthread.isAlive());
        vthread.join();
        assertFalse(vthread.isAlive());
    }

    @Test
    @DisplayName("Virtual thread should have correct name")
    public void testVirtualThreadName() throws InterruptedException {
        Thread vthread = Thread.ofVirtual()
                .name("test-vthread")
                .unstarted(() -> {});

        assertEquals("test-vthread", vthread.getName());
    }

    @Test
    @DisplayName("Virtual threads should be daemon by default")
    public void testVirtualThreadDaemon() {
        Thread vthread = Thread.ofVirtual()
                .unstarted(() -> {});

        assertTrue(vthread.isDaemon());
    }
}
