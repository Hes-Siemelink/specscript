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

    fun getCommandHandler(command: String): CommandHandler
    fun clone(): ScriptContext
}

fun ScriptContext.getInputVariables(): ObjectNode {
    return variables.getOrPut(INPUT_VARIABLE) { Json.newObject() } as ObjectNode
}

const val INPUT_VARIABLE = "input"
const val OUTPUT_VARIABLE = "output"
const val SCRIPT_TEMP_DIR_VARIABLE = "SCRIPT_TEMP_DIR"
