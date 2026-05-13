package com.woocommerce.android.support.help

object HelpAiSupportChatEntryPoint {
    fun isAvailable(featureFlagEnabled: Boolean): Boolean = featureFlagEnabled

    fun shouldUsePreLoginLaunchMode(isUserLoggedIn: Boolean): Boolean = !isUserLoggedIn
}
