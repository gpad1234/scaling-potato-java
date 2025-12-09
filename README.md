# Project Loom Concurrent Java Application

A comprehensive Maven-based Java project demonstrating Project Loom features including virtual threads and structured concurrency.


## Features

### Virtual Threads
- Create and manage virtual threads efficiently
- Execute massive numbers of concurrent tasks
- Use virtual thread executors for task execution

### Structured Concurrency
- Manage groups of related tasks with clear lifecycle
- Handle errors consistently across multiple tasks
- Ensure all subtasks complete or are cancelled

### Thread Pooling
- Virtual thread per task executor implementation
- Performance comparisons between platform and virtual threads
- Efficient resource utilization

## Requirements

- **Java 21+** (Project Loom features are in Java 21)
- **Maven 3.8.0+**
- **macOS/Linux/Windows** with appropriate JDK installed

## Project Structure

```
concurrent-java/
├── pom.xml                          # Maven configuration
├── src/
│   ├── main/
│   │   ├── java/com/example/loom/
│   │   │   ├── Main.java           # Entry point
│   │   │   ├── VirtualThreadExamples.java
│   │   │   ├── StructuredConcurrencyExamples.java
│   │   │   └── ThreadPoolExamples.java
│   │   └── resources/
│   │       └── logback.xml         # Logging configuration
│   └── test/
│       └── java/com/example/loom/
│           ├── VirtualThreadTests.java
│           └── ThreadPoolTests.java
└── README.md                        # This file
```

## Maven Commands

### Build the Project
```bash
mvn clean compile
```

### Run Tests
```bash
mvn test
```

### Package the Application
```bash
mvn package
```

### Run the Application
```bash
# Using exec-maven-plugin
mvn exec:java

# Or using the JAR
java -jar target/concurrent-java-loom-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Clean Build Artifacts
```bash
mvn clean
```

### View Project Info
```bash
mvn project-info-reports:dependencies
```

## Compiler Configuration

The project is configured to:
- Use Java 21 as the target version
- Enable preview features (`--enable-preview`) for Project Loom
- Support both compilation and test execution with preview features

## Key Loom Features Demonstrated

### 1. Virtual Threads
Creating lightweight threads that are managed by the JVM:
```java
Thread vthread = Thread.ofVirtual()
    .name("my-virtual-thread")
    .unstarted(() -> {
        // task code
    });
vthread.start();
```

### 2. Virtual Thread Executor Service
Processing many tasks with minimal resource overhead:
```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10000; i++) {
        executor.submit(task);
    }
}
```

### 3. Structured Concurrency
Managing groups of related concurrent tasks:
```java
try (var scope = new StructuredTaskScope<String>()) {
    Subtask<String> task1 = scope.fork(() -> computeValue());
    scope.join();
    String result = task1.get();
}
```

## Performance Characteristics

Virtual threads provide significant benefits for:
- **I/O-bound workloads**: Can create millions of virtual threads
- **High concurrency scenarios**: Better scalability than platform threads
- **Simple application code**: No callback-based or reactive programming needed

## Dependencies

- **JUnit 5**: Testing framework
- **SLF4J**: Logging facade
- **Logback**: Logging implementation

All dependencies are automatically managed by Maven.

## Troubleshooting

### "Preview feature" Compilation Error
Ensure `--enable-preview` is configured in `maven-compiler-plugin`:
```xml
<compilerArgs>
    <arg>--enable-preview</arg>
</compilerArgs>
```

### Java Version Not Found
Verify Java 21+ is installed:
```bash
java -version
```

### Test Failures
Ensure tests also have `--enable-preview` in `maven-surefire-plugin`:
```xml
<argLine>--enable-preview</argLine>
```

## Resources

- [Project Loom JEP 444](https://openjdk.org/jeps/444)
- [Java 21 Release Notes](https://www.oracle.com/java/technologies/javase/21-relnotes.html)
- [Structured Concurrency JEP](https://openjdk.org/jeps/453)

## License

This project is provided as an educational example.
