package com.woocommerce.android.support.help

object HelpAiSupportChatEntryPoint {
    fun isAvailable(featureFlagEnabled: Boolean): Boolean = featureFlagEnabled

    fun shouldOpenAiSupportChatFromContactSupport(aiSupportChatAvailable: Boolean): Boolean = aiSupportChatAvailable

    fun shouldUsePreLoginLaunchMode(hasSelectedSite: Boolean): Boolean = !hasSelectedSite
}
