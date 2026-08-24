package org.wordpress.android.fluxc.utils

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.Locale
import javax.inject.Inject

class HttpsUrlNormalizer @Inject constructor() {
    data class Result(
        val normalizedUrl: String,
        val wasUpgraded: Boolean,
    )

    @JvmOverloads
    fun normalize(rawUrl: String, addHttpsSchemeIfMissing: Boolean = false): Result {
        val trimmedUrl = rawUrl.trim()
        val scheme = SCHEME_REGEX.find(trimmedUrl)?.groupValues?.get(1)?.lowercase(Locale.ROOT)
        val urlWithScheme = when {
            scheme == null && addHttpsSchemeIfMissing -> "https://$trimmedUrl"
            scheme == null -> throw IllegalArgumentException("URL has no scheme")
            scheme == HTTP_SCHEME || scheme == HTTPS_SCHEME -> trimmedUrl
            else -> throw IllegalArgumentException("Unsupported URL scheme")
        }
        val parsedUrl = requireNotNull(urlWithScheme.toHttpUrlOrNull()) { "Malformed URL" }
        val hadEmptyPath = urlWithScheme.hasEmptyPath()
        val wasUpgraded = parsedUrl.scheme == HTTP_SCHEME
        val normalizedUrlWithPath = if (wasUpgraded) {
            parsedUrl.newBuilder().scheme(HTTPS_SCHEME).build().toString()
        } else {
            parsedUrl.toString()
        }
        val normalizedUrl = if (hadEmptyPath) {
            normalizedUrlWithPath.withoutSyntheticRootPath()
        } else {
            normalizedUrlWithPath
        }
        return Result(normalizedUrl, wasUpgraded)
    }

    private fun String.hasEmptyPath(): Boolean {
        val authorityStart = indexOf("://") + 3
        val authorityEnd = listOf(indexOf('?', authorityStart), indexOf('#', authorityStart), length)
            .filter { it >= 0 }
            .min()
        val pathStart = indexOf('/', authorityStart)
        return pathStart == -1 || pathStart >= authorityEnd
    }

    private fun String.withoutSyntheticRootPath(): String {
        val authorityEnd = indexOf('/', startIndex = indexOf("://") + 3)
        return if (authorityEnd >= 0 && getOrNull(authorityEnd + 1) in listOf(null, '?', '#')) {
            removeRange(authorityEnd, authorityEnd + 1)
        } else {
            this
        }
    }

    companion object {
        private val SCHEME_REGEX = Regex("^([A-Za-z][A-Za-z0-9+.-]*):")
        private const val HTTP_SCHEME = "http"
        private const val HTTPS_SCHEME = "https"
    }
}
