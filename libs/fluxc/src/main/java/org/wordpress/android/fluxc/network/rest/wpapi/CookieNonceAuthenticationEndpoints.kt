package org.wordpress.android.fluxc.network.rest.wpapi

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.extensions.slashJoin

data class CookieNonceAuthenticationEndpoints(
    val canonicalSiteOrigin: String,
    val loginEntryUrl: String? = null,
    val adminBaseUrl: String? = null,
    val adminBaseVerification: AdminBaseVerification = AdminBaseVerification.NONE
) {
    fun validate(): ValidationResult {
        val canonical = canonicalSiteOrigin.networkUrl()
            ?: return invalid(Endpoint.CANONICAL, ValidationError.INVALID_URL)
        if (canonical.hasUserInfo() || canonical.query != null) {
            return invalid(Endpoint.CANONICAL, ValidationError.UNSAFE_ORIGIN)
        }

        val (login, loginError) = validatedEndpoint(
            loginEntryUrl ?: canonical.toString().slashJoin("wp-login.php"),
            canonical
        )
        if (loginError != null) return invalid(Endpoint.LOGIN, loginError)

        val (admin, adminError) = validatedEndpoint(
            adminBaseUrl ?: canonical.toString().slashJoin("wp-admin"),
            canonical,
            allowQuery = false
        )
        if (adminError != null) return invalid(Endpoint.ADMIN, adminError)

        return ValidationResult.Valid(canonical, requireNotNull(login), requireNotNull(admin).reusableAdminBase())
    }

    private fun validatedEndpoint(
        rawUrl: String,
        canonical: HttpUrl,
        allowQuery: Boolean = true
    ): Pair<HttpUrl?, ValidationError?> {
        val url = rawUrl.networkUrl()
            ?: return null to ValidationError.INVALID_URL
        if ((!allowQuery && url.query != null) || !isSafeFor(url, canonical)) {
            return null to ValidationError.UNSAFE_ORIGIN
        }
        return url to null
    }

    enum class Endpoint { CANONICAL, LOGIN, ADMIN }
    enum class ValidationError { INVALID_URL, UNSAFE_ORIGIN }
    enum class AdminBaseVerification { NONE, AUTHENTICATED_DASHBOARD }

    sealed interface ValidationResult {
        data class Valid(val canonicalSiteOrigin: HttpUrl, val loginEntryUrl: HttpUrl, val adminBaseUrl: HttpUrl) :
            ValidationResult

        data class Invalid(val endpoint: Endpoint, val error: ValidationError) : ValidationResult
    }

    companion object {
        fun from(site: SiteModel) = CookieNonceAuthenticationEndpoints(
            canonicalSiteOrigin = site.url,
            loginEntryUrl = site.loginUrl?.takeUnless(String::isBlank),
            adminBaseUrl = site.adminUrl?.takeUnless(String::isBlank)
        )

        fun isSafeFor(candidate: HttpUrl, canonical: HttpUrl, previous: HttpUrl? = null): Boolean {
            if (candidate.hasUserInfo() || candidate.host != canonical.host) return false
            if (previous?.scheme == "https" && candidate.scheme != "https") return false
            return when (canonical.scheme) {
                "https" -> candidate.scheme == "https" && candidate.port == canonical.port
                "http" -> canonical.port == HTTP_PORT &&
                    candidate.scheme == "https" && candidate.port == HTTPS_PORT ||
                    candidate.scheme == "http" && candidate.port == canonical.port
                else -> false
            }
        }

        private fun invalid(endpoint: Endpoint, error: ValidationError) =
            ValidationResult.Invalid(endpoint, error)

        private fun String.networkUrl() = toHttpUrlOrNull()?.takeIf { it.scheme == "http" || it.scheme == "https" }
            ?.newBuilder()?.fragment(null)?.build()

        private fun HttpUrl.reusableAdminBase(): HttpUrl {
            val base = if (encodedPathSegments.lastOrNull().equals(ADMIN_INDEX, ignoreCase = true)) {
                newBuilder().removePathSegment(encodedPathSegments.lastIndex).build()
            } else {
                this
            }
            return if (base.encodedPath.endsWith('/')) base else base.newBuilder().addPathSegment("").build()
        }

        private fun HttpUrl.hasUserInfo() = encodedUsername.isNotEmpty() || encodedPassword.isNotEmpty()
        private const val HTTP_PORT = 80
        private const val HTTPS_PORT = 443
        private const val ADMIN_INDEX = "index.php"
    }
}
