package com.woocommerce.android.aiassistant.chat.openai

import com.woocommerce.android.aiassistant.auth.WpComOAuthTokenProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WpComOpenAiSseAuthProviderTest {
    @Test
    fun `given wpcom token provider, when bearer token is requested, then wpcom token is returned`() = runTest {
        val tokenProvider = RecordingWpComOAuthTokenProvider()
        val authProvider = WpComOpenAiSseAuthProvider(tokenProvider)

        val token = authProvider.bearerToken()

        assertThat(token).isEqualTo("wpcom-token-1")
        assertThat(tokenProvider.provideCalls).isEqualTo(1)
    }

    private class RecordingWpComOAuthTokenProvider : WpComOAuthTokenProvider {
        var provideCalls = 0
            private set

        override suspend fun provide(): String {
            provideCalls += 1
            return "wpcom-token-$provideCalls"
        }
    }
}
