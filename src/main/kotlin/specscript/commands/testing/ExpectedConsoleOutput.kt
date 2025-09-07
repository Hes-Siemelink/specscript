package specscript.commands.testing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ValueNode
import specscript.language.CommandHandler
import specscript.language.InstacliCommandError
import specscript.language.ScriptContext
import specscript.language.ValueHandler
import specscript.util.IO.rewireSystemErr
import specscript.util.IO.rewireSystemOut
import specscript.util.Json
import specscript.util.toDisplayYaml
import java.io.ByteArrayOutputStream
import java.io.PrintStream

object ExpectedConsoleOutput : CommandHandler("Expected console output", "core/testing"), ValueHandler {

    private const val OUT = "stdout"
    private const val ERR = "stderr"

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {

        val output = context.session[OUT]?.toString() ?: "<no output>"

        if (output.trim() != data.toDisplayYaml().trim()) {
            val error = Json.newObject()
            error.put("expected", data.toDisplayYaml().trim())
            error.put("actual", output.trim())
            throw InstacliCommandError("Output", "Unexpected console output.", error)
        }

        return null
    }

    fun storeOutput(context: ScriptContext, stdout: String, stderr: String): String {
        val out = stdout.trim()
        val err = stderr.trim()
        val combined = buildString {
            if (out.isEmpty()) {
                append(err)
            } else {
                append(out)
                if (err.isNotEmpty()) {
                    append("\n")
                    append(err)
                }
            }
        }

        // Combine stdout and stderr into a new stream
        val stream = ByteArrayOutputStream()
        PrintStream(stream).print(combined)

        context.session[OUT] = stream

        return combined
    }

    fun <T> captureSystemOutAndErr(context: ScriptContext, doThis: () -> T): T {

        val (originalOut, capturedOut) = rewireSystemOut()
        val (originalErr, capturedErr) = rewireSystemErr()

        context.session[OUT] = capturedOut
        context.session[ERR] = capturedErr

        try {
            val result = doThis()

            return result
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
            context.session.remove(OUT)
            context.session.remove(ERR)
        }
    }

    fun reset(context: ScriptContext) {
        (context.session[OUT] as? ByteArrayOutputStream)?.reset()
        (context.session[ERR] as? ByteArrayOutputStream)?.reset()
    }

}

