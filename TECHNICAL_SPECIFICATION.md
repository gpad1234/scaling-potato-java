# Project Loom Technical Specification

## Document Overview

This technical specification provides detailed documentation of Project Loom features and their implementation in the concurrent-java-loom application. Project Loom introduces virtual threads and structured concurrency to Java, enabling more scalable and maintainable concurrent applications.

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Feature 1: Virtual Threads](#feature-1-virtual-threads)
3. [Feature 2: Structured Concurrency](#feature-2-structured-concurrency)
4. [Feature 3: Virtual Thread Executors](#feature-3-virtual-thread-executors)
5. [Architecture and Design](#architecture-and-design)
6. [Performance Characteristics](#performance-characteristics)
7. [Best Practices](#best-practices)
8. [API Reference](#api-reference)

---

## Executive Summary

Project Loom (JEP 444) is a major Java enhancement that fundamentally changes how concurrent applications are written. It introduces:

- **Virtual Threads**: Lightweight threads managed by the JVM (millions can coexist)
- **Structured Concurrency**: A programming model for managing groups of related concurrent tasks
- **Improved Scalability**: Better resource utilization and simpler application code

This specification covers three core features implemented in the concurrent-java-loom project.

---

## Feature 1: Virtual Threads

### Overview

Virtual threads are lightweight threads managed by the Java Virtual Machine. Unlike platform threads (which are 1:1 mapped to OS threads), virtual threads are multiplexed onto a small pool of OS threads called "carrier threads."

### Key Characteristics

| Characteristic | Platform Thread | Virtual Thread |
|---|---|---|
| Cost | ~1-2 MB memory | ~1 KB memory |
| Creation | Expensive | Cheap |
| Scalability | Thousands | Millions |
| Blocking I/O | Blocks OS thread | Pauses virtual thread |
| Context switching | Kernel-level | JVM-level |

### Usage Patterns

#### Basic Virtual Thread Creation

```java
// Create a virtual thread
Thread vthread = Thread.ofVirtual()
    .name("my-vthread")
    .unstarted(() -> {
        System.out.println("Hello from virtual thread");
    });

vthread.start();
vthread.join();
```

**Key Methods:**
- `Thread.ofVirtual()`: Builder for creating virtual threads
- `.name(String)`: Set thread name
- `.unstarted(Runnable)`: Create thread without starting
- `.start()`: Start thread execution
- `.join()`: Wait for completion

#### Virtual Thread Properties

```java
// Check if thread is virtual
boolean isVirtual = thread.isVirtual();

// Virtual threads are daemon threads by default
boolean isDaemon = thread.isDaemon();

// Get thread name
String name = thread.getName();
```

### Features

#### 1. Lightweight Thread Creation

Virtual threads are designed for massive concurrency:

```java
// Create 100,000 virtual threads easily
for (int i = 0; i < 100_000; i++) {
    Thread vthread = Thread.ofVirtual()
        .unstarted(() -> performTask())
        .start();
}
```

**Memory Usage:**
- Platform thread: ~1-2 MB per thread
- Virtual thread: ~1 KB per thread
- Savings: 1000x reduction for 100,000 threads

#### 2. Transparent I/O Handling

When a virtual thread performs blocking I/O:

```java
Thread vthread = Thread.ofVirtual().unstarted(() -> {
    // Virtual thread pauses, carrier thread serves other virtual threads
    InputStream data = socket.getInputStream(); // Blocks virtual thread
    byte[] buffer = new byte[1024];
    data.read(buffer); // Paused, not blocking OS thread
});
```

**Behavior:**
- Virtual thread suspends automatically
- Carrier thread becomes available for other work
- No explicit async/await needed

#### 3. Exception Handling

Virtual threads support standard Java exception handling:

```java
Thread vthread = Thread.ofVirtual().unstarted(() -> {
    try {
        riskyOperation();
    } catch (IOException e) {
        logger.error("Operation failed", e);
    }
});
```

#### 4. Interruption Support

Virtual threads can be interrupted like platform threads:

```java
Thread vthread = Thread.ofVirtual().unstarted(() -> {
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        logger.info("Thread interrupted");
        Thread.currentThread().interrupt();
    }
});

vthread.start();
// ... later
vthread.interrupt();
vthread.join();
```

### Test Coverage

**VirtualThreadFeatureTests.java** covers:
- Basic thread creation and execution
- Lifecycle management (start, join, interrupt)
- Concurrent execution of multiple threads
- Massive concurrency (5000+ threads)
- Resource sharing and atomic operations
- Comparison with platform threads

### Use Cases

1. **Web Servers**: Handle millions of concurrent connections
2. **API Gateways**: Manage high-volume request processing
3. **Message Processing**: Process thousands of messages concurrently
4. **Batch Processing**: Efficient parallel task execution
5. **I/O-Heavy Applications**: Network requests, database operations

---

## Feature 2: Structured Concurrency

### Overview

Structured concurrency is a programming model that treats a group of concurrent tasks as a single unit with a clear lifecycle. It enforces a hierarchical task structure and ensures proper cleanup.

### Core Concepts

#### Task Scope

A scope encapsulates a group of related concurrent tasks:

```java
try (var scope = new StructuredTaskScope<String>()) {
    // Fork subtasks
    Subtask<String> task1 = scope.fork(() -> "result1");
    Subtask<String> task2 = scope.fork(() -> "result2");
    
    // Join - wait for all tasks to complete
    scope.join();
    
    // Access results
    String result1 = task1.get();
    String result2 = task2.get();
} // Scope auto-closes, cancelling any remaining tasks
```

#### Subtask Lifecycle

```
fork() ──> running ──> succeeded ──> completed
                    \─> failed ──> completed
                    \─> cancelled ──> completed
```

### Built-in Scope Types

#### 1. Basic StructuredTaskScope

Collects results from multiple tasks:

```java
try (var scope = new StructuredTaskScope<Integer>()) {
    Subtask<Integer> sum1 = scope.fork(() -> calculateSum(1, 1000));
    Subtask<Integer> sum2 = scope.fork(() -> calculateSum(1001, 2000));
    
    scope.join();
    
    int total = sum1.get() + sum2.get();
}
```

**Characteristics:**
- No specific error handling
- All tasks must complete
- Results available after join

#### 2. ShutdownOnFailure

Cancels remaining tasks if any task fails:

```java
try (var scope = new ShutdownOnFailure()) {
    scope.fork(() -> successfulTask());
    scope.fork(() -> failingTask()); // Throws exception
    
    scope.join().throwIfFailed();
    // If any task failed, exception is thrown
}
```

**Use Cases:**
- Validations where all must succeed
- Critical operations where failure cascades

#### 3. ShutdownOnSuccess

Cancels remaining tasks when first task succeeds:

```java
try (var scope = new ShutdownOnSuccess<String>()) {
    scope.fork(() -> { Thread.sleep(1000); return "slow"; });
    scope.fork(() -> { Thread.sleep(10); return "fast"; });
    
    var outcome = scope.join();
    String fastest = outcome.result(); // "fast"
}
```

**Use Cases:**
- Racing multiple strategies
- First available result
- Failover scenarios

### Features

#### 1. Clear Scope Boundaries

```java
try (var scope = new StructuredTaskScope<String>()) {
    // Tasks created within this scope
    Subtask<String> task1 = scope.fork(() -> work());
    
    scope.join(); // All tasks must complete before proceeding
} // Scope guarantees cleanup - no task leaks

// Cannot fork new tasks outside scope
scope.fork(() -> work()); // Throws IllegalStateException
```

**Benefits:**
- No orphaned tasks
- Guaranteed resource cleanup
- Clear task ownership

#### 2. Exception Propagation

```java
try (var scope = new ShutdownOnFailure()) {
    scope.fork(() -> {
        throw new DatabaseException("Query failed");
    });
    
    scope.join().throwIfFailed();
} catch (DatabaseException e) {
    // Exception properly propagated
    logger.error("Database operation failed", e);
}
```

#### 3. Task Result Aggregation

```java
try (var scope = new StructuredTaskScope<Integer>()) {
    List<Subtask<Integer>> tasks = new ArrayList<>();
    
    for (int i = 0; i < 10; i++) {
        Subtask<Integer> task = scope.fork(() -> calculateValue());
        tasks.add(task);
    }
    
    scope.join();
    
    // Aggregate results
    int sum = tasks.stream()
        .mapToInt(t -> t.get())
        .sum();
}
```

#### 4. Nested Scopes

```java
try (var outerScope = new StructuredTaskScope<Integer>()) {
    Subtask<Integer> result = outerScope.fork(() -> {
        try (var innerScope = new StructuredTaskScope<Integer>()) {
            Subtask<Integer> inner1 = innerScope.fork(() -> 10);
            Subtask<Integer> inner2 = innerScope.fork(() -> 20);
            
            innerScope.join();
            
            return inner1.get() + inner2.get();
        }
    });
    
    outerScope.join();
    // Returns 30
}
```

**Hierarchical Structure:**
```
Outer Scope
├── Task 1
│   ├── Inner Scope
│   │   ├── Inner Task 1
│   │   └── Inner Task 2
└── Task 2
```

### Test Coverage

**StructuredConcurrencyFeatureTests.java** covers:
- Basic fork and join operations
- Multiple subtasks in parallel
- Task lifecycle management
- Error handling with ShutdownOnFailure
- Fast-fail with ShutdownOnSuccess
- Scope boundary enforcement
- Nested scope structures
- Result aggregation patterns
- Performance characteristics

### Use Cases

1. **Parallel Computations**: Split work across multiple cores
2. **Fault Tolerance**: Handle failures gracefully
3. **Microservices**: Parallel API calls with timeout
4. **Data Processing**: Parallel processing of data chunks
5. **Validation**: Ensure multiple conditions are met

### Comparison: Before and After

**Before (Callbacks/Futures):**
```java
CompletableFuture<Integer> f1 = CompletableFuture.supplyAsync(() -> compute1());
CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(() -> compute2());

CompletableFuture<Integer> combined = f1.thenCombine(f2, (r1, r2) -> r1 + r2);

try {
    Integer result = combined.get(5, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    combined.cancel(true);
}
```

**After (Structured Concurrency):**
```java
try (var scope = new StructuredTaskScope<Integer>()) {
    Subtask<Integer> t1 = scope.fork(() -> compute1());
    Subtask<Integer> t2 = scope.fork(() -> compute2());
    
    scope.join();
    
    Integer result = t1.get() + t2.get();
}
```

**Benefits:**
- Cleaner syntax
- Automatic cancellation
- Clear resource ownership
- Better error handling

---

## Feature 3: Virtual Thread Executors

### Overview

Virtual thread executors (`newVirtualThreadPerTaskExecutor()`) simplify working with large numbers of virtual threads by managing thread creation and lifecycle.

### Basic Usage

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10_000; i++) {
        executor.submit(() -> {
            // Each task runs in its own virtual thread
            performIOOperation();
        });
    }
    // All tasks complete before executor closes
}
```

### Key Features

#### 1. One Thread Per Task

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    // Each submission creates a new virtual thread
    executor.submit(() -> operation1());
    executor.submit(() -> operation2());
    executor.submit(() -> operation3());
    // Three virtual threads, not limited by pool size
}
```

**Comparison:**

| Executor Type | Threads | When to Use |
|---|---|---|
| `newFixedThreadPool(n)` | Limited to n | CPU-bound, few long-lived tasks |
| `newCachedThreadPool()` | Unlimited (platform threads) | Variable load, but costly |
| `newVirtualThreadPerTaskExecutor()` | Unlimited (virtual threads) | High concurrency, I/O-bound |

#### 2. Task Submission Methods

```java
// Submit Runnable
executor.submit(() -> System.out.println("task"));

// Submit Callable with return value
Future<String> future = executor.submit(() -> "result");
String result = future.get();

// Submit collection of tasks
List<Callable<String>> tasks = Arrays.asList(
    () -> "result1",
    () -> "result2",
    () -> "result3"
);
List<Future<String>> futures = executor.invokeAll(tasks);

// Execute any task (returns first result)
String fastest = executor.invokeAny(tasks);
```

#### 3. Exception Handling

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    Future<String> future = executor.submit(() -> {
        throw new RuntimeException("Task failed");
    });
    
    try {
        String result = future.get();
    } catch (ExecutionException e) {
        // Cause contains the actual exception
        Throwable cause = e.getCause();
    }
}
```

#### 4. Graceful Shutdown

```java
var executor = Executors.newVirtualThreadPerTaskExecutor();

// Submit tasks
executor.submit(() -> operation1());
executor.submit(() -> operation2());

// Stop accepting new tasks
executor.shutdown();

// Wait for completion
boolean terminated = executor.awaitTermination(10, TimeUnit.SECONDS);

if (!terminated) {
    // Force shutdown if not complete
    List<Runnable> remaining = executor.shutdownNow();
    logger.warn("Force shut down with {} remaining tasks", remaining.size());
}
```

### Test Coverage

**VirtualThreadExecutorFeatureTests.java** covers:
- Virtual thread creation per task
- Concurrent task execution
- Exception handling
- Graceful shutdown
- Runnable and Callable submission
- invokeAll and invokeAny patterns
- Large-scale execution (5000+ threads)
- I/O-bound task performance
- Batch processing
- Error handling without stopping executor

### Design Patterns

#### Pattern 1: Fire and Forget

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (Event event : events) {
        executor.submit(() -> processEvent(event));
    }
}
```

#### Pattern 2: Collect Results

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<Result>> futures = new ArrayList<>();
    
    for (Item item : items) {
        Future<Result> future = executor.submit(() -> process(item));
        futures.add(future);
    }
    
    for (Future<Result> future : futures) {
        Result result = future.get();
        handleResult(result);
    }
}
```

#### Pattern 3: Stream Processing

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    dataStream
        .forEach(item -> executor.submit(() -> process(item)));
}
```

### Use Cases

1. **Web Servers**: Handle thousands of concurrent requests
2. **API Clients**: Parallel API calls
3. **Data Processing**: Process large datasets concurrently
4. **Event Processing**: Handle high-volume event streams
5. **Batch Operations**: Concurrent batch processing

---

## Architecture and Design

### System Architecture

```
┌─────────────────────────────────────────────────┐
│         Application Code                        │
│  (Main, VirtualThreadExamples, etc.)            │
└──────────┬──────────────────────────────────────┘
           │
           ├─────────────────┬──────────────────┐
           ▼                 ▼                  ▼
    ┌─────────────┐  ┌──────────────┐  ┌────────────────┐
    │  Virtual    │  │  Structured  │  │  Virtual Thread│
    │  Threads    │  │  Concurrency │  │  Executors     │
    └─────────────┘  └──────────────┘  └────────────────┘
           │                 │                  │
           └─────────────────┴──────────────────┘
                     │
           ┌─────────▼──────────┐
           │   Java 21 Runtime   │
           │  (Virtual Thread    │
           │   Scheduler)        │
           └─────────┬───────────┘
                     │
           ┌─────────▼──────────┐
           │  Carrier Threads    │
           │  (Platform Threads) │
           └─────────┬───────────┘
                     │
           ┌─────────▼──────────┐
           │   OS Thread Pool    │
           └────────────────────┘
```

### Threading Model

```
Virtual Thread 1 ─────┐
Virtual Thread 2 ──── Carrier Thread 1 ──> OS Thread 1
Virtual Thread 3 ─────┘

Virtual Thread 4 ─────┐
Virtual Thread 5 ──── Carrier Thread 2 ──> OS Thread 2
Virtual Thread 6 ─────┘

...continuing for all available CPU cores
```

### Execution Flow

#### Virtual Thread Execution

```
1. Thread.ofVirtual().start()
   ↓
2. JVM Scheduler assigns to Carrier Thread
   ↓
3. Virtual Thread executes
   ↓
4a. If CPU-bound: continues execution
   ↓
4b. If I/O blocking: Virtual Thread pauses
    Carrier Thread serves other Virtual Threads
   ↓
5. When I/O completes: Virtual Thread resumes
   ↓
6. Execution completes or blocks again
```

#### Structured Concurrency Execution

```
scope.fork(task1) ──┐
scope.fork(task2) ──┼─> Queue in executor
scope.fork(task3) ──┘
   ↓
scope.join() ──> Wait for all to complete
   ↓
task1.get(), task2.get(), task3.get() ──> Retrieve results
   ↓
scope.close() ──> Cancel remaining tasks (if any)
```

---

## Performance Characteristics

### Virtual Threads vs Platform Threads

| Metric | Virtual Threads | Platform Threads |
|--------|---|---|
| Memory per thread | ~1 KB | ~1-2 MB |
| Context switch cost | ~100 ns | ~1-10 µs |
| Thread creation time | ~1 µs | ~100 µs |
| Max concurrent threads | 1,000,000+ | 10,000 |
| Suitable for | I/O-bound | CPU-bound |

### Benchmarks (Typical Results)

#### Scenario 1: 10,000 Tasks with 10ms I/O

```
Platform Thread Pool (10 threads):
- Time: ~10,000ms (sequential)
- Memory: ~20MB

Virtual Threads:
- Time: ~10-50ms (concurrent)
- Memory: ~10MB
- Speedup: 100-200x
```

#### Scenario 2: 1,000 Concurrent Connections

```
Traditional Executor (limited pool):
- Connections queued: Many blocked
- Latency: High and variable
- Resource overhead: High

Virtual Threads:
- All connections served concurrently
- Latency: Consistent and low
- Resource overhead: Minimal
```

### Resource Utilization

```
CPU-Bound Task (100 threads):
┌─ Virtual Threads: ×1.0 CPU usage (efficient)
├─ Platform Threads (10 pool): ×0.9 CPU usage (context switching overhead)
└─ Platform Threads (100 threads): ×0.7 CPU usage (excessive context switching)

I/O-Bound Task (1000 threads, 10ms each):
┌─ Virtual Threads: ~10-50ms completion
├─ Platform Threads (10 pool): ~1000ms completion
└─ Platform Threads (100 pool): ~100ms completion
```

---

## Best Practices

### Virtual Threads

#### DO

✅ Use virtual threads for I/O-intensive operations:
```java
// Web service calls
Thread vthread = Thread.ofVirtual().unstarted(() -> {
    String data = httpClient.get(url);
    processData(data);
});
```

✅ Create large numbers when needed:
```java
for (int i = 0; i < 100_000; i++) {
    Thread.ofVirtual().unstarted(task).start();
}
```

✅ Use structured concurrency to manage groups:
```java
try (var scope = new StructuredTaskScope<Result>()) {
    for (int i = 0; i < 1000; i++) {
        scope.fork(() -> computeValue());
    }
    scope.join();
}
```

#### DON'T

❌ Don't use virtual threads for CPU-intensive work:
```java
// Bad: CPU-bound task wastes virtual thread benefits
Thread vthread = Thread.ofVirtual().unstarted(() -> {
    int sum = complexCalculation(); // Blocks carrier thread
});
```

❌ Don't hold locks for extended periods:
```java
// Bad: Blocks carrier thread for long time
Thread vthread = Thread.ofVirtual().unstarted(() -> {
    synchronized (resource) {
        Thread.sleep(1000); // Blocks carrier thread!
    }
});
```

❌ Don't assume traditional thread pool sizing:
```java
// Bad: Creates unnecessary threads
ExecutorService executor = Executors.newFixedThreadPool(10);

// Good: One thread per task
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
```

### Structured Concurrency

#### DO

✅ Use appropriate scope type:
```java
// For "all must succeed"
try (var scope = new ShutdownOnFailure()) {
    scope.fork(() -> validate1());
    scope.fork(() -> validate2());
    scope.join().throwIfFailed();
}

// For "first success wins"
try (var scope = new ShutdownOnSuccess<String>()) {
    scope.fork(() -> tryStrategyA());
    scope.fork(() -> tryStrategyB());
    var outcome = scope.join();
    String winner = outcome.result();
}
```

✅ Handle exceptions properly:
```java
try {
    try (var scope = new ShutdownOnFailure()) {
        scope.fork(() -> riskyOperation1());
        scope.fork(() -> riskyOperation2());
        scope.join().throwIfFailed();
    }
} catch (FailureException e) {
    // Handle failure appropriately
    logger.error("Operations failed", e);
}
```

✅ Use nested scopes for complex workflows:
```java
try (var mainScope = new StructuredTaskScope<Integer>()) {
    Subtask<Integer> result = mainScope.fork(() -> {
        try (var subScope = new StructuredTaskScope<Integer>()) {
            // Sub-operations
        }
        return computeValue();
    });
}
```

#### DON'T

❌ Don't create scope outside try-with-resources:
```java
// Bad: May leak if exception occurs
StructuredTaskScope<String> scope = new StructuredTaskScope<>();
```

❌ Don't ignore exceptions:
```java
// Bad: Silently ignores failures
try (var scope = new ShutdownOnFailure()) {
    scope.fork(() -> mightFail());
    try {
        scope.join().throwIfFailed();
    } catch (Exception e) {
        // Ignoring!
    }
}
```

❌ Don't mix sync and async operations carelessly:
```java
// Bad: Blocks virtual thread waiting for result
try (var scope = new StructuredTaskScope<String>()) {
    Subtask<String> task = scope.fork(() -> longRunningOperation());
    Thread.sleep(5000); // Blocks!
    scope.join();
}
```

### Virtual Thread Executors

#### DO

✅ Use try-with-resources for automatic shutdown:
```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> task1());
    executor.submit(() -> task2());
    // Automatically shut down
}
```

✅ Handle Future results:
```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    Future<String> future = executor.submit(() -> getData());
    String result = future.get();
    processResult(result);
}
```

✅ Monitor task completion:
```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<?>> futures = new ArrayList<>();
    
    for (Item item : items) {
        futures.add(executor.submit(() -> process(item)));
    }
    
    int completed = 0;
    for (Future<?> future : futures) {
        future.get();
        completed++;
    }
}
```

#### DON'T

❌ Don't forget to shut down executor:
```java
// Bad: May waste resources
var executor = Executors.newVirtualThreadPerTaskExecutor();
executor.submit(() -> task());
// No shutdown!
```

❌ Don't ignore Future exceptions:
```java
// Bad: Silently ignores exceptions
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> {
        throw new RuntimeException("Failed");
    });
    Thread.sleep(100); // Exception is ignored
}
```

❌ Don't reuse executor after shutdown:
```java
// Bad: Executor is closed
var executor = Executors.newVirtualThreadPerTaskExecutor();
executor.shutdown();
executor.submit(() -> task()); // Throws RejectedExecutionException
```

---

## API Reference

### Virtual Threads API

#### Thread.ofVirtual()

Creates a builder for virtual thread creation.

```java
Thread.Builder vb = Thread.ofVirtual()
    .name("vthread-", 0)      // Name pattern
    .uncaughtExceptionHandler(ueh);

Thread thread = vb.unstarted(() -> {
    System.out.println("Virtual thread task");
});

thread.start();
thread.join();
```

**Methods:**
- `name(String)` - Set thread name
- `name(String, long)` - Name pattern with counter
- `uncaughtExceptionHandler(UncaughtExceptionHandler)` - Exception handler
- `unstarted(Runnable)` - Create unstarted thread
- `start(Runnable)` - Create and start thread

#### Thread Methods

```java
// Query virtual status
boolean isVirtual = thread.isVirtual();

// Standard thread methods still work
thread.start();
thread.join();
thread.join(long millis);
thread.interrupt();
boolean interrupted = thread.isInterrupted();
String name = thread.getName();
```

### Structured Concurrency API

#### StructuredTaskScope<T>

```java
try (var scope = new StructuredTaskScope<String>()) {
    // Fork subtasks
    Subtask<String> task = scope.fork(() -> "result");
    
    // Wait for completion
    scope.join();
    
    // Get result
    String result = task.get();
} // Auto-close cancels remaining tasks
```

**Methods:**
- `fork(Callable<T>)` - Submit subtask
- `join()` - Wait for completion
- `joinUntil(Instant)` - Wait until deadline

#### ShutdownOnFailure

```java
try (var scope = new ShutdownOnFailure()) {
    scope.fork(() -> operation1());
    scope.fork(() -> operation2());
    
    scope.join().throwIfFailed();
} catch (FailureException e) {
    // Handle failure
}
```

#### ShutdownOnSuccess<T>

```java
try (var scope = new ShutdownOnSuccess<String>()) {
    scope.fork(() -> strategy1());
    scope.fork(() -> strategy2());
    
    var outcome = scope.join();
    T result = outcome.result();
}
```

### Virtual Thread Executor API

#### Executors.newVirtualThreadPerTaskExecutor()

```java
ExecutorService executor = 
    Executors.newVirtualThreadPerTaskExecutor();

// Submit work
executor.submit(() -> System.out.println("task"));
executor.submit(() -> "result");

// Shutdown
executor.shutdown();
executor.awaitTermination(10, TimeUnit.SECONDS);
```

**Methods:**
- `submit(Runnable)`
- `submit(Callable<T>)`
- `invokeAll(Collection<Callable<T>>)`
- `invokeAny(Collection<Callable<T>>)`
- `shutdown()`
- `shutdownNow()`
- `awaitTermination(long, TimeUnit)`

---

## Testing Strategy

### Test Organization

Tests are organized into nested test classes for clarity:

```
VirtualThreadFeatureTests
├── BasicVirtualThreadCreation
├── VirtualThreadLifecycle
├── VirtualThreadConcurrency
└── VirtualThreadVsPlatformThread

StructuredConcurrencyFeatureTests
├── BasicStructuredTaskScope
├── ErrorHandling
├── ConcurrencyScoping
├── ComputationPatterns
└── PerformanceCharacteristics

VirtualThreadExecutorFeatureTests
├── VirtualThreadPerTaskExecutor
├── TaskSubmissionPatterns
├── ResourceManagement
├── PerformanceCharacteristics
└── ErrorHandling
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=VirtualThreadFeatureTests

# Run specific test
mvn test -Dtest=VirtualThreadFeatureTests#testMassiveConcurrency

# Run with coverage
mvn test -Pcoverage
```

---

## Conclusion

Project Loom fundamentally improves Java's concurrent programming model by making it practical to write high-concurrency applications with simple, straightforward code. Virtual threads reduce resource overhead while structured concurrency ensures safe task management.

This project demonstrates all three core features with comprehensive test coverage and practical examples suitable for production use.

### Key Takeaways

1. **Virtual Threads** enable millions of concurrent tasks with minimal overhead
2. **Structured Concurrency** provides a safe, clear model for managing related tasks
3. **Virtual Thread Executors** simplify thread pool management for high-concurrency scenarios
4. Together, these features enable building highly scalable applications with maintainable code

### References

- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [JEP 453: Structured Concurrency (Preview)](https://openjdk.org/jeps/453)
- [Java 21 Release Notes](https://www.oracle.com/java/technologies/javase/21-relnotes.html)
- [Project Loom](https://wiki.openjdk.org/display/loom/)
