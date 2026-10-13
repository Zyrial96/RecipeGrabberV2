package com.recipegrabber

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.recipegrabber.data.logging.AppLogger
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.UUID

class DiagnosticPrivacyDeviceTest {
    private val app = ApplicationProvider.getApplicationContext<Context>()
    private val root = File(app.cacheDir, "diagnostics/qa-${UUID.randomUUID()}").also { it.mkdirs() }
    private val context = object : ContextWrapper(app) {
        override fun getFilesDir() = File(root, "files").also { it.mkdirs() }
        override fun getCacheDir() = File(root, "cache").also { it.mkdirs() }
    }

    @After fun cleanup() { root.deleteRecursively() }

    @Test fun legacyAndNewDiagnosticsExcludeSensitiveValues() = runBlocking {
        File(context.filesDir, "logs").mkdirs()
        File(context.filesDir, "logs/app.log").writeText("unlabelled synthetic legacy secret")
        val legacyExport = File(context.cacheDir, "recipegrabber-logs.txt").apply { writeText("synthetic cached secret") }
        val logger = AppLogger(context)
        logger.i("QA", "https://example.org/private test@example.org Bearer synthetic-token")
        logger.e("QA", "Safe failure", IOException("arbitrary synthetic exception secret"))
        val export = logger.getLogContent()
        for (value in listOf("synthetic legacy", "https://example.org", "test@example.org", "synthetic-token", "synthetic exception")) {
            assertFalse(export.contains(value))
        }
        assertTrue(export.contains("Safe failure"))
        assertTrue(export.contains("IOException"))
        assertTrue(export.toByteArray().size <= 256 * 1024)
        assertFalse(legacyExport.readText().contains("synthetic cached secret"))
    }

    @Test fun sharingBuildsAnExplicitReadOnlyContentUri() = runBlocking {
        val logger = AppLogger(context)
        logger.i("QA", "Safe sharing fixture")
        val intent = logger.createShareIntent()
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        val uri = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("content", uri.scheme)
        val content = app.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
        assertTrue(content.contains("Safe sharing fixture"))
    }
}
