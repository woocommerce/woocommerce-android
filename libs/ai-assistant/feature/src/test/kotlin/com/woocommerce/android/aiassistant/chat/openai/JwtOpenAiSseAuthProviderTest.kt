package com.woocommerce.android.aiassistant.chat.openai

import com.woocommerce.android.aiassistant.core.auth.JwtTokenProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class JwtOpenAiSseAuthProviderTest {
    @Test
    fun `given jwt token provider, when bearer token is requested, then jwt is returned`() = runTest {
        val tokenProvider = RecordingJwtTokenProvider()
        val authProvider = JwtOpenAiSseAuthProvider(tokenProvider)

        val token = authProvider.bearerToken()

        assertThat(token).isEqualTo("jwt-token-1")
        assertThat(tokenProvider.provideCalls).isEqualTo(1)
    }

    @Test
    fun `given jwt token provider, when invalidated, then provider is invalidated`() = runTest {
        val tokenProvider = RecordingJwtTokenProvider()
        val authProvider = JwtOpenAiSseAuthProvider(tokenProvider)

        authProvider.invalidate()

        assertThat(tokenProvider.invalidations).isEqualTo(1)
    }

    private class RecordingJwtTokenProvider : JwtTokenProvider {
        var provideCalls = 0
            private set
        var invalidations = 0
            private set

        override suspend fun provide(): String {
            provideCalls += 1
            return "jwt-token-$provideCalls"
        }

        override suspend fun invalidate() {
            invalidations += 1
        }
    }
}
