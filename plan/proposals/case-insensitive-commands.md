# Proposal: Case-Insensitive Command Matching

## Problem

Command matching is case-sensitive. Writing `input schema` or `INPUT SCHEMA` instead of `Input schema` throws
`Unknown command`. This is the friction noted in the existing TODO in `CommandLibrary.kt`:

```kotlin
// TODO Store commands in canonical form: all lower case and spaces
```

## Proposed Solution

Add two utility functions to `CommandHandler.kt` and apply them at storage, lookup, and comparison sites.

### Utilities in `CommandHandler.kt`

```kotlin
fun canonicalCommandName(name: String): String = name.lowercase()

fun commandEquals(a: String, b: String): Boolean = a.equals(b, ignoreCase = true)
```

`canonicalCommandName` is used at map storage and lookup boundaries. `commandEquals` is used for inline comparisons
(`.find {}`, `when {}`) where two command names need comparing without going through a map.

Both live in `CommandHandler.kt` alongside the existing `CommandHandler` class, handler interfaces, and parameter
utilities — all command-name concerns in one place.

### Changes

**`CommandLibrary.kt`** — store keys in canonical form:

```kotlin
private fun commandMap(vararg commands: CommandHandler): Map<String, CommandHandler> {
    return commands.associateBy { canonicalCommandName(it.name) }
}
```

**`FileContext.getCommandHandler()`** — normalize the lookup key:

```kotlin
override fun getCommandHandler(command: String): CommandHandler {
    val canonical = canonicalCommandName(command)

    // Variable syntax — unchanged, checked before command lookup
    val match = VARIABLE_REGEX.matchEntire(command)
    if (match != null) {
        return AssignVariable(match.groupValues[1])
    }

    CommandLibrary.commands[canonical]?.let { return it }
    localFileCommands[canonical]?.let { return it }
    importedFileCommands[canonical]?.let { return it }

    throw SpecScriptException("Unknown command: $command")
}
```

**`FileContext.addCommand()`** — store file commands with canonical key:

```kotlin
private fun addCommand(commands: MutableMap<String, SpecScriptFile>, file: Path) {
    // ...
    val name = asScriptCommand(file.name)
    commands[canonicalCommandName(name)] = SpecScriptFile(file)
}
```

**`FileContext.getCliScriptFile()`** — normalize lookup:

```kotlin
fun getCliScriptFile(rawCommand: String): SpecScriptFile? {
    val command = asScriptCommand(rawCommand)
    return localFileCommands[canonicalCommandName(command)]
}
```

**`Script.kt`** — all `command.name == Handler.name` comparisons use `commandEquals`:

```kotlin
// getScriptInfo()
val scriptInfoCommand = commands.find { commandEquals(it.name, ScriptInfo.name) }
val inputParameterCommand = commands.find { commandEquals(it.name, InputParameters.name) }
val inputSchemaCommand = commands.find { commandEquals(it.name, InputSchema.name) }

// splitTestCases()
if (commandEquals(command.name, TestCase.name)) {

// splitTests()
for (command in commands) {
    when {
        commandEquals(command.name, BeforeTests.name) -> { ... }
        commandEquals(command.name, AfterTests.name) -> { ... }
        commandEquals(command.name, Tests.name) -> { ... }
    }
}
```

Note: `splitTests()` currently uses `when (command.name)` with string targets. Since `when` matching is exact,
it changes to `when {}` with `commandEquals` predicates.

**`TestUtil.kt`** — same treatment for test discovery comparisons:

```kotlin
val hasNewTests = script.commands.any { commandEquals(it.name, Tests.name) }
val hasLegacyTests = script.commands.any { commandEquals(it.name, TestCase.name) }

// getTestTitle()
val command = commands.find { commandEquals(it.name, commandHandler.name) }
```

**`McpServer.kt`** — replace hardcoded strings with handler references and `commandEquals`:

```kotlin
val inputSchemaCommand = scriptFile.script.commands.find { commandEquals(it.name, InputSchema.name) }
val inputParamsCommand = scriptFile.script.commands.find { commandEquals(it.name, InputParameters.name) }
```

**`Eval.kt`** — no change needed. Calls `getCommandHandler()` which already normalizes.

### What does NOT change

- **`CommandHandler.name`** — stays as-is (`"Input schema"`, `"Http server"`, etc.). Used for display and schema path
  derivation.
- **Schema validation** — `CommandHandler.validate()` uses `this.name` (the display name) to derive the schema file
  path. Unaffected.
- **Existing commands** — no changes to any command implementation.
- **Spec files and code examples** — all already use canonical casing. No updates needed.
- **Samples** — no updates needed.
- **`asCliCommand()`** — already lowercases. No change needed.
- **`findSubcommands()` / `getSubcommand()`** — already use `asCliCommand()` which lowercases. No change needed.

### Summary

| File | Changes |
|------|---------|
| `CommandHandler.kt` | Add `canonicalCommandName()` and `commandEquals()` |
| `CommandLibrary.kt` | Canonical key in `commandMap()`, remove TODO |
| `FileContext.kt` | Canonical lookup in `getCommandHandler()`, `addCommand()`, `getCliScriptFile()` |
| `Script.kt` | `commandEquals()` in `getScriptInfo()`, `splitTestCases()`, `splitTests()` |
| `TestUtil.kt` | `commandEquals()` in `getTestCases()`, `getTestTitle()` |
| `McpServer.kt` | Replace hardcoded strings with handler references + `commandEquals()` |

Total: ~15 line changes across 6 files. Zero changes to commands, specs, samples, or tests.
