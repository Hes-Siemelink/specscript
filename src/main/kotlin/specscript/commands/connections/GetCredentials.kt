package specscript.commands.connections

import specscript.language.*
import specscript.util.Json.newObject
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.StringNode
import tools.jackson.databind.node.ValueNode

object GetCredentials : CommandHandler("Get credentials", "core/connections"), ValueHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {

        val targetName = data.asString() ?: throw CommandFormatException("Specify target resource")
        val credentials = context.getCredentials()
        val target = credentials.targetResources[targetName] ?: return StringNode("")

        return when {
            target.default != null -> {
                target.default()
            }

            target.credentials.isNotEmpty() -> {
                target.credentials.first()
            }

            else -> throw SpecScriptCommandError(
                "No accounts defined for $targetName",
                type = "no accounts",
                data = newObject("target", targetName)
            )
        }
    }
}