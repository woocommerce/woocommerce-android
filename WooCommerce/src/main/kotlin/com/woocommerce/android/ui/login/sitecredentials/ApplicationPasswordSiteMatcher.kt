package com.woocommerce.android.ui.login.sitecredentials

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object ApplicationPasswordSiteMatcher {
    private const val WWW_PREFIX = "www."
    private const val APPROVAL_PAGE = "authorize-application.php"

    /**
     * True when [candidateUrl] is on [siteUrl]'s host, ignoring `www.`, case, scheme and port, and at or below its
     * path, so a subfolder install still matches while a sibling site on the same host doesn't.
     */
    fun isWithinSite(siteUrl: String?, candidateUrl: String?): Boolean {
        val site = siteUrl?.toSiteUrl() ?: return false
        val candidate = candidateUrl?.toSiteUrl() ?: return false
        val sitePath = site.serverPath().map(String::lowercase)
        val candidatePath = candidate.serverPath().map(String::lowercase)
        return site.comparableHost() == candidate.comparableHost() &&
            candidatePath.size >= sitePath.size &&
            candidatePath.subList(0, sitePath.size) == sitePath
    }

    fun isSameSiteAddress(siteUrl: String?, candidateUrl: String?): Boolean =
        isWithinSite(siteUrl, candidateUrl) && isWithinSite(candidateUrl, siteUrl)

    /**
     * True for WordPress's approval page, also when the server is given extra path after it. Checked before `..` is
     * resolved, because some servers run the page and pass the rest as extra path. Also true when the URL has a user
     * name.
     */
    fun isApprovalPage(url: String?): Boolean =
        url?.toUrlWithDefaultScheme()?.serverPath(resolveDots = false).orEmpty().any {
            it.equals(APPROVAL_PAGE, ignoreCase = true)
        }

    /**
     * The WordPress address that serves [authorizationUrl], which is that address followed by the admin
     * directory and `authorize-application.php`. Null when the URL isn't an approval page.
     */
    fun approvalSiteUrl(authorizationUrl: String?): String? {
        val url = authorizationUrl?.toSiteUrl() ?: return null
        val segments = url.serverPath()
        val approvalPageIndex = segments.indexOfLast { it.equals(APPROVAL_PAGE, ignoreCase = true) }
        if (approvalPageIndex < 1) return null
        return HttpUrl.Builder()
            .scheme(url.scheme)
            .host(url.host)
            .port(url.port)
            .apply { segments.take(approvalPageIndex - 1).forEach(::addPathSegment) }
            .build()
            .toString()
            .trimEnd('/')
    }

    private fun String.toSiteUrl(): HttpUrl? =
        toUrlWithDefaultScheme()?.takeIf { it.username.isEmpty() && it.password.isEmpty() }

    private fun String.toUrlWithDefaultScheme(): HttpUrl? {
        val trimmed = trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        return withScheme.toHttpUrlOrNull()
    }

    private fun HttpUrl.comparableHost(): String = host.lowercase().removePrefix(WWW_PREFIX)

    /**
     * The path as a web server resolves it. OkHttp keeps an encoded slash (`%2F`) inside one decoded segment, while
     * servers such as nginx treat it as a separator, so segments are split again and `.` and `..` are resolved.
     */
    private fun HttpUrl.serverPath(resolveDots: Boolean = true): List<String> {
        val resolved = ArrayDeque<String>()
        pathSegments.flatMap { it.split('/', '\\') }.forEach { segment ->
            when {
                segment.isEmpty() || segment == "." && resolveDots -> Unit
                segment == ".." && resolveDots -> resolved.removeLastOrNull()
                else -> resolved.addLast(segment)
            }
        }
        return resolved.toList()
    }
}
