package com.woocommerce.android.util

import org.wordpress.android.fluxc.model.SiteModel
import java.security.MessageDigest

private const val SITE_HASH_PREFIX = "woopos-site-v1:"
private const val SITE_URL_HASH_PREFIX = "woopos-site-url-v1:"
private const val SITE_HASH_HEX_LENGTH = 32
private val URL_SCHEME = Regex("^[a-z][a-z0-9+.-]*://", RegexOption.IGNORE_CASE)

data class SitePairingIdentity(
    val siteHash: String,
    val siteUrlHash: String?,
) {
    fun matches(otherSiteHash: String, otherSiteUrlHash: String?): Boolean =
        otherSiteHash == siteHash || (siteUrlHash != null && otherSiteUrlHash == siteUrlHash)
}

fun SiteModel.pairingIdentity(): SitePairingIdentity? {
    val urlHash = normalizedSiteUrl(url)?.let(::siteUrlHash)
    val idHash = remoteId().value.takeIf { it != 0L }?.let(::siteIdHash)
    return (idHash ?: urlHash)?.let { SitePairingIdentity(siteHash = it, siteUrlHash = urlHash) }
}

fun siteIdHash(remoteId: Long): String = sha256Prefix("$SITE_HASH_PREFIX$remoteId")

private fun siteUrlHash(normalizedUrl: String): String = sha256Prefix("$SITE_URL_HASH_PREFIX$normalizedUrl")

private fun normalizedSiteUrl(url: String?): String? {
    val withoutScheme = url.orEmpty().trim().replace(URL_SCHEME, "")
    val host = withoutScheme.substringBefore('/').lowercase().removePrefix("www.")
    val path = withoutScheme.substringAfter('/', missingDelimiterValue = "").trimEnd('/')
    return when {
        host.isEmpty() -> null
        path.isEmpty() -> host
        else -> "$host/$path"
    }
}

private fun sha256Prefix(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    return bytes.joinToString(separator = "") { "%02x".format(it) }.take(SITE_HASH_HEX_LENGTH)
}
