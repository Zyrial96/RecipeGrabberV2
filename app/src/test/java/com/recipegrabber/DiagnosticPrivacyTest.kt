package com.recipegrabber

import com.recipegrabber.data.logging.DiagnosticSanitizer
import com.recipegrabber.data.logging.PrivateLogStore
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DiagnosticPrivacyTest {
    @TempDir lateinit var directory: File

    @Test fun `known secrets and URLs are sanitized before truncation`() {
        val message = "Authorization: Bearer synthetic-token; email test@example.org https://example.org/private?token=synthetic sk-synthetic012345 api_key=synthetic-key password=synthetic-password"
        val safe = DiagnosticSanitizer.sanitize(message)
        for (privateValue in listOf("synthetic-token", "test@example.org", "https://", "sk-synthetic012345", "synthetic-key", "synthetic-password")) {
            assertFalse(safe.contains(privateValue), privateValue)
        }
        assertFalse(DiagnosticSanitizer.sanitize("api_key=" + "synthetic".repeat(2000)).contains("synthetic"))
    }

    @Test fun `raw response and multiline log injection are suppressed`() {
        assertEquals("Provider response omitted", DiagnosticSanitizer.sanitize("Raw response: arbitrary private recipe content"))
        assertFalse(DiagnosticSanitizer.sanitize("Safe event\nFORGED ENTRY\r\u0000").contains('\n'))
        assertEquals("App", DiagnosticSanitizer.safeTag("https://private.example"))
    }

    @Test fun `legacy unknown content is removed once without losing new diagnostics`() {
        File(directory, "app.log").writeText("unlabelled synthetic private value")
        val store = PrivateLogStore(directory)
        assertFalse(store.export().contains("synthetic private"))
        store.append("New safe diagnostic")
        assertTrue(PrivateLogStore(directory).export().contains("New safe diagnostic"))
    }

    @Test fun `file retention and export are bounded`() {
        val store = PrivateLogStore(directory, maxFileBytes = 256, maxFiles = 3, maxExportBytes = 128)
        repeat(100) { store.append("Safe diagnostic event $it " + "x".repeat(40)) }
        val logs = directory.listFiles()!!.filter { it.name.endsWith(".log") }
        assertTrue(logs.size <= 3)
        assertTrue(logs.all { it.length() <= 256 })
        val export = store.export()
        assertTrue(export.toByteArray().size <= 128)
        assertTrue(export.contains("Safe diagnostic"))
    }

    @Test fun `clear only touches owned diagnostic files`() {
        val unrelated = File(directory, "notes.txt").apply { writeText("Keep this") }
        val store = PrivateLogStore(directory)
        store.append("Safe event")
        store.clear()
        assertEquals("Keep this", unrelated.readText())
        assertEquals("", store.export())
    }
}
