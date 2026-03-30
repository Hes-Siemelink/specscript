package specscript.util

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.nio.file.Path
import java.nio.file.Paths

object IO {
    fun captureSystemOut(echo: Boolean = true, doThis: () -> Unit): String {

        val original = System.out
        val captured = ByteArrayOutputStream()
        System.setOut(if (echo) dualStream(original, captured) else PrintStream(captured))

        try {
            doThis()

            captured.flush()
            return captured.toString()
        } finally {
            System.setOut(original)
        }
    }

    fun captureSystemOutAndErr(echo: Boolean = true, doThis: () -> Unit): Pair<String, String> {

        val originalOut = System.out
        val originalErr = System.err
        val capturedOut = ByteArrayOutputStream()
        val capturedErr = ByteArrayOutputStream()
        System.setOut(if (echo) dualStream(originalOut, capturedOut) else PrintStream(capturedOut))
        System.setErr(if (echo) dualStream(originalErr, capturedErr) else PrintStream(capturedErr))

        try {
            doThis()

            capturedOut.flush()
            capturedErr.flush()
            return Pair(capturedOut.toString(), capturedErr.toString())
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
        }
    }

    fun rewireSystemOut(): Pair<PrintStream, ByteArrayOutputStream> {
        val original = System.out
        val copy = ByteArrayOutputStream()
        System.setOut(dualStream(original, copy))
        return Pair(original, copy)
    }

    fun rewireSystemErr(): Pair<PrintStream, ByteArrayOutputStream> {
        val original = System.err
        val copy = ByteArrayOutputStream()
        System.setErr(dualStream(original, copy))
        return Pair(original, copy)
    }

    private fun dualStream(
        original: PrintStream,
        byteArrayOutputStream: ByteArrayOutputStream
    ): PrintStream {
        val customOut = PrintStream(object : OutputStream() {
            override fun write(b: Int) {
                original.write(b) // Write to the console
                byteArrayOutputStream.write(b) // Write to the ByteArrayOutputStream
            }
        })
        return customOut
    }

    val TEMP_DIR: Path = Paths.get(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize()

    fun Path.isTempDir(): Boolean {
        return toAbsolutePath().normalize().startsWith(TEMP_DIR)
    }
}


