package com.recipegrabber.data.logging

import java.io.File

internal class PrivateLogStore(
    private val directory: File,
    private val maxFileBytes: Int = 256 * 1024,
    private val maxFiles: Int = 4,
    private val maxExportBytes: Int = 256 * 1024
) {
    private val current get() = File(directory, "app.log")
    private var ready = false

    init { require(maxFileBytes >= 64 && maxFiles >= 1 && maxExportBytes >= 64) }

    private fun files() = directory.listFiles().orEmpty()
        .filter { it.isFile && it.name.matches(Regex("app(?:-[0-9-]+)?\\.log")) }

    private fun prepare() {
        if (ready) return
        check(directory.isDirectory || directory.mkdirs()) { "Log directory unavailable" }
        val marker = File(directory, "privacy-v1")
        if (!marker.exists()) {
            // Unknown old payloads cannot be safely recovered by pattern matching.
            files().forEach { it.writeText("Legacy diagnostic content removed during privacy upgrade.\n") }
            marker.writeText("1")
        }
        prune()
        ready = true
    }

    fun append(line: String) {
        prepare()
        val safe = (DiagnosticSanitizer.sanitize(line) + "\n").toByteArray(Charsets.UTF_8)
        val bytes = safe.take(maxFileBytes).toByteArray()
        if (current.exists() && current.length() + bytes.size > maxFileBytes) {
            val archive = File(directory, "app-${System.nanoTime().toString().replace('-', '0')}.log")
            check(current.renameTo(archive)) { "Log rotation failed" }
        }
        current.appendBytes(bytes)
        prune()
    }

    private fun prune() {
        val archives = files().filter { it != current }.sortedWith(compareByDescending<File> { it.lastModified() }.thenByDescending { it.name })
        archives.drop((maxFiles - 1).coerceAtLeast(0)).forEach { check(it.delete()) { "Log retention failed" } }
    }

    fun export(): String {
        prepare()
        val result = StringBuilder()
        var used = 0
        for (file in files().sortedWith(compareByDescending<File> { it.lastModified() }.thenByDescending { it.name })) {
            if (used >= maxExportBytes) break
            file.bufferedReader().useLines { lines ->
                for (line in lines) {
                    val safe = DiagnosticSanitizer.sanitize(line) + "\n"
                    val size = safe.toByteArray(Charsets.UTF_8).size
                    if (used + size > maxExportBytes) break
                    result.append(safe)
                    used += size
                }
            }
        }
        return result.toString()
    }

    fun clear() {
        prepare()
        files().forEach { check(it.delete()) { "Log clearing failed" } }
    }
}
