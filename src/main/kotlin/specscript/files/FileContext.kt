package specscript.files

import specscript.commands.CommandLibrary
import specscript.commands.variables.AssignVariable
import specscript.language.*
import specscript.language.types.Type
import specscript.language.types.TypeRegistry
import specscript.language.types.TypeSpecification
import specscript.util.Json
import specscript.util.toDomainObject
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.StringNode
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
    override var workingDir: Path = Path.of("."),
    override val scriptHome: Path = scriptFile.let {
        if (it.isDirectory()) it else it.toAbsolutePath().normalize().parent
    }
) : ScriptContext {

    constructor(cliFile: Path, parent: ScriptContext, variables: MutableMap<String, JsonNode> = mutableMapOf()) : this(
        cliFile,
        variables,
        parent.session,
        parent.interactive
    ) {
        // Propagate parent's connection definitions for inheritance (first one wins)
        if (parent is FileContext) {
            val overrides = getConnectionOverrides()
            for ((name, value) in parent.info.connections.properties()) {
                overrides.putIfAbsent(name, value)
            }
        }
    }

    override fun clone(): ScriptContext {
        return FileContext(scriptFile, variables.toMutableMap(), session.toMutableMap(), interactive, workingDir, scriptHome)
    }

    override val scriptDir: Path by lazy {
        if (scriptFile.isDirectory()) {
            scriptFile
        } else {
            scriptFile.toAbsolutePath().normalize().parent
        }
    }

    init {
        variables[SCRIPT_DIR_VARIABLE] = StringNode(scriptHome.toAbsolutePath().toString())
        variables[PWD_VARIABLE] = StringNode(System.getProperty("user.dir"))
        variables[ENV_VARIABLE] = Json.newObject(System.getenv())
        variables[SCRIPT_TEMP_DIR_VARIABLE] = StringNode(
            Files.createTempDirectory("specscript-").apply { deleteOnShutdown() }.toAbsolutePath().toString()
        )

        PackageRegistry.autoPackagePath = PackageRegistry.findEnclosingPackageLibrary(scriptDir)
    }

    override val tempDir: Path
        get() = Path.of(variables[SCRIPT_TEMP_DIR_VARIABLE]!!.stringValue())

    fun setTempDir(dir: Path) {
        variables[SCRIPT_TEMP_DIR_VARIABLE] = StringNode(dir.toAbsolutePath().toString())
    }

    override val output: JsonNode?
        get() = variables[OUTPUT_VARIABLE]
    override var error: SpecScriptCommandError? = null

    val info: DirectoryInfo by lazy { SpecScriptDirectories.get(scriptDir) }
    val name: String
        get() = scriptDir.name

    @Suppress("UNCHECKED_CAST")
    fun getConnectionOverrides(): MutableMap<String, JsonNode> {
        return session.getOrPut("connect-to.overrides") { mutableMapOf<String, JsonNode>() } as MutableMap<String, JsonNode>
    }

    /**
     * When set, command lookup falls back to this context for local file commands and imports.
     * Used by Run's inline script form so the inline block sees the host script's commands.
     */
    var parentCommandLookup: FileContext? = null

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

        val canonical = canonicalCommandName(command)

        // Standard commands
        CommandLibrary.commands[canonical]?.let { handler ->
            return handler
        }

        // File commands
        localFileCommands[canonical]?.let { handler ->
            return handler
        }

        // Imported commands
        importedFileCommands[canonical]?.let { handler ->
            return handler
        }

        // Delegate to parent context (for inline scripts that need host commands)
        parentCommandLookup?.let { parent ->
            try {
                return parent.getCommandHandler(command)
            } catch (_: SpecScriptException) {
                // Parent didn't have it either, fall through to error
            }
        }

        // No handler found for command
        throw SpecScriptException("Unknown command: $command")
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

        for (packageImport in info.parsedImports) {
            if (packageImport.local) {
                commands.putAll(
                    PackageRegistry.scanLocalCommands(
                        scriptDir, packageImport.source, packageImport.items
                    )
                )
            }

            else {
                val packageDir = PackageRegistry.findPackage(packageImport.source) ?: continue
                commands.putAll(
                    PackageRegistry.scanCommands(packageDir, packageImport.items)
                )
            }
        }

        return commands
    }

    private fun addCommand(commands: MutableMap<String, SpecScriptFile>, file: Path) {
        if (file.isDirectory()) return
        if (!(file.name.endsWith(YAML_SPEC_EXTENSION) || file.name.endsWith(MARKDOWN_SPEC_EXTENSION))) return

        val name = asScriptCommand(file.name)
        commands[canonicalCommandName(name)] = SpecScriptFile(file)
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
        return localFileCommands[canonicalCommandName(command)]
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
    for ((name, type) in info.types.properties()) {
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
    return commandName.removeSpecScriptExtension().replace('-', ' ')
}

/**
 * Creates cli command name stripping extension, converting spaces to dashes and making it all lowercase
 */
fun asCliCommand(commandName: String): String {
    return commandName.removeSpecScriptExtension().replace(' ', '-').lowercase(Locale.getDefault())
}

fun String.removeSpecScriptExtension(): String {
    return removeSuffix(YAML_SPEC_EXTENSION).removeSuffix(MARKDOWN_SPEC_EXTENSION)
}

fun Path.deleteOnShutdown() {
    Runtime.getRuntime().addShutdownHook(Thread {
        toFile().deleteRecursively()
    })
}