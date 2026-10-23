package com.recipegrabber.presentation.util

import java.net.URI
import java.net.URLDecoder
import java.util.Locale

fun recipeArtworkUrl(thumbnailUrl: String?, sourceUrl: String): String? {
    fun https(value: String): URI? = runCatching {
        URI(value).takeIf { it.scheme.equals("https", true) && !it.host.isNullOrBlank() && it.userInfo == null }
    }.getOrNull()
    thumbnailUrl?.takeIf { https(it) != null }?.let { return it }
    val source = https(sourceUrl) ?: return null
    val host = source.host.lowercase(Locale.ROOT)
    val segments = source.path.orEmpty().split('/').filter { it.isNotEmpty() }
    val id = when {
        host == "youtu.be" -> segments.singleOrNull()
        host in setOf("youtube.com", "www.youtube.com", "m.youtube.com") -> when {
            segments == listOf("watch") -> runCatching {
                source.rawQuery.orEmpty().split('&').map { it.split('=', limit = 2) }
                    .firstOrNull { it.size == 2 && it[0] == "v" }?.get(1)?.let { URLDecoder.decode(it, "UTF-8") }
            }.getOrNull()
            segments.size == 2 && segments[0] in setOf("shorts", "live", "embed") -> segments[1]
            else -> null
        }
        else -> null
    }
    return id?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" }
}
