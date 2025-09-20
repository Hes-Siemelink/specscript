package specscript.spec

import org.junit.jupiter.api.*
import specscript.commands.http.HttpServer
import specscript.commands.userinteraction.TestPrompt
import specscript.commands.userinteraction.UserPrompt
import specscript.files.SpecScriptFile
import java.nio.file.Path

class SpecScriptTestSuite {

    @BeforeEach
    fun setup() {
        UserPrompt.default = TestPrompt
    }

    @TestFactory
    fun `Main README_md`(): List<DynamicNode> {
        return SpecScriptFile(TestPaths.README).getCodeExamples() + SpecScriptFile(Path.of("README-2.md")).getCodeExamples()
    }

    @TestFactory
    fun specification(): List<DynamicNode> {
        return TestPaths.SPEC.getTests()
    }

    companion object {

        @BeforeAll
        @JvmStatic
        fun startTestServer() {
            SpecScriptFile(TestPaths.SAMPLE_SERVER).run()
        }

        @AfterAll
        @JvmStatic
        fun stopTestServer() {
            HttpServer.stop(2525)
        }
    }
}