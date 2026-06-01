package com.woocommerce.android.support.help

object HelpAiSupportChatEntryPoint {
    fun isAvailable(featureFlagEnabled: Boolean): Boolean = featureFlagEnabled

    fun shouldShowContactSupport(aiSupportChatAvailable: Boolean): Boolean = !aiSupportChatAvailable

    fun shouldUsePreLoginLaunchMode(isUserLoggedIn: Boolean): Boolean = !isUserLoggedIn
}
