package specscript.files

import specscript.language.canonicalCommandName
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

typealias PackageName = String

object PackageRegistry {

    var packagePath: Path? = null
    var autoPackagePath: Path? = null

    fun findPackage(name: PackageName): Path? {
        for (libDir in searchPath()) {
            val candidate = libDir.resolve(name)
            if (candidate.isDirectory() && isPackage(candidate, name)) {
                return candidate
            }
        }
        return null
    }

    fun findEnclosingPackageLibrary(startDir: Path): Path? {
        var dir: Path? = startDir.toAbsolutePath().normalize()
        while (dir != null) {
            val info = SpecScriptDirectories.get(dir)
            if (info.packageInfo != null) {
                return dir.parent
            }
            dir = dir.parent
        }
        return null
    }

    private fun searchPath(): List<Path> {
        val paths = mutableListOf<Path>()

        autoPackagePath?.let { paths.add(it) }

        packagePath?.let { paths.add(it) }

        System.getenv("SPECSCRIPT_PACKAGE_PATH")?.let { envPath ->
            envPath.split(":").filter { it.isNotBlank() }.forEach {
                paths.add(Path.of(it))
            }
        }

        val defaultDir = Path.of(System.getProperty("user.home"), ".specscript", "packages")
        if (defaultDir.exists()) {
            paths.add(defaultDir)
        }

        return paths
    }

    private fun isPackage(dir: Path, expectedName: PackageName): Boolean {
        val info = SpecScriptDirectories.get(dir)
        return info.packageInfo != null
    }

    fun scanCommands(
        packageDir: Path,
        items: List<ImportItem>
    ): Map<String, SpecScriptFile> {
        val commands = mutableMapOf<String, SpecScriptFile>()

        for (item in items) {
            when (item) {
                is ImportItem.Command -> {
                    val file = packageDir.resolve("${item.path}$YAML_SPEC_EXTENSION")
                    if (file.exists()) {
                        val name = item.alias
                            ?: canonicalCommandName(asScriptCommand(file.name))
                        commands[canonicalCommandName(name)] = SpecScriptFile(file)
                    }
                }

                is ImportItem.Name -> {
                    addNameImport(commands, packageDir, item.value, item.alias)
                }

                is ImportItem.Directory -> {
                    val dir = packageDir.resolve(item.path)
                    if (dir.isDirectory()) {
                        addDirectoryCommands(commands, dir)
                    }
                }

                is ImportItem.Wildcard -> {
                    val dir = if (item.path.isEmpty()) packageDir
                    else packageDir.resolve(item.path)

                    if (dir.isDirectory()) {
                        if (item.recursive) {
                            addRecursiveCommands(commands, dir)
                        }

                        else {
                            addDirectoryCommands(commands, dir)
                        }
                    }
                }

            }
        }

        return commands
    }

    fun scanLocalCommands(
        configDir: Path,
        localPath: String,
        items: List<ImportItem>
    ): Map<String, SpecScriptFile> {
        val resolvedPath = localPath.removePrefix("./")
        val dir = configDir.resolve(resolvedPath)
        val commands = mutableMapOf<String, SpecScriptFile>()

        for (item in items) {
            when (item) {
                is ImportItem.Command -> {
                    val file = dir.resolve("${item.name}$YAML_SPEC_EXTENSION")
                    if (file.exists()) {
                        val name = item.alias
                            ?: canonicalCommandName(asScriptCommand(file.name))
                        commands[canonicalCommandName(name)] = SpecScriptFile(file)
                    }
                }

                is ImportItem.Name -> {
                    addNameImport(commands, dir, item.value, item.alias)
                }

                is ImportItem.Wildcard -> {
                    if (item.recursive) {
                        addRecursiveCommands(commands, dir)
                    }

                    else {
                        addDirectoryCommands(commands, dir)
                    }
                }

                is ImportItem.Directory -> {
                    val subDir = dir.resolve(item.path)
                    if (subDir.isDirectory()) {
                        addDirectoryCommands(commands, subDir)
                    }
                }

            }
        }

        return commands
    }

    private fun addNameImport(
        commands: MutableMap<String, SpecScriptFile>,
        baseDir: Path,
        name: String,
        alias: String? = null
    ) {
        val asFile = baseDir.resolve("$name$YAML_SPEC_EXTENSION")
        if (asFile.exists()) {
            val commandName = alias
                ?: canonicalCommandName(asScriptCommand(asFile.name))
            commands[canonicalCommandName(commandName)] = SpecScriptFile(asFile)
            return
        }

        val asDir = baseDir.resolve(name)
        if (asDir.isDirectory()) {
            addDirectoryCommands(commands, asDir)
        }
    }

    private fun addDirectoryCommands(
        commands: MutableMap<String, SpecScriptFile>,
        dir: Path
    ) {
        if (!dir.isDirectory()) return

        Files.list(dir).use { stream ->
            stream.filter { it.name.endsWith(YAML_SPEC_EXTENSION) || it.name.endsWith(MARKDOWN_SPEC_EXTENSION) }
                .filter { !it.isDirectory() }
                .forEach { file ->
                    val commandName = asScriptCommand(file.name)
                    commands[canonicalCommandName(commandName)] = SpecScriptFile(file)
                }
        }
    }

    private fun addRecursiveCommands(commands: MutableMap<String, SpecScriptFile>, dir: Path) {
        if (!dir.isDirectory()) return

        addDirectoryCommands(commands, dir)

        Files.list(dir).use { stream ->
            stream.filter { it.isDirectory() && !isExcluded(it) }
                .forEach { subDir ->
                    addRecursiveCommands(commands, subDir)
                }
        }
    }

    private fun isExcluded(dir: Path): Boolean {
        if (dir.name == "tests") return true

        val info = SpecScriptDirectories.get(dir)
        return info.hidden
    }
}
