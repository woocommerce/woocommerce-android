package com.woocommerce.android.aiassistant.auth

import com.woocommerce.android.aiassistant.core.auth.AssistantAuthException
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.JWTToken
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAICompletionsErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAIJWTTokenResponse
import java.util.Base64

class WpComJetpackAiTokenProviderTest {
    private val selectedSite: SelectedSite = mock()
    private val restClient: JetpackAIRestClient = mock()

    private lateinit var provider: WpComJetpackAiTokenProvider

    @Before
    fun setUp() {
        provider = WpComJetpackAiTokenProvider(selectedSite, restClient)
    }

    @Test
    fun `given no selected site, when provide is called, then AssistantAuthException is thrown`() = runTest {
        whenever(selectedSite.getOrNull()).thenReturn(null)

        val error = runCatching { provider.provide() }.exceptionOrNull()

        assertThat(error)
            .isInstanceOf(AssistantAuthException::class.java)
            .hasMessageContaining("No selected site")
    }

    @Test
    fun `given REST returns error, when provide is called, then AssistantAuthException carries the error type`() = runTest {
        val site = SiteModel().apply { siteId = SITE_ID }
        selectedSite.stub { on { getOrNull() } doReturn site }
        whenever(restClient.fetchJetpackAIJWTToken(site)).thenReturn(
            JetpackAIJWTTokenResponse.Error(
                type = JetpackAICompletionsErrorType.AUTH_ERROR,
                message = "expired",
            )
        )

        val error = runCatching { provider.provide() }.exceptionOrNull()

        assertThat(error)
            .isInstanceOf(AssistantAuthException::class.java)
            .hasMessageContaining("AUTH_ERROR")
    }

    @Test
    fun `given REST returns error, when provide is called twice, then both calls hit the network`() = runTest {
        val site = SiteModel().apply { siteId = SITE_ID }
        selectedSite.stub { on { getOrNull() } doReturn site }
        whenever(restClient.fetchJetpackAIJWTToken(site)).thenReturn(
            JetpackAIJWTTokenResponse.Error(
                type = JetpackAICompletionsErrorType.GENERIC_ERROR,
                message = null,
            )
        )

        runCatching { provider.provide() }
        runCatching { provider.provide() }

        verify(restClient, times(2)).fetchJetpackAIJWTToken(site)
    }

    @Test
    fun `given REST returns malformed JWT, when provide is called, then AssistantAuthException is thrown`() = runTest {
        val site = SiteModel().apply { siteId = SITE_ID }
        selectedSite.stub { on { getOrNull() } doReturn site }
        whenever(restClient.fetchJetpackAIJWTToken(site)).thenReturn(
            JetpackAIJWTTokenResponse.Success(malformedJwtToken())
        )

        val error = runCatching { provider.provide() }.exceptionOrNull()

        assertThat(error)
            .isInstanceOf(AssistantAuthException::class.java)
            .hasMessageContaining(INVALID_JWT_MESSAGE)
    }

    @Test
    fun `given cached JWT is malformed, when provide is called, then AssistantAuthException is thrown`() = runTest {
        val site = SiteModel().apply { siteId = SITE_ID }
        selectedSite.stub { on { getOrNull() } doReturn site }
        givenCachedToken(malformedJwtToken())

        val error = runCatching { provider.provide() }.exceptionOrNull()

        assertThat(error)
            .isInstanceOf(AssistantAuthException::class.java)
            .hasMessageContaining(INVALID_JWT_MESSAGE)
        verify(restClient, times(0)).fetchJetpackAIJWTToken(site)
    }

    @Test
    fun `given no token has been minted, when invalidate is called, then nothing is thrown`() = runTest {
        provider.invalidate()
    }

    private fun givenCachedToken(token: JWTToken) {
        val field = provider.javaClass.getDeclaredField("cached")
        field.isAccessible = true
        field.set(provider, token.value)
    }

    private fun malformedJwtToken(): JWTToken {
        val malformedPayload = Base64.getEncoder().encodeToString("not-json".toByteArray())
        return JWTToken("ignored.$malformedPayload.ignored")
    }

    companion object {
        private const val SITE_ID = 42L
        private const val INVALID_JWT_MESSAGE = "Invalid Jetpack AI JWT"
    }
}
