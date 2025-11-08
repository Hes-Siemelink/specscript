package specscript.spec

import org.junit.jupiter.api.*
import specscript.commands.http.HttpServer
import specscript.commands.userinteraction.TestPrompt
import specscript.commands.userinteraction.UserPrompt
import specscript.files.SpecScriptFile
import specscript.test.getCodeExamplesAsTests
import specscript.test.getTests
import java.nio.file.Path

object TestPaths {
    val README: Path = Path.of("README.md")
    val SPEC: Path = Path.of("specification")
    val SAMPLE_SERVER: Path = Path.of("samples/http-server/sample-server/sample-server.spec.yaml")
}

class SpecScriptTestSuite {

    @BeforeEach
    fun setup() {
        UserPrompt.default = TestPrompt
    }

    @TestFactory
    fun `Main README_md`(): List<DynamicNode> {
        return SpecScriptFile(TestPaths.README).getCodeExamplesAsTests() + SpecScriptFile(Path.of("README-2.md")).getCodeExamplesAsTests()
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