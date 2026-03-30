package specscript.commands.userinteraction

import specscript.commands.testing.getAnswers
import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.language.SpecScriptCommandError
import specscript.language.ValueHandler
import specscript.language.types.ParameterData
import specscript.util.toDisplayYaml
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.StringNode
import tools.jackson.databind.node.ValueNode

/**
 * Asks user to confirm a question
 */
object Confirm : CommandHandler("Confirm", "core/user-interaction"), ValueHandler {

    val yes = StringNode("Yes")
    val no = StringNode("No")

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode {

        val question = data.toDisplayYaml()

        val confirmationDialog = ParameterData(
            description = question,
            enum = listOf(yes, no)
        )

        val answer = confirmationDialog.prompt(answers = context.getAnswers(), interactive = context.interactive)

        if (answer == no) {
            throw SpecScriptCommandError("No confirmation -- action canceled.")
        }

        return answer
    }
}
