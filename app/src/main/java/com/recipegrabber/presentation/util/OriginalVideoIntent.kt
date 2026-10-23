package com.recipegrabber.presentation.util

import android.content.Intent
import android.net.Uri
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

fun originalVideoIntent(sourceUrl: String): Intent? {
    val url = sourceUrl.toHttpUrlOrNull() ?: return null
    if (!url.isHttps || url.username.isNotEmpty() || url.password.isNotEmpty()) return null
    return Intent(Intent.ACTION_VIEW, Uri.parse(url.toString()))
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
