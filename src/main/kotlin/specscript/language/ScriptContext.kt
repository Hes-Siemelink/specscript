package specscript.language

import specscript.language.types.TypeRegistry
import specscript.util.Json
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import java.nio.file.Path

interface ScriptContext {
    val interactive: Boolean
    val variables: MutableMap<String, JsonNode>
    val session: MutableMap<String, Any?>
    val scriptFile: Path
    val scriptDir: Path
    val workingDir: Path
    val tempDir: Path
    val output: JsonNode?
    var error: SpecScriptCommandError?
    val types: TypeRegistry

    /**
     * The directory where the original script file lives. For normal scripts, this equals scriptDir.
     * For Markdown test execution where scriptDir points to a temp dir, this points to the real
     * spec file's parent directory.
     */
    val scriptHome: Path
        get() = scriptDir

    fun getCommandHandler(command: String): CommandHandler
    fun clone(): ScriptContext
}

fun ScriptContext.getInputVariables(): ObjectNode {
    return variables.getOrPut(INPUT_VARIABLE) { Json.newObject() } as ObjectNode
}

const val INPUT_VARIABLE = "input"
const val OUTPUT_VARIABLE = "output"
const val ENV_VARIABLE = "env"
const val SCRIPT_DIR_VARIABLE = "SCRIPT_HOME"
const val SCRIPT_TEMP_DIR_VARIABLE = "SCRIPT_TEMP_DIR"
const val PWD_VARIABLE = "PWD"
