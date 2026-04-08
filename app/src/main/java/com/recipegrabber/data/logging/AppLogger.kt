package com.recipegrabber.data.logging

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

object LogLevel {
    const val DEBUG = "DEBUG"
    const val INFO = "INFO"
    const val WARN = "WARN"
    const val ERROR = "ERROR"
}

@Singleton
class AppLogger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val logDir by lazy { File(context.filesDir, "logs").also { it.mkdirs() } }
    private val currentLogFile by lazy { File(logDir, "app.log") }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.GERMANY)
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY)
    private val buffer = ConcurrentLinkedQueue<String>()
    private val isWriting = AtomicBoolean(false)
    private val maxLogSize = 5 * 1024 * 1024L

    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String) = log(LogLevel.WARN, tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val stackTrace = throwable?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            "\n${sw}"
        } ?: ""
        log(LogLevel.ERROR, tag, "$message$stackTrace")
    }

    private fun log(level: String, tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val threadName = Thread.currentThread().name
        val logLine = "$timestamp [$level] [$threadName] $tag: $message"
        android.util.Log.d("RG-$tag", message)
        buffer.add(logLine)
        flushIfNeeded()
    }

    private fun flushIfNeeded() {
        if (isWriting.compareAndSet(false, true)) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    flushBuffer()
                    rotateIfNeeded()
                } finally {
                    isWriting.set(false)
                }
            }
        }
    }

    private fun flushBuffer() {
        val writer = FileWriter(currentLogFile, true)
        writer.use { w ->
            while (buffer.isNotEmpty()) {
                val line = buffer.poll() ?: break
                w.write(line)
                w.write("\n")
            }
        }
    }

    private fun rotateIfNeeded() {
        if (currentLogFile.length() > maxLogSize) {
            val date = fileDateFormat.format(Date())
            val rotated = File(logDir, "app-$date.log")
            if (rotated.exists()) rotated.delete()
            currentLogFile.renameTo(rotated)
        }
    }

    suspend fun getLogContent(): String = withContext(Dispatchers.IO) {
        flushBuffer()
        val sb = StringBuilder()
        sb.appendLine("=== RecipeGrabber Log Export ===")
        sb.appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        sb.appendLine("Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
        sb.appendLine("App Version: ${getAppVersion()}")
        sb.appendLine("Exported: ${dateFormat.format(Date())}")
        sb.appendLine("================================\n")

        val logFiles = logDir.listFiles()
            ?.filter { it.name.endsWith(".log") }
            ?.sortedBy { it.name }
            ?: emptyList()

        for (file in logFiles) {
            sb.appendLine("--- ${file.name} ---")
            sb.appendLine(file.readText())
            sb.appendLine()
        }

        sb.toString()
    }

    suspend fun clearLogs(): Boolean = withContext(Dispatchers.IO) {
        logDir.listFiles()?.forEach { it.delete() }
        buffer.clear()
        true
    }

    fun createShareIntent(): Intent {
        val logContent = kotlinx.coroutines.runBlocking { getLogContent() }
        val file = File(context.cacheDir, "recipegrabber-logs.txt")
        file.writeText(logContent)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "RecipeGrabber Logs")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun getAppVersion(): String {
        return try {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pi.versionName} (${pi.longVersionCode})"
        } catch (e: Exception) {
            "unknown"
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppLogger? = null

        fun initialize(context: Context): AppLogger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppLogger(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}