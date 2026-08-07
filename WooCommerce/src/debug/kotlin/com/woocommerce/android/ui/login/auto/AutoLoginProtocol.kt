package com.woocommerce.android.ui.login.auto

internal enum class AutoLoginConnection {
    WP_API,
    WPCOM
}

internal class AutoLoginCredentials(
    val username: String,
    val password: String
) {
    override fun toString(): String = REDACTED

    companion object {
        private const val REDACTED = "[REDACTED]"
    }
}

internal class AutoLoginRequest(
    val connection: AutoLoginConnection,
    val siteUrl: String,
    val credentials: AutoLoginCredentials
) {
    override fun toString(): String = "AutoLoginRequest(connection=$connection)"
}

internal sealed interface AutoLoginRequestParseResult {
    data class Success(val request: AutoLoginRequest) : AutoLoginRequestParseResult
    data object Invalid : AutoLoginRequestParseResult
}

internal enum class AutoLoginStatus {
    SUCCESS,
    ALREADY_ACTIVE,
    CONFLICT,
    INVALID_REQUEST,
    AUTH_REQUIRES_2FA,
    AUTH_FAILED,
    SITE_FAILED,
    INTERNAL_ERROR;

    val shouldNavigate: Boolean
        get() = this == SUCCESS || this == ALREADY_ACTIVE
}

internal sealed interface AutoLoginResult {
    data object Success : AutoLoginResult
    data object AlreadyActive : AutoLoginResult
    data class Failure(val status: AutoLoginStatus) : AutoLoginResult {
        init {
            require(!status.shouldNavigate)
        }
    }
}
