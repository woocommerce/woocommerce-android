package org.wordpress.android.fluxc.network.rest.wpapi

import org.wordpress.android.fluxc.network.UnexpectedStoreResponse

sealed interface Nonce {
    val value: String?
        get() = null
    val username: String?

    data class Available(override val value: String, override val username: String) : Nonce
    data class FailedRequest(
        val timeOfResponse: Long,
        override val username: String,
        val type: CookieNonceErrorType,
        val networkError: WPAPINetworkError? = null,
        val errorMessage: String? = null,
        val loginEntryVerified: Boolean = false,
        val unexpectedStoreResponse: UnexpectedStoreResponse? = null,
        val step: CookieNonceLoginStep? = null,
    ) : Nonce

    data class Unknown(
        override val username: String?,
        val loginEntryVerified: Boolean = false,
    ) : Nonce

    enum class CookieNonceErrorType {
        INVALID_RESPONSE,
        INVALID_CREDENTIALS,
        CUSTOM_LOGIN_URL,
        CUSTOM_ADMIN_URL,
        INVALID_NONCE,
        BASIC_AUTH_REQUIRED,
        GENERIC_ERROR,
        UNKNOWN
    }

    enum class CookieNonceLoginStep {
        LOGIN_PAGE,
        CREDENTIALS_SUBMISSION,
        DASHBOARD_VERIFICATION,
        NONCE_RETRIEVAL
    }
}
