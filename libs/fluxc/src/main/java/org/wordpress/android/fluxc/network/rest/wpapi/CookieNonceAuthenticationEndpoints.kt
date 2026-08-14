package org.wordpress.android.fluxc.network.rest.wpapi

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.extensions.slashJoin

data class CookieNonceAuthenticationEndpoints(
    val siteUrl: String,
    val loginEntryUrl: String? = null,
    val adminBaseUrl: String? = null,
    val adminBaseVerification: AdminBaseVerification = AdminBaseVerification.NONE
) {
    fun validate(): ValidationResult {
        val validatedSiteUrl = siteUrl.networkUrl()
            ?: return invalid(Endpoint.CANONICAL, ValidationError.INVALID_URL)
        return if (validatedSiteUrl.hasUserInfo() || validatedSiteUrl.query != null) {
            invalid(Endpoint.CANONICAL, ValidationError.UNSAFE_ORIGIN)
        } else {
            validateEndpoints(validatedSiteUrl)
        }
    }

    private fun validateEndpoints(siteUrl: HttpUrl): ValidationResult {
        val (login, loginError) = validatedEndpoint(
            loginEntryUrl ?: siteUrl.toString().slashJoin("wp-login.php"),
            siteUrl
        )
        if (loginError != null) return invalid(Endpoint.LOGIN, loginError)

        val (admin, adminError) = validatedEndpoint(
            adminBaseUrl ?: siteUrl.toString().slashJoin("wp-admin"),
            siteUrl,
            allowQuery = false
        )
        if (adminError != null) return invalid(Endpoint.ADMIN, adminError)

        return ValidationResult.Valid(
            siteUrl = siteUrl,
            loginEntryUrl = requireNotNull(login),
            adminBaseUrl = requireNotNull(admin).asAdminBaseUrl(),
            adminBaseVerification = adminBaseVerification
        )
    }

    private fun validatedEndpoint(
        rawUrl: String,
        siteUrl: HttpUrl,
        allowQuery: Boolean = true
    ): Pair<HttpUrl?, ValidationError?> {
        val url = rawUrl.networkUrl()
            ?: return null to ValidationError.INVALID_URL
        if ((!allowQuery && url.query != null) || !isAllowedForSite(url, siteUrl)) {
            return null to ValidationError.UNSAFE_ORIGIN
        }
        return url to null
    }

    enum class Endpoint { CANONICAL, LOGIN, ADMIN }
    enum class ValidationError { INVALID_URL, UNSAFE_ORIGIN }
    enum class AdminBaseVerification { NONE, AUTHENTICATED_DASHBOARD }

    sealed interface ValidationResult {
        data class Valid(
            val siteUrl: HttpUrl,
            val loginEntryUrl: HttpUrl,
            val adminBaseUrl: HttpUrl,
            val adminBaseVerification: AdminBaseVerification
        ) : ValidationResult {
            fun allows(candidateUrl: HttpUrl, previousUrl: HttpUrl? = null): Boolean =
                isAllowedForSite(candidateUrl, siteUrl, previousUrl)

            fun isAdminBase(candidateUrl: HttpUrl): Boolean =
                candidateUrl.asAdminBaseUrl() == adminBaseUrlFor(candidateUrl)

            fun adminBaseUrlFor(loginUrl: HttpUrl): HttpUrl =
                adminBaseUrl.withSecureLoginScheme(loginUrl, siteUrl)

            fun nonceUrlFor(loginUrl: HttpUrl): HttpUrl = adminBaseUrlFor(loginUrl)
                .newBuilder()
                .addPathSegment(ADMIN_AJAX_PATH_SEGMENT)
                .encodedQuery(REST_NONCE_QUERY)
                .build()

            fun isNonceEndpoint(candidateUrl: HttpUrl?): Boolean =
                candidateUrl != null &&
                    candidateUrl.encodedPathSegments.lastOrNull() == ADMIN_AJAX_PATH_SEGMENT &&
                    candidateUrl.encodedQuery == REST_NONCE_QUERY
        }

        data class Invalid(val endpoint: Endpoint, val error: ValidationError) : ValidationResult
    }

    companion object {
        fun from(site: SiteModel) = CookieNonceAuthenticationEndpoints(
            siteUrl = site.url,
            loginEntryUrl = site.loginUrl?.takeUnless(String::isBlank),
            adminBaseUrl = site.adminUrl?.takeUnless(String::isBlank)
        )

        private fun isAllowedForSite(candidate: HttpUrl, siteUrl: HttpUrl, previous: HttpUrl? = null): Boolean {
            if (candidate.hasUserInfo() || candidate.host != siteUrl.host) return false
            if (previous?.scheme == "https" && candidate.scheme != "https") return false
            return when (siteUrl.scheme) {
                "https" -> candidate.scheme == "https" && candidate.port == siteUrl.port
                "http" -> when (candidate.scheme) {
                    "https" -> siteUrl.port == HTTP_PORT && candidate.port == HTTPS_PORT
                    "http" -> candidate.port == siteUrl.port
                    else -> false
                }
                else -> false
            }
        }

        private fun invalid(endpoint: Endpoint, error: ValidationError) =
            ValidationResult.Invalid(endpoint, error)

        private fun String.networkUrl() = toHttpUrlOrNull()?.takeIf { it.scheme == "http" || it.scheme == "https" }
            ?.newBuilder()?.fragment(null)?.build()

        private fun HttpUrl.asAdminBaseUrl(): HttpUrl {
            val baseUrl = if (encodedPathSegments.lastOrNull().equals("index.php", ignoreCase = true)) {
                newBuilder().removePathSegment(encodedPathSegments.lastIndex).build()
            } else {
                this
            }
            return if (baseUrl.encodedPath.endsWith('/')) {
                baseUrl
            } else {
                baseUrl.newBuilder().addPathSegment("").build()
            }
        }

        private fun HttpUrl.withSecureLoginScheme(loginUrl: HttpUrl, siteUrl: HttpUrl): HttpUrl =
            if (siteUrl.isDefaultHttp() && isDefaultHttp() && loginUrl.scheme == "https") {
                newBuilder().scheme("https").port(HTTPS_PORT).build()
            } else {
                this
            }

        private fun HttpUrl.isDefaultHttp(): Boolean = scheme == "http" && port == HTTP_PORT

        private fun HttpUrl.hasUserInfo() = encodedUsername.isNotEmpty() || encodedPassword.isNotEmpty()
        private const val HTTP_PORT = 80
        private const val HTTPS_PORT = 443
        private const val ADMIN_AJAX_PATH_SEGMENT = "admin-ajax.php"
        private const val REST_NONCE_QUERY = "action=rest-nonce"
    }
}
