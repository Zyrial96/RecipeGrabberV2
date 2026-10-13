package com.recipegrabber.data.logging

internal object DiagnosticSanitizer {
    private val url = Regex("(?i)https?://[^\\s<>]+")
    private val email = Regex("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val authorization = Regex("(?i)\\b(?:Bearer|Basic)\\s+[^\\s,;]+")
    private val key = Regex("\\b(?:sk-[A-Za-z0-9_-]{8,}|AIza[A-Za-z0-9_-]{12,}|apify_api_[A-Za-z0-9_-]{8,})")
    private val jwt = Regex("\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")
    private val namedSecret = Regex("(?i)((?:api[_ -]?key|access[_ -]?token|refresh[_ -]?token|authorization|password|client[_ -]?secret)[\"']?\\s*[=:]\\s*)(?:\"[^\"]*\"|'[^']*'|[^\\s,;}]+)")

    fun sanitize(message: String): String {
        if (message.contains("Raw response:", ignoreCase = true)) return "Provider response omitted"
        if (message.length > 8192) return "Oversized diagnostic omitted"
        var result = message
        result = url.replace(result, "[URL]")
        result = email.replace(result, "[EMAIL]")
        result = authorization.replace(result, "[AUTHORIZATION]")
        result = key.replace(result, "[API_KEY]")
        result = jwt.replace(result, "[TOKEN]")
        result = namedSecret.replace(result) { "${it.groupValues[1]}[SECRET]" }
        return result.replace(Regex("[\\r\\n\\u0000-\\u001f\\u007f]"), " ").take(1024)
    }

    fun safeTag(tag: String) = if (tag.matches(Regex("[A-Za-z0-9_-]{1,40}"))) tag else "App"
}
