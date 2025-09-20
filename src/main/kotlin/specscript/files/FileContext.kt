package specscript.files

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import specscript.commands.CommandLibrary
import specscript.commands.variables.AssignVariable
import specscript.language.*
import specscript.language.types.Type
import specscript.language.types.TypeRegistry
import specscript.language.types.TypeSpecification
import specscript.util.toDomainObject
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.isDirectory
import kotlin.io.path.name

const val YAML_SPEC_EXTENSION = ".spec.yaml"
const val MARKDOWN_SPEC_EXTENSION = ".spec.md"

/**
 * Context for running a SpecScript script inside a directory.
 * It will scan the directory for other scripts and expose them as commands.
 */
class FileContext(
    override val scriptFile: Path,
    override val variables: MutableMap<String, JsonNode> = mutableMapOf(),
    override val session: MutableMap<String, Any?> = mutableMapOf(),
    override val interactive: Boolean = false,
    override val workingDir: Path = Path.of(".")
) : ScriptContext {

    constructor(cliFile: Path, parent: ScriptContext, variables: MutableMap<String, JsonNode> = mutableMapOf()) : this(
        cliFile,
        variables,
        parent.session,
        parent.interactive
    )

    override fun clone(): ScriptContext {
        return FileContext(scriptFile, variables.toMutableMap(), session.toMutableMap(), interactive, workingDir)
    }

    override val scriptDir: Path by lazy {
        if (scriptFile.isDirectory()) {
            scriptFile
        } else {
            scriptFile.toAbsolutePath().normalize().parent
        }
    }

    override val tempDir: Path
        get() {
            return if (variables.containsKey(SCRIPT_TEMP_DIR_VARIABLE)) {
                Path.of(variables[SCRIPT_TEMP_DIR_VARIABLE]!!.textValue())
            } else {
                Files.createTempDirectory("specscript-").apply {
                    toFile().deleteOnExit()
                    variables[SCRIPT_TEMP_DIR_VARIABLE] = TextNode(toAbsolutePath().toString())
                }
            }
        }

    fun setTempDir(dir: Path) {
        variables[SCRIPT_TEMP_DIR_VARIABLE] = TextNode(dir.toAbsolutePath().toString())
    }

    override val output: JsonNode?
        get() = variables[OUTPUT_VARIABLE]
    override var error: SpecScriptCommandError? = null

    val info: DirectoryInfo by lazy { SpecScriptDirectories.get(scriptDir) }
    val name: String
        get() = scriptDir.name

    private val localFileCommands: Map<String, SpecScriptFile> by lazy { findLocalFileCommands() }
    private val importedFileCommands: Map<String, SpecScriptFile> by lazy { findImportedCommands() }
    private val subdirectoryCommands: Map<String, DirectoryInfo> by lazy { findSubcommands() }

    override val types: TypeRegistry by lazy {
        TypeRegistry().apply {
            loadTypes(info)
        }
    }

    override fun getCommandHandler(command: String): CommandHandler {

        // Variable syntax
        val match = VARIABLE_REGEX.matchEntire(command)
        if (match != null) {
            return AssignVariable(match.groupValues[1])
        }

        // Standard commands
        CommandLibrary.commands[command]?.let { handler ->
            return handler
        }

        // File commands
        localFileCommands[command]?.let { handler ->
            return handler
        }

        // Imported commands
        importedFileCommands[command]?.let { handler ->
            return handler
        }

        // No handler found for command
        throw ScriptingException("Unknown command: $command")
    }

    private fun findLocalFileCommands(): Map<String, SpecScriptFile> {

        val commands = mutableMapOf<String, SpecScriptFile>()

        for (file in scriptDir.toFile().listFiles()!!) {
            addCommand(commands, file.toPath())
        }

        return commands
    }

    private fun findImportedCommands(): Map<String, SpecScriptFile> {

        val commands = mutableMapOf<String, SpecScriptFile>()

        for (cliFile in info.imports) {
            addCommand(commands, scriptDir.resolve(cliFile))
        }

        return commands
    }

    private fun addCommand(commands: MutableMap<String, SpecScriptFile>, file: Path) {
        if (file.isDirectory()) return
        if (!(file.name.endsWith(YAML_SPEC_EXTENSION) || file.name.endsWith(MARKDOWN_SPEC_EXTENSION))) return

        val name = asScriptCommand(file.name)
        commands[name] = SpecScriptFile(file)
    }

    private fun findSubcommands(): Map<String, DirectoryInfo> {
        val subcommands = mutableMapOf<String, DirectoryInfo>()

        Files.list(scriptDir)
            .filter { it.isDirectory() && it.name != "tests" && it.hasCliCommands() }
            .forEach { dir ->
                subcommands[asCliCommand(dir.name)] = SpecScriptDirectories.get(dir)
            }

        return subcommands
    }

    fun getAllCommands(): List<CommandInfo> {
        val commands = mutableListOf<CommandInfo>()
        commands.addAll(localFileCommands.values)
        commands.addAll(subdirectoryCommands.values)

        return commands.sortedBy { it.name }
    }

    fun getCliScriptFile(rawCommand: String): SpecScriptFile? {
        val command = asScriptCommand(rawCommand)
        return localFileCommands[command]
    }

    fun getSubcommand(rawCommand: String): DirectoryInfo? {
        val command = asCliCommand(rawCommand)
        return subdirectoryCommands[command]
    }
}

/**
 * Load types from directory.
 */
private fun TypeRegistry.loadTypes(info: DirectoryInfo) {
    for ((name, type) in info.types.fields()) {
        registerType(Type(name, type.toDomainObject(TypeSpecification::class)))
    }
}


private fun Path.hasCliCommands(): Boolean {
    return Files.walk(this).anyMatch { file ->
        !file.isDirectory()
                && (file.name.endsWith(YAML_SPEC_EXTENSION) || file.name.endsWith(MARKDOWN_SPEC_EXTENSION))
    }
}

//
// Command names
//

/**
 * Creates command name from file name by stripping extension and converting dashes to spaces.
 */
fun asScriptCommand(commandName: String): String {
    var command = commandName

    // Strip extension
    if (command.endsWith(YAML_SPEC_EXTENSION)) {
        command = command.take(commandName.length - YAML_SPEC_EXTENSION.length)
    }
    if (command.endsWith(MARKDOWN_SPEC_EXTENSION)) {
        command = command.take(commandName.length - MARKDOWN_SPEC_EXTENSION.length)
    }

    // Spaces for dashes
    command = command.replace('-', ' ')

    // Start with a capital
    command =
        command.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    return command
}

fun asCliCommand(commandName: String): String {
    var command = commandName

    // Strip extension
    if (command.endsWith(YAML_SPEC_EXTENSION)) {
        command = command.take(commandName.length - YAML_SPEC_EXTENSION.length)
    }
    if (command.endsWith(MARKDOWN_SPEC_EXTENSION)) {
        command = command.take(commandName.length - MARKDOWN_SPEC_EXTENSION.length)
    }

    // Dashes for spaces
    command = command.replace(' ', '-')

    // All lower case
    command = command.lowercase(Locale.getDefault())

    return command
}
