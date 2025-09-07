# SpecScript Library Usage

SpecScript is designed as a library with a simple CLI interface for programmatic usage.

## Programmatic usage

You can use SpecScript programmatically in your Kotlin applications:

```kotlin
import specscript.cli.SpecScriptMain
import java.nio.file.Path

// Execute a script file
val result = SpecScriptMain.main(arrayOf("script.cli"), Path.of("/working/dir"))
if (result == 0) {
    println("Script executed successfully")
} else {
    println("Script execution failed")
}
```

## CLI Options Integration

SpecScript uses the same command-line options as Instacli but optimized for non-interactive usage:

```kotlin
// Execute with output capture
SpecScriptMain.main(arrayOf("--output", "script.cli"))

// Execute in debug mode  
SpecScriptMain.main(arrayOf("--debug", "script.cli"))
```

## Fast execution

SpecScript CLI is optimized for:
- **Minimal startup time** - No interactive components loaded
- **Clear error messages** - Fails fast with specific error information  
- **Library integration** - Designed to be embedded in other applications
- **Non-interactive operation** - No prompts or user input required

## Shared utilities

SpecScript provides shared utilities that both SpecScript and Instacli use:

```kotlin
import specscript.cli.CliFileUtils
import specscript.cli.CliErrorReporter

// Resolve files with .cli extension support
val file = CliFileUtils.resolveFile("script", workingDir)

// Execute files with consistent behavior  
CliFileUtils.executeFile(file, options, context, output)

// Report errors consistently
CliErrorReporter.reportInvocationError(exception)
```