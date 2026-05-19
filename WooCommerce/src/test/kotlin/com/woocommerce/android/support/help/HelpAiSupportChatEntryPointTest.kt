package com.woocommerce.android.support.help

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class HelpAiSupportChatEntryPointTest {
    @Test
    fun `given feature flag disabled, when checking availability, then chat is unavailable`() {
        val isAvailable = HelpAiSupportChatEntryPoint.isAvailable(featureFlagEnabled = false)

        assertThat(isAvailable).isFalse()
    }

    @Test
    fun `given feature flag enabled, when checking availability, then chat is available`() {
        val isAvailable = HelpAiSupportChatEntryPoint.isAvailable(featureFlagEnabled = true)

        assertThat(isAvailable).isTrue()
    }

    @Test
    fun `given user is logged out, when selecting launch mode, then pre-login is used`() {
        val shouldUsePreLoginLaunchMode = HelpAiSupportChatEntryPoint.shouldUsePreLoginLaunchMode(
            isUserLoggedIn = false
        )

        assertThat(shouldUsePreLoginLaunchMode).isTrue()
    }

    @Test
    fun `given user is logged in, when selecting launch mode, then normal help mode is used`() {
        val shouldUsePreLoginLaunchMode = HelpAiSupportChatEntryPoint.shouldUsePreLoginLaunchMode(
            isUserLoggedIn = true
        )

        assertThat(shouldUsePreLoginLaunchMode).isFalse()
    }
}
