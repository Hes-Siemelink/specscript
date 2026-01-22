package specscript.util

import com.networknt.schema.Schema
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.dialect.Dialects
import com.networknt.schema.resource.SchemaIdResolvers
import specscript.language.CommandFormatException
import tools.jackson.databind.JsonNode
import java.nio.file.Path
import java.util.function.Consumer

object JsonSchemas {

    private val schemas = mutableMapOf<String, Schema?>()

    var registry: SchemaRegistry = SchemaRegistry.withDialect(
        Dialects.getDraft202012(),
        Consumer { builder: SchemaRegistry.Builder? ->
            builder!!.schemaIdResolvers(Consumer { schemaIdResolvers: SchemaIdResolvers.Builder? ->
                schemaIdResolvers!!
                    .mapPrefix("https://specscript.info/v1/commands", "classpath:commands")
            })
        })

//    val factory: SchemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012(),
//        builder -> builder.schemaIdResolvers(schemaIdResolvers -> schemaIdResolvers
//    .mapPrefix("https://spec.openapis.org/oas/3.1", "classpath:oas/3.1")));
//    {
//        it.schemaMappers { schemaMappers ->
//            schemaMappers.mapPrefix("https://specscript.info/v1/commands", "classpath:commands")
//        }
//    }

    fun getSchema(schemaName: String): Schema? {
        return schemas.getOrPut(schemaName) {
            loadSchema(schemaName)
        }
    }

    private fun loadSchema(schemaName: String): Schema? {
        val schemaFile = when {
            Resources.exists("commands/$schemaName.yaml") -> "commands/$schemaName.yaml"
            Resources.exists("commands/$schemaName.json") -> "commands/$schemaName.json"
            else -> return null
        }
        return registry.getSchema(SchemaLocation.of(Resources.getUri(schemaFile).toString()))
    }

    fun load(schemaFile: Path): Schema? {
        return registry.getSchema(SchemaLocation.of(schemaFile.toUri().toString()))
    }
}


internal fun JsonNode.validateWithSchema(schemaName: String) {
    val schema = JsonSchemas.getSchema(schemaName) ?: return
    val messages = schema.validate(this)
    if (messages.isNotEmpty()) {
        throw CommandFormatException("Schema validation errors according to \"${schemaName}\":\n$messages")
    }
}

