package com.woocommerce.android.aiassistant.auth

import com.woocommerce.android.aiassistant.core.auth.AssistantAuthException
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class WpComOAuthTokenProviderTest {
    @Test
    fun `given an existing access token, when providing token, then token value is returned`() = runTest {
        val provider = AccessTokenWpComOAuthTokenProvider(
            accessToken = mock {
                on { exists() } doReturn true
                on { get() } doReturn WPCOM_TOKEN
            }
        )

        val token = provider.provide()

        assertThat(token).isEqualTo(WPCOM_TOKEN)
    }

    @Test
    fun `given access token does not exist, when providing token, then auth exception is thrown`() = runTest {
        val provider = AccessTokenWpComOAuthTokenProvider(
            accessToken = mock {
                on { exists() } doReturn false
            }
        )

        val result = runCatching { provider.provide() }

        assertThat(result.exceptionOrNull()).isInstanceOf(AssistantAuthException::class.java)
    }

    @Test
    fun `given access token value is blank, when providing token, then auth exception is thrown`() = runTest {
        val provider = AccessTokenWpComOAuthTokenProvider(
            accessToken = mock {
                on { exists() } doReturn true
                on { get() } doReturn " "
            }
        )

        val result = runCatching { provider.provide() }

        assertThat(result.exceptionOrNull()).isInstanceOf(AssistantAuthException::class.java)
    }

    private companion object {
        private const val WPCOM_TOKEN = "wpcom-token"
    }
}
