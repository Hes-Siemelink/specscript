package specscript.util

import com.networknt.schema.InputFormat
import com.networknt.schema.Schema
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.dialect.Dialects
import specscript.language.CommandFormatException
import tools.jackson.databind.JsonNode
import java.nio.file.Path
import kotlin.io.path.inputStream

object JsonSchemas {

    private val schemas = mutableMapOf<String, Schema?>()

    var registry: SchemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012()) { builder ->
        builder.schemaIdResolvers { resolvers ->
            resolvers.mapPrefix("https://specscript.info/v1/commands", "classpath:commands")
        }
    }

    fun getSchema(schemaName: String): Schema? {
        return schemas.getOrPut(schemaName) {
            loadSchema(schemaName)
        }
    }

    private fun loadSchema(schemaName: String): Schema? {
        val schemaFile = when {
            Resources.exists("commands/$schemaName.yaml") -> "classpath:commands/$schemaName.yaml"
            Resources.exists("commands/$schemaName.json") -> "classpath:commands/$schemaName.json"
            else -> return null
        }
        return registry.getSchema(SchemaLocation.of(schemaFile))
    }

    fun load(schemaFile: Path): Schema? {
        return registry.getSchema(schemaFile.inputStream(), InputFormat.YAML)
    }
}


internal fun JsonNode.validateWithSchema(schemaName: String) {
    val schema = JsonSchemas.getSchema(schemaName) ?: return
    val messages = schema.validate(this)
    if (messages.isNotEmpty()) {
        throw CommandFormatException("Schema validation errors according to \"${schemaName}\":\n$messages")
    }
}

