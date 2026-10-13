package com.recipegrabber.domain

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object VideoLinkParser {
    private val links = Regex("https://[^\\s<>\"]+", RegexOption.IGNORE_CASE)
    private val socialHosts = listOf("tiktok.com", "instagram.com", "facebook.com", "twitter.com", "x.com", "vimeo.com", "dailymotion.com")

    fun findVideoUrl(sharedText: String): String? = links.findAll(sharedText).map { it.value }
        .firstOrNull { candidate ->
            val url = candidate.toHttpUrlOrNull()
            url != null && url.isHttps && url.username.isEmpty() && url.password.isEmpty() && isVideo(url)
        }

    private fun isVideo(url: HttpUrl): Boolean {
        fun hostMatches(domain: String) = url.host == domain || url.host.endsWith(".$domain")
        if (hostMatches("youtube.com") || url.host == "youtu.be") {
            return (url.host == "youtu.be" && url.pathSegments.first().isNotBlank()) ||
                (url.encodedPath == "/watch" && !url.queryParameter("v").isNullOrBlank()) ||
                (url.pathSegments.first() in listOf("shorts", "live", "embed") &&
                    url.pathSegments.getOrNull(1)?.isNotBlank() == true)
        }
        return socialHosts.any(::hostMatches) ||
            url.pathSegments.last().substringAfterLast('.').lowercase(java.util.Locale.ROOT) in listOf("mp4", "webm", "mov")
    }
}
