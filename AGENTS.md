# AGENTS.md - AI Learning Project

## Project Overview
Spring Boot 3.2 REST API for learning AI integration with OpenAI. Built with Spring AI framework for LLM chat capabilities with conversation history and configurable system prompts.

## Architecture

### Core Design: Three-Layer Service Architecture
```
ChatController (REST endpoints)
    ↓
ChatService (chat logic, message building, streaming)
    ├→ ConversationHistoryService (in-memory persistence)
    ├→ PromptConfigService (system prompt templates)
    └→ Spring AI ChatModel (OpenAI integration)
```

**Critical Pattern**: Services are autowired via `@RequiredArgsConstructor` and coordinate through clean interfaces. No direct database persistence—conversation history lives in `ConcurrentHashMap<String, List<Conversation>>`.

### Message Flow for Chat Requests
1. `ChatRequest` arrives at `/api/chat/message` or `/api/chat/stream`
2. `ChatService.chat()` or `ChatService.streamChat()` builds message list:
   - SystemMessage (from PromptConfigService template)
   - User+Assistant pairs from conversation history (capped at `max-history`)
   - New UserMessage from request
3. `Spring AI ChatModel.call()` or `.stream()` invokes OpenAI
4. Response saved to `ConversationHistoryService` before returning to client

### Configuration-Driven Behavior
- **Prompt templates**: Initialized in `PromptConfigService.@PostConstruct` with 5 built-ins (default, coding, debugging, explanation, creative)
- **Settable at runtime**: `POST /api/chat/prompts/{templateName}` updates in-memory Map
- **Max history**: `ai.prompts.max-history` config controls conversation context window
- **API key**: Injected from `OPENAI_API_KEY` environment variable via `${...}` placeholder in `application.yml`

## Key Design Decisions

### In-Memory Only Storage
Conversation history persists only in `ConcurrentHashMap` during application runtime. Useful for learning but **not suitable for production**. For persistence, consider replacing with repository layer (Spring Data JPA, MongoDB, etc.).

### Reactive Streaming with SSE
- `/api/chat/stream` returns `SseEmitter` that publishes chunks via `chatModel.stream().subscribe()`
- Full response accumulated in `StringBuilder` before saved to history (ensures complete context)
- Error handling: `completeWithError()` on exceptions

### Record-Based DTOs
All DTOs are `record` types (`ChatRequest`, `ChatResponse`, `ConversationResponse`). Immutable by default; validation via Jakarta annotations on record fields.

### Concurrent Safety
- `ConcurrentHashMap` over HashMap for thread-safe history access without explicit locking
- Services are stateless; only stores manage state

## Build & Run

```bash
# Build
mvn clean install

# Run locally
mvn spring-boot:run

# Build JAR
mvn clean package
java -jar target/ai-learning-0.0.1-SNAPSHOT.jar

# With custom config
OPENAI_API_KEY=key OPENAI_MODEL=gpt-4 mvn spring-boot:run
```

**Note**: Java 21+ required (set in pom.xml).

## API Endpoints & Usage

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/chat/message` | POST | Synchronous chat (returns full JSON response) |
| `/api/chat/stream` | POST | Streaming chat (SSE response) |
| `/api/chat/conversations` | GET | List all conversation IDs |
| `/api/chat/conversation/{id}` | GET | Fetch history for conversation |
| `/api/chat/conversation/{id}` | DELETE | Clear conversation history |
| `/api/chat/prompts` | GET | List all prompt templates |
| `/api/chat/prompts/{name}` | POST | Create/update prompt template |

**Example: Send message with custom prompt template**
```bash
curl -X POST http://localhost:8080/api/chat/message \
  -d '{"message":"Explain REST","promptTemplate":"coding","conversationId":"conv123"}' \
  -H "Content-Type: application/json"
```

## Common Development Tasks

### Adding a New Prompt Template
Edit `PromptConfigService.init()` or call `POST /api/chat/prompts/mytemplate` at runtime. Templates auto-available immediately in `ChatService.buildMessages()`.

### Extending ChatRequest
Record fields are validated; add new `@NotBlank` or `@Size` constraints as needed. Changes propagate automatically to request handling in `ChatController.sendMessage()` and `streamMessage()`.

### Debugging Message Construction
- Set log level in `application.yml`: `logging.level.com.learning.ai: DEBUG`
- `ChatService.buildMessages()` logs via `@Slf4j`; trace SystemMessage selection and history window

### Testing Integration
Place test OpenAI key in `OPENAI_API_KEY` env var, then run IT. Spring AI's MockChatModel available for unit testing without API calls.

## Important Files & Responsibilities

| File | Purpose |
|------|---------|
| `AiConfig.java` | Defines ChatClient bean (currently unused; ChatModel used directly) |
| `PromptConfigService.java` | Template registration and retrieval; initializes 5 built-in prompts |
| `ChatService.java` | Message orchestration, streaming logic, history saving |
| `ConversationHistoryService.java` | Thread-safe in-memory conversation store |
| `ChatController.java` | HTTP request routing, validation, response formatting |
| `application.yml` | OpenAI credentials, model selection, custom prompt defaults |

## Spring AI Integration Points

- **Dependency**: `spring-ai-openai-spring-boot-starter:1.0.0-M1` (milestone version)
- **ChatModel Bean**: Auto-configured by Spring Boot from `OPENAI_API_KEY` and `application.yml` settings
- **Message Types**: `SystemMessage`, `UserMessage`, `AssistantMessage` from `org.springframework.ai.chat.messages.*`
- **Prompt**: Wrapper around message list; passed to `chatModel.call()` or `chatModel.stream()`

## Known Limitations & TODOs

- **No persistence**: Restart clears all conversation history
- **ChatClient bean unused**: `AiConfig.chatClient()` defined but not utilized (use ChatModel directly instead)
- **Error handling**: Missing retry logic for OpenAI API timeouts
- **No request/response logging**: Sensitive data (API keys) risk exposure if logging enabled

## Conventions for Contributors

1. **Logging**: Always use `@Slf4j` on Services; avoid System.out
2. **Dependency Injection**: Use `@RequiredArgsConstructor` + constructor params (no field injection)
3. **Configuration**: Externalize via `@Value` or `@ConfigurationProperties` to `application.yml`
4. **Thread Safety**: Wrap shared state in `ConcurrentHashMap` or synchronize explicitly
5. **Validation**: Use Jakarta Validation annotations on DTOs; let framework handle 400 errors
6. **API Responses**: Wrap results in `ResponseEntity<T>` with appropriate HTTP status codes

