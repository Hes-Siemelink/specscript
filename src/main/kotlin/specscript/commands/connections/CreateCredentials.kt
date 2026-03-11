package specscript.commands.connections

import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.util.toDomainObject
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

object CreateCredentials : CommandHandler("Create credentials", "core/connections"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode {

        val newCredentials = data.toDomainObject(CreateCredentialsInfo::class)
        val credentials = context.getCredentials()
        val target = credentials.targetResources.getOrPut(newCredentials.target) {
            TargetResource()
        }

        target.credentials.add(newCredentials.credentials)

        credentials.save()

        return newCredentials.credentials
    }
}