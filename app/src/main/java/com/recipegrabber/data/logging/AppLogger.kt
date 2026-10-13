package com.recipegrabber.data.logging

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
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
    private val store by lazy {
        val legacyExport = File(context.cacheDir, "recipegrabber-logs.txt")
        if (legacyExport.isFile) legacyExport.writeText("Legacy diagnostic export removed during privacy upgrade.\n")
        PrivateLogStore(File(context.filesDir, "logs"), maxExportBytes = 255 * 1024)
    }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.GERMANY)
    private val buffer = ConcurrentLinkedQueue<String>()
    private val isWriting = AtomicBoolean(false)
    private val loggerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fileMutex = filesMutex

    init {
        loggerScope.launch {
            try {
                fileMutex.withLock { store.export() }
            } catch (_: Exception) {
                android.util.Log.w("RG-App", "Diagnostic storage unavailable")
            }
        }
    }

    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String) = log(LogLevel.WARN, tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val failure = throwable?.let { " (${it.javaClass.simpleName})" }.orEmpty()
        log(LogLevel.ERROR, tag, message + failure)
    }

    private fun log(level: String, tag: String, message: String) {
        val timestamp = format(dateFormat, Date())
        val safeTag = DiagnosticSanitizer.safeTag(tag)
        val safe = DiagnosticSanitizer.sanitize(message)
        val logLine = "$timestamp [$level] $safeTag: $safe"
        android.util.Log.println(androidPriority(level), "RG-$safeTag", safe)
        synchronized(buffer) {
            if (buffer.size >= 256) buffer.poll()
            buffer.add(logLine)
        }
        flushIfNeeded()
    }

    private fun flushIfNeeded() {
        if (isWriting.compareAndSet(false, true)) {
            loggerScope.launch {
                try {
                    fileMutex.withLock {
                        flushBufferLocked()
                    }
                } catch (_: Exception) {
                    buffer.clear()
                    android.util.Log.w("RG-App", "Diagnostic storage unavailable")
                } finally {
                    isWriting.set(false)
                    if (buffer.isNotEmpty()) {
                        flushIfNeeded()
                    }
                }
            }
        }
    }

    private fun flushBufferLocked() {
        repeat(256) {
            val line = buffer.poll() ?: return
            store.append(line)
        }
    }

    suspend fun getLogContent(): String = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            flushBufferLocked()
            val header = "=== RecipeGrabber Diagnostics ===\nAndroid SDK: ${android.os.Build.VERSION.SDK_INT}\nApp: ${getAppVersion()}\n\n"
            header + store.export()
        }
    }

    suspend fun clearLogs(): Boolean = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            buffer.clear()
            store.clear()
        }
        true
    }

    suspend fun createShareIntent(): Intent = withContext(Dispatchers.IO) {
        val logContent = getLogContent()
        val directory = File(context.cacheDir, "diagnostics")
        check(directory.isDirectory || directory.mkdirs()) { "Diagnostic export unavailable" }
        val file = File(directory, "recipegrabber-logs.txt")
        file.writeText(logContent)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "RecipeGrabber Logs")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun getAppVersion(): String {
        return try {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            DiagnosticSanitizer.sanitize("${pi.versionName} (${pi.longVersionCode})").take(64)
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun format(formatter: SimpleDateFormat, date: Date): String {
        return synchronized(formatter) {
            formatter.format(date)
        }
    }

    private fun androidPriority(level: String): Int {
        return when (level) {
            LogLevel.DEBUG -> android.util.Log.DEBUG
            LogLevel.INFO -> android.util.Log.INFO
            LogLevel.WARN -> android.util.Log.WARN
            LogLevel.ERROR -> android.util.Log.ERROR
            else -> android.util.Log.DEBUG
        }
    }

    companion object {
        private val filesMutex = Mutex()
        @Volatile
        private var INSTANCE: AppLogger? = null

        fun initialize(context: Context): AppLogger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppLogger(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
