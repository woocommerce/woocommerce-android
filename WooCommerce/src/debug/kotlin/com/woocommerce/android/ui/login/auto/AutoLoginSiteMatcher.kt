package com.woocommerce.android.ui.login.auto

import org.wordpress.android.fluxc.model.SiteModel
import java.net.URI
import java.util.Locale
import javax.inject.Inject

internal class AutoLoginSiteMatcher @Inject constructor() {
    fun matches(site: SiteModel, requestedUrl: String, expectedOrigin: Int): Boolean {
        val requestedAddress = canonicalAddress(requestedUrl) ?: return false
        return site.origin == expectedOrigin &&
            canonicalAddress(site.url) == requestedAddress
    }

    private fun canonicalAddress(value: String): CanonicalAddress? {
        val uri = runCatching { URI(value).normalize() }.getOrNull() ?: return null
        val hasSafeSchemeAndHost =
            uri.scheme.equals(HTTPS_SCHEME, ignoreCase = true) && !uri.host.isNullOrBlank()
        val hasExtraParts = listOf(uri.userInfo, uri.query, uri.fragment).any { it != null }
        if (!hasSafeSchemeAndHost || hasExtraParts) {
            return null
        }
        val path = uri.rawPath
            .orEmpty()
            .ifEmpty { ROOT_PATH }
            .let { if (it.length > 1) it.removeSuffix(ROOT_PATH) else it }
        return CanonicalAddress(
            host = uri.host.lowercase(Locale.US),
            port = uri.port.takeUnless { it == -1 || it == HTTPS_PORT },
            path = path
        )
    }

    private data class CanonicalAddress(
        val host: String,
        val port: Int?,
        val path: String
    )

    private companion object {
        const val HTTPS_SCHEME = "https"
        const val HTTPS_PORT = 443
        const val ROOT_PATH = "/"
    }
}
