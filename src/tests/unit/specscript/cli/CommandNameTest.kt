package specscript.cli

import specscript.files.asCliCommand
import specscript.files.asScriptCommand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CommandNameTest {

    @Test
    fun toScriptCommand() {
        assertEquals("Command", asScriptCommand("command"))
        assertEquals("Command", asScriptCommand("command.spec.yaml"))
        assertEquals("Command with words", asScriptCommand("command-with-words.spec.yaml"))
        assertEquals("Command with words", asScriptCommand("Command with words"))
        assertEquals("Command with MIXED Case", asScriptCommand("Command with MIXED Case"))
    }

    @Test
    fun toCliCommand() {
        assertEquals("command", asCliCommand("command"))
        assertEquals("command", asCliCommand("command.spec.yaml"))
        assertEquals("command-with-words", asCliCommand("command-with-words.spec.yaml"))
        assertEquals("command-with-words", asCliCommand("Command with words"))
        assertEquals("command-with-mixed-case", asCliCommand("Command with MIXED Case"))
    }
}