# Scaling Potato 🥔

A distributed NLP query processing system with:
- **Backend**: Thread pool socket server + HTTP REST API
- **Database**: H2 embedded SQL database for query history
- **Frontend**: Simple web interface for natural language queries
- **NLP**: OpenAI API integration for query processing
- **Concurrency**: Project Loom virtual threads for massive scalability

## Architecture

```
┌─────────────────────────────────────────────────────┐
│               Frontend (Web UI)                     │
│          (HTML/CSS/JS - index.html)                 │
└──────────────┬──────────────────────────────────────┘
               │
     ┌─────────┴─────────┐
     │                   │
┌────▼────────────────┐ ┌▼────────────────────┐
│  HTTP Server        │ │  Socket Server      │
│  (REST API)         │ │  (Thread Pool)      │
│  Port: 8080         │ │  Port: 9999         │
└────┬─────┬──────────┘ └┬──────────┬─────────┘
     │     │            │          │
     │  ┌──┴────────────┴──┐       │
     │  │   NLP Service    │       │
     │  │  (OpenAI API)    │       │
     │  └──┬───────────────┘       │
     └─────┼──────────────────┬────┘
          │                  │
     ┌────▼──────────────────▼────┐
     │   Database Service         │
     │  (H2 Embedded Database)    │
     │  Query History & Stats     │
     └────────────────────────────┘
```

## Setup

### Prerequisites
- Java 25+ (with Project Loom)
- Maven 3.8.0+
- OpenAI API key (optional - app works with mock responses without it)

### Installation

1. **Clone the repository**
```bash
cd /Users/gp/scaling-potato-java
```

2. **Configure environment variables**
Edit `.env` file and add your OpenAI API key:
```
OPENAI_API_KEY=sk-your-api-key-here
DATABASE_URL=jdbc:h2:mem:scalingpotato
PORT=9999
THREAD_POOL_SIZE=50
```

3. **Build the project**
```bash
mvn clean package
```

## Running the Application

Start the server:
```bash
mvn exec:java
```

Output should show:
```
[INFO] === Scaling Potato NLP Application ===
[INFO] Configuration loaded:
[INFO]   Port: 9999
[INFO]   Thread Pool Size: 50
[INFO]   Database: jdbc:h2:mem:scalingpotato
[INFO]   OpenAI API configured: true
[INFO] Server started successfully!
[INFO] Open http://localhost:8080 in your browser
[INFO] Press 'q' to shutdown the servers
```

## Using the Application

### Web Interface
Open `http://localhost:8080` in your browser to access the interactive UI.

**Features:**
- Enter natural language queries
- View processing time and responses
- Check database statistics

### REST API Endpoints

**POST /api/query**
```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"query": "What is machine learning?"}'
```

Response:
```json
{
  "response": "Machine learning is...",
  "processingTime": 245
}
```

**GET /api/stats**
```bash
curl http://localhost:8080/api/stats
```

Response:
```json
{
  "totalQueries": 42,
  "averageProcessingTimeMs": 235.50
}
```

### Socket Server Connection
Connect directly to the socket server on port 9999:

```bash
telnet localhost 9999
```

Send commands:
- Regular query: Type your question and press Enter
- Get stats: Type `STATS` and press Enter
- Exit: Type `EXIT` and press Enter

Example:
```
$ telnet localhost 9999
Connected to localhost.
Escape character is '^]'.
What is Java?
RESPONSE:{"response": "Java is...", "timestamp": 1702041600000}
TIME:152ms
---
STATS
STATS:
Total queries: 1
Average processing time: 152.00ms
---
EXIT
Goodbye!
```

## Project Structure

```
src/
├── main/
│   ├── java/com/example/
│   │   ├── nlp/
│   │   │   ├── NLPService.java          # OpenAI integration
│   │   │   └── ScalingPotatoApp.java    # Main entry point
│   │   ├── db/
│   │   │   └── DatabaseService.java     # H2 database operations
│   │   ├── loom/
│   │   │   ├── Main.java                # Original Loom examples
│   │   │   ├── VirtualThreadExamples.java
│   │   │   └── ThreadPoolExamples.java
│   │   └── server/
│   │       ├── QueryServer.java         # Socket server
│   │       ├── ClientHandler.java       # Client request handling
│   │       └── WebServer.java           # HTTP server
│   └── resources/
│       ├── index.html                   # Web UI
│       ├── logback.xml                  # Logging config
│       └── .env                         # Environment config
└── test/
    └── java/com/example/loom/
        ├── VirtualThreadFeatureTests.java
        ├── VirtualThreadExecutorFeatureTests.java
        └── ...
```

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Java | Project Loom (Virtual Threads) | 25 |
| Build | Maven | 3.8+ |
| Database | H2 Embedded | 2.2.220 |
| NLP | OpenAI API | gpt-3.5-turbo |
| Config | java-dotenv | 1.0.0 |
| Logging | SLF4J + Logback | 2.0.9 |
| Testing | JUnit 5 | 5.10.0 |

## Performance Characteristics

- **Virtual Threads**: Can handle 10,000+ concurrent connections
- **Thread Pool**: Configurable size (default 50 threads)
- **Database**: In-memory H2 for fast query storage and retrieval
- **Response Time**: Typically 150-300ms with OpenAI API

## Configuration

### Environment Variables (.env)

```env
# OpenAI Configuration
OPENAI_API_KEY=sk-your-api-key-here

# Database Configuration
DATABASE_URL=jdbc:h2:mem:scalingpotato
DATABASE_USER=sa
DATABASE_PASSWORD=

# Server Configuration
PORT=9999                  # Socket server port
HTTP_PORT=8080            # Web server port (hardcoded)
THREAD_POOL_SIZE=50       # Number of threads in pool
```

## Building and Testing

### Run all tests
```bash
mvn clean test
```

### Run specific test class
```bash
mvn test -Dtest=VirtualThreadFeatureTests
```

### Build with dependencies
```bash
mvn clean package assembly:assembly
```

### Generate documentation
```bash
mvn javadoc:javadoc
```

## Troubleshooting

### Port already in use
```
Error: Address already in use
```
Change the port in `.env` or kill the process:
```bash
lsof -ti:8080,9999 | xargs kill -9
```

### OpenAI API errors
```
Error: 401 Unauthorized
```
- Verify your API key is correct in `.env`
- Ensure it has access to the chat completion API
- Without API key, app runs in mock mode

### Database errors
```
Error: H2 driver not found
```
```bash
mvn clean install
```

### Java version issues
```
Invalid source release 25 with --enable-preview
```
Update to Java 25+:
```bash
java --version  # Should be 25+
```

## Contributing

1. Create a feature branch
2. Make your changes
3. Run tests: `mvn test`
4. Commit and push

## Future Enhancements

- [ ] Async request handling with Project Loom structured concurrency
- [ ] Caching layer for repeated queries
- [ ] Metrics and monitoring dashboard
- [ ] Support for multiple LLM providers
- [ ] Query result ranking/filtering
- [ ] User authentication and API keys
- [ ] Rate limiting and throttling
- [ ] Integration with external knowledge bases

## License

MIT License - See LICENSE file for details

## Contact

For questions or issues, open an issue on GitHub.
