package specscript.files

import com.fasterxml.jackson.annotation.JsonProperty
import specscript.language.CommandInfo
import specscript.util.IO.isTempDir
import specscript.util.Json
import specscript.util.Yaml
import tools.jackson.databind.JsonNode
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.name

class DirectoryInfo : CommandInfo {

    var dir: Path = Path.of(".")

    override var hidden: Boolean = false
    override var name: String = ""

    @JsonProperty("Script info")
    override var description: String = ""

    @JsonProperty("specscript-version")
    override var specScriptVersion: String = "unknown"

    var imports = mutableListOf<String>()
    var connections = Json.newObject()

    var types = Json.newObject()

    companion object {

        const val FILE_NAME = "specscript-config.yaml"

        fun load(dir: Path): DirectoryInfo {
            val configFile = dir.resolve(FILE_NAME)

            val info = if (configFile.exists()) {
                Yaml.readTyped(configFile.toFile())
            } else {
                DirectoryInfo()
            }

            // Pull description from README.md if not set
            if (info.description.isEmpty()) {
                val readme = dir.resolve("README.md")
                info.description = getDescriptionFromMarkdown(readme)
            }

            info.dir = dir

            if (info.name.isEmpty()) {
                info.name = dir.name
            }

            val typesFile = dir.resolve("types.yaml")
            if (typesFile.exists()) {
                info.types = Yaml.readTyped(typesFile.toFile())
            }

            return info
        }

        private fun getDescriptionFromMarkdown(readme: Path): String {
            return if (readme.exists()) {
                val doc = SpecScriptMarkdown.scan(readme)
                doc.description ?: ""
            } else {
                ""
            }
        }
    }
}

object SpecScriptDirectories {
    val directories = mutableMapOf<Path, DirectoryInfo>()

    fun get(dir: Path): DirectoryInfo {
        return if (dir.isTempDir()) {
            // Do not cache directories that are made on the fly by scripts
            DirectoryInfo.load(dir)
        } else {
            val key = dir.toAbsolutePath().normalize()
            directories.getOrPut(key) {
                DirectoryInfo.load(dir)
            }
        }
    }

    /**
     * Search for a connection by name, starting from [startDir] and walking up parent directories.
     * Returns the connection definition and the directory it was found in, or null if not found.
     */
    fun findConnection(name: String, startDir: Path): ConnectionMatch? {
        var dir: Path? = startDir.toAbsolutePath().normalize()
        while (dir != null) {
            val info = get(dir)
            val connection = info.connections[name]
            if (connection != null) {
                return ConnectionMatch(connection, dir)
            }
            dir = dir.parent
        }
        return null
    }
}

data class ConnectionMatch(val definition: JsonNode, val configDir: Path)