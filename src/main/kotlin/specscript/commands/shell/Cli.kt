package specscript.commands.shell

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.databind.node.ValueNode
import specscript.cli.InstacliMain
import specscript.commands.testing.ExpectedConsoleOutput
import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.language.ValueHandler
import specscript.util.IO
import specscript.util.toDomainObject
import java.nio.file.Path

object Cli : CommandHandler("Cli", "core/shell"), ValueHandler, ObjectHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {

        return runCli(context, data.asText())
    }

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val info = data.toDomainObject(CliData::class)

        return runCli(context, info.command, info.cd?.let { Path.of(it) } ?: context.tempDir)
    }

    private fun runCli(context: ScriptContext, command: String, workingDir: Path = context.tempDir): TextNode {
        val line = command.split("\\s+".toRegex())
        val args = line.drop(1).toTypedArray()

        // Capture console output
        val (stdout, stderr) = IO.captureSystemOutAndErr {
            InstacliMain.main(args, workingDir = workingDir)
        }

        // Make sure we can check the output
        val combined = ExpectedConsoleOutput.storeOutput(context, stdout, stderr)

        return TextNode(combined)
    }
}

data class CliData(
    val command: String,
    val cd: String? = null,
)