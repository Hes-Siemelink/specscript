# LLM Prompt Command — Implementation Proposal

## Problem

The `LLM prompt` command needs to send prompts to LLM services. We need:

1. A unified message format across providers (OpenAI, Anthropic, Google, etc.)
2. A mock provider for spec tests (no real API keys in CI)
3. A path to tool calling — eventually SpecScript scripts will be registered as tools the LLM can invoke
4. Conceptual parity with the TypeScript implementation (which will use LangChain.js or Vercel AI SDK)

The command shape:

```yaml
LLM prompt:
  message: Translate 'Hello' to Spanish
  provider: mock-anthropic
  model: claude-3-5-sonnet
  api-key: ${env.ANTHROPIC_API_KEY}
```

Value syntax (when defaults are set):

```yaml
LLM prompt: Translate 'Hello' to Spanish
```

### Naming Decisions

- **`LLM` prefix** (not `AI`): More precise — this command specifically calls a language model.
  "AI" is reserved as a broader category (future: image generation, embeddings, etc.). Consistent
  with MCP using its own prefix (`Mcp server`, not `AI server`).
- **`prompt`** (not `chat`): This command is single-shot. "Chat" implies multi-turn conversation and
  should be reserved for a future multi-turn command. "Prompt" matches how users think about the
  action: "I'm sending a prompt to an LLM."
- **`message:` field** (not `prompt:`): Avoids redundancy with the command name
  (`LLM prompt: prompt: ...` stutters). "Message" is what you're sending to the LLM. Evolves
  naturally to `messages:` (plural) for future multi-turn support.

## Library Decision: LangChain4j

### Options Considered

| | KOOG (JetBrains) | LangChain4j | Own Abstraction |
|---|---|---|---|
| Stability | 1.0 (just released) | 1.17.0, 2+ years, 12.4k stars | N/A |
| Language | Kotlin-native (KMP) | Java-first, has `langchain4j-kotlin` module | Kotlin |
| Unified messages | Yes | Yes (ChatMessage hierarchy) | Must define ourselves |
| Tool calling | Yes | Yes (ToolSpecification + execution loop) | Must implement ourselves |
| MCP support | Yes | Yes (langchain4j-agentic-mcp) | N/A |
| TypeScript parallel | No equivalent | LangChain.js / Vercel AI SDK share same concepts | Separate concern |
| Risk | New 1.0, small community | Boring and stable, large community | Maintenance → half-baked framework |

### Why LangChain4j

1. **Boring is good.** Stable, well-documented, predictable API evolution. No surprises.
2. **Concepts transfer to TypeScript.** LangChain4j's `ChatModel`, `ChatMessage`, `ToolSpecification`
   map 1:1 to LangChain.js / Vercel AI SDK equivalents. The TypeScript implementation can follow the
   same mental model with a different library.
3. **Tool calling is built-in.** When we want SpecScript scripts as LLM tools, we implement
   `ToolSpecification` + `ToolExecutionResultMessage`. The provider differences (OpenAI's `tool_calls`
   vs Anthropic's `tool_use` blocks) are already handled.
4. **We don't build a framework.** Every new LLM feature (structured output, streaming, multimodal)
   arrives as a library update, not as code we write and maintain per provider.
5. **Mockability.** `ChatModel` is a plain interface. A `MockChatModel` that returns canned responses
   is ~20 lines of code.

### Why Not KOOG

KOOG is now 1.0 and Kotlin-native, which is appealing. However:
- Smaller community, less battle-tested in production
- Full agent framework (graph strategies, persistence) — more machinery than we need right now
- No TypeScript equivalent exists — the TypeScript implementation would diverge conceptually
- KOOG could be reconsidered later if it gains traction and we want deeper Kotlin integration

### Why Not Own Abstraction

The "just use Ktor" approach works until tool calling. At that point, each provider's wire format for
tool calls, tool results, and multi-turn tool-use loops diverges. We'd be writing and maintaining
provider-specific serialization for a moving target. That's exactly what LangChain4j already does.

## Architecture

### Dependencies

```kotlin
// build.gradle.kts
implementation("dev.langchain4j:langchain4j:1.17.0")           // core: ChatModel, messages, tools
implementation("dev.langchain4j:langchain4j-open-ai:1.17.0")   // OpenAI provider (Phase 2)
implementation("dev.langchain4j:langchain4j-anthropic:1.17.0") // Anthropic provider (Phase 2)
```

Only `langchain4j` (core) is needed for Phase 1. Provider modules are added in Phase 2.

### Mock Strategy: `LLM answers` Command

*(Follows the existing `Answers` command pattern.)*

Rather than hardcoding mock responses in code, spec tests explicitly declare their expected
prompt→response mappings via a new `LLM answers` command. This is the same pattern as `Answers`
(which prerecords user prompt responses for non-interactive testing).

```yaml
LLM answers:
  "Translate 'Hello' to Spanish": Hola

LLM prompt:
  message: Translate 'Hello' to Spanish
  provider: mock-anthropic
  model: claude-3-5-sonnet

Expected output: Hola
```

**Why this is better than a hardcoded mock:**
- Test setup is visible in the spec (self-documenting)
- Each test case defines its own responses (no shared global state)
- Throws a descriptive error on unmatched prompts — no silent wrong behavior
- Follows an established SpecScript pattern

Implementation:

```kotlin
object LlmAnswers : CommandHandler("LLM answers", "ai/prompt"), ObjectHandler {
    private const val LLM_ANSWERS_KEY = "ai.llm-answers"

    fun getFrom(context: ScriptContext): Map<String, String> =
        (context.session[LLM_ANSWERS_KEY] as? Map<String, String>) ?: emptyMap()

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val answers = mutableMapOf<String, String>()
        data.fields().forEach { (key, value) -> answers[key] = value.asText() }
        context.session[LLM_ANSWERS_KEY] = answers
        return null
    }
}
```

The `MockChatModel` then looks up responses from the session:

```kotlin
class MockChatModel(private val context: ScriptContext) : ChatModel {
    override fun chat(request: ChatRequest): ChatResponse {
        val userMessage = request.messages().filterIsInstance<UserMessage>().last()
        val text = userMessage.text()
        val answers = LlmAnswers.getFrom(context)
        val response = answers[text]
            ?: throw SpecScriptCommandError("No LLM answer recorded for prompt: '$text'")
        return ChatResponse.builder()
            .aiMessage(AiMessage.from(response))
            .build()
    }
}
```

### Command: LLM prompt

```kotlin
object LlmPrompt : CommandHandler("LLM prompt", "ai/prompt"), ObjectHandler, ValueHandler {
    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val merged = mergeWithDefaults(data, context)
        val model = resolveModel(merged, context)
        val messages = buildMessages(merged)
        val response = model.chat(ChatRequest.builder().messages(messages).build())
        return TextNode(response.aiMessage().text())
    }

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {
        // Simple string = message text, uses all defaults
        val defaults = LlmDefaults.getFrom(context)
            ?: throw SpecScriptCommandError("LLM defaults must be set when using value syntax")
        val merged = defaults.deepCopy().put("message", data.asText())
        return execute(merged, context)
    }

    private fun resolveModel(data: ObjectNode, context: ScriptContext): ChatModel {
        val provider = data.get("provider")?.asText() ?: "openai"
        val modelName = data.get("model")?.asText()
        val apiKey = data.get("api-key")?.asText()
        return ChatModelRegistry.get(provider, modelName, apiKey, context)
    }

    private fun buildMessages(data: ObjectNode): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        data.get("system")?.asText()?.let { messages.add(SystemMessage.from(it)) }
        data.get("message")?.asText()?.let { messages.add(UserMessage.from(it)) }
        return messages
    }
}
```

### Command: LLM defaults

```kotlin
object LlmDefaults : CommandHandler("LLM defaults", "ai/prompt"), ObjectHandler, ValueHandler {
    private const val LLM_DEFAULTS = "ai.llm-defaults"

    fun getFrom(context: ScriptContext): ObjectNode? =
        context.session[LLM_DEFAULTS] as? ObjectNode

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        context.session[LLM_DEFAULTS] = data
        return null
    }

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? =
        getFrom(context) ?: Json.newObject()
}
```

### ChatModel Registry

```kotlin
object ChatModelRegistry {
    private val factories = mutableMapOf<String, (model: String?, apiKey: String?, context: ScriptContext) -> ChatModel>()

    init {
        // Mock providers — always available, all resolve to same mock
        register("mock") { _, _, ctx -> MockChatModel(ctx) }
        register("mock-anthropic") { _, _, ctx -> MockChatModel(ctx) }
        register("mock-openai") { _, _, ctx -> MockChatModel(ctx) }
    }

    fun register(provider: String, factory: (model: String?, apiKey: String?, context: ScriptContext) -> ChatModel)
    fun get(provider: String, model: String?, apiKey: String?, context: ScriptContext): ChatModel
}
```

Real providers (OpenAI, Anthropic) register themselves in Phase 2:

```kotlin
// Phase 2 — not this round
ChatModelRegistry.register("openai") { model, apiKey, _ ->
    OpenAiChatModel.builder().apiKey(apiKey).modelName(model).build()
}
ChatModelRegistry.register("anthropic") { model, apiKey, _ ->
    AnthropicChatModel.builder().apiKey(apiKey).modelName(model).build()
}
```

### File Layout

```
src/main/kotlin/specscript/commands/ai/
├── LlmPrompt.kt             # LLM prompt command handler
├── LlmDefaults.kt           # LLM defaults command handler
├── LlmAnswers.kt            # LLM answers command (mock responses for tests)
├── ChatModelRegistry.kt     # Provider→ChatModel factory registry
└── MockChatModel.kt         # ChatModel impl that reads from LlmAnswers session state
```

## Execution Plan

### Phase 1: Core + Mock (this round)

- [ ] Add `langchain4j` core dependency to build.gradle.kts
- [ ] Implement `LlmAnswers` command (session-stored prompt→response map, follows Answers pattern)
- [ ] Implement `MockChatModel` that reads from `LlmAnswers` session state (error on unmatched)
- [ ] Implement `ChatModelRegistry` with mock providers registered
- [ ] Implement `LlmDefaults` command (session-stored defaults, follows Http defaults pattern)
- [ ] Implement `LlmPrompt` command (merges defaults, resolves model, calls ChatModel)
- [ ] Register all three commands in `CommandLibrary`
- [ ] Update spec to use new naming (`LLM prompt`, `LLM defaults`, `LLM answers`) and `message:` field
- [ ] Validate spec examples pass

### Phase 2: Real Providers (follow-up)

- [ ] Add `langchain4j-open-ai` and `langchain4j-anthropic` dependencies
- [ ] Register real provider factories in `ChatModelRegistry`
- [ ] Add `system:` field support in the spec
- [ ] Add connection support (tie into `Connect to` for credential management)
- [ ] Streaming support (LangChain4j has `StreamingChatModel`)

### Phase 3: Tool Calling (follow-up)

- [ ] Define how SpecScript scripts are exposed as `ToolSpecification`
- [ ] Implement tool execution loop (LLM requests tool → run script → return result)
- [ ] Expose response messages to scripts (multi-turn conversations)
- [ ] Spec out the `messages:` field for full conversation control
- [ ] Introduce `LLM chat` command for multi-turn conversations

## Scope for This Round

- Kotlin only (no TypeScript)
- Mock provider only (no real API calls, no provider dependencies yet)
- The spec examples passing with mock provider
- No integration test infrastructure
- Real providers, streaming, tool calling are all Phase 2+

## Open Questions

1. **Provider naming in the spec**: The spec uses `mock-anthropic`. We register this as an alias for
   the generic mock. The mock doesn't simulate provider-specific behavior — it just returns canned
   text from `LLM answers`. This is intentional: spec tests verify SpecScript plumbing, not LLM
   behavior.

2. **langchain4j-kotlin module**: LangChain4j has Kotlin coroutine extensions (`chatAsync`). We
   could use them, but since SpecScript commands are synchronous (they return `JsonNode?`), we'll
   use the synchronous `ChatModel.chat()` API. No need for coroutines in Phase 1.

3. **Spec update required**: The existing spec draft uses `AI prompt` and `prompt:` field. We need
   to update it to `LLM prompt` with `message:` field, and add `LLM answers` setup blocks.
