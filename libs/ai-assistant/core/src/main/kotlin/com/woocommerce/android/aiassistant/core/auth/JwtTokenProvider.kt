package com.woocommerce.android.aiassistant.core.auth

/**
 * Supplies a Jetpack AI JWT to [com.woocommerce.android.aiassistant.core.chat.ChatService].
 * Implementations may mint on demand or cache-and-refresh; the contract only
 * requires that [provide] returns a token good for one streaming call.
 *
 * [invalidate] is called by the chat service after a 401 so the next [provide]
 * mints a fresh token. Implementations with no cache can leave the default
 * no-op.
 */
interface JwtTokenProvider {
    suspend fun provide(): String
    suspend fun invalidate() {}
}

/**
 * Raised by [JwtTokenProvider] implementations when a JWT cannot be obtained.
 * The chat service maps this to [com.woocommerce.android.aiassistant.core.chat.ChatStreamError.AUTH]
 * at the stream layer; [com.woocommerce.android.aiassistant.core.chat.toAssistantError] maps it to
 * [com.woocommerce.android.aiassistant.core.chat.AssistantError.Auth] at the loop layer.
 */
class AssistantAuthException(
    message: String = "Failed to obtain Jetpack AI JWT",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
