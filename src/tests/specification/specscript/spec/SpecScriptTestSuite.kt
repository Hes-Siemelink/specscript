package specscript.spec

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.TestFactory
import specscript.commands.http.HttpServer
import specscript.files.SpecScriptFile
import specscript.test.getCodeExamplesAsTests
import specscript.test.getTests
import java.nio.file.Path

object TestPaths {
    val README: Path = Path.of("README.md")
    val SPEC: Path = Path.of("specification")
    val SAMPLE_SERVER: Path = Path.of("specification/code-examples/sample-server/start.spec.yaml")
}

class SpecScriptTestSuite {

    @TestFactory
    fun `Main README_md`(): List<DynamicNode> {
        return SpecScriptFile(TestPaths.README).getCodeExamplesAsTests() + SpecScriptFile(Path.of("README-old.md")).getCodeExamplesAsTests()
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
            HttpServer.stop("sample-server")
        }
    }
}