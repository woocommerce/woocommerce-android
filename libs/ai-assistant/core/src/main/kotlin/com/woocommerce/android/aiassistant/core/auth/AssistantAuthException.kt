package com.woocommerce.android.aiassistant.core.auth

/** Raised by chat auth providers when a transport credential cannot be obtained. */
class AssistantAuthException(
    message: String = "Failed to obtain assistant auth credential",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
