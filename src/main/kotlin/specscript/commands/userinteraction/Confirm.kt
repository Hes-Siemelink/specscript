package specscript.commands.userinteraction

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.databind.node.ValueNode
import specscript.language.CommandHandler
import specscript.language.InstacliCommandError
import specscript.language.ScriptContext
import specscript.language.ValueHandler
import specscript.language.types.ParameterData
import specscript.util.toDisplayYaml

/**
 * Asks user to confirm a question
 */
object Confirm : CommandHandler("Confirm", "core/user-interaction"), ValueHandler {

    val yes = TextNode("Yes")
    val no = TextNode("No")

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode {

        val question = data.toDisplayYaml()

        val confirmationDialog = ParameterData(
            description = question,
            enum = listOf(yes, no)
        )

        val answer = confirmationDialog.prompt()

        if (answer == no) {
            throw InstacliCommandError("No confirmation -- action canceled.")
        }

        return answer
    }
}