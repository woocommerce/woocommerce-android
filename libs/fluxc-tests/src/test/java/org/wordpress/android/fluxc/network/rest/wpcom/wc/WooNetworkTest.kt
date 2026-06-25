package org.wordpress.android.fluxc.network.rest.wpcom.wc

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsNetwork
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsStore
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.JetpackApplicationPasswordsErrorHandler
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.JetpackApplicationPasswordsSupport
import org.wordpress.android.fluxc.network.rest.wpcom.JetpackTunnelWPAPINetwork
import org.wordpress.android.fluxc.test
import java.security.GeneralSecurityException
import java.util.Optional

@RunWith(RobolectricTestRunner::class)
class WooNetworkTest {
    private val testSite = SiteModel().apply {
        origin = SiteModel.ORIGIN_WPCOM_REST
        url = "https://example.com"
        siteId = 123
    }
    private val testPath = "path"
    private val applicationPasswordsConfiguration = FakeApplicationPasswordsConfiguration()
    private val applicationPasswordsNetwork: ApplicationPasswordsNetwork = mock()
    private val jetpackTunnelWPAPINetwork: JetpackTunnelWPAPINetwork = mock()
    private val jetpackApplicationPasswordsErrorHandler: JetpackApplicationPasswordsErrorHandler = mock()
    private val jetpackApplicationPasswordsSupport: JetpackApplicationPasswordsSupport = mock()
    private val unknownBlogListener: UnknownBlogListener = mock()

    private val sut = WooNetwork(
        applicationPasswordsConfiguration = applicationPasswordsConfiguration,
        applicationPasswordsNetwork = applicationPasswordsNetwork,
        jetpackTunnelWPAPINetwork = jetpackTunnelWPAPINetwork,
        jetpackApplicationPasswordsSupport = jetpackApplicationPasswordsSupport,
        jetpackApplicationPasswordsErrorHandler = jetpackApplicationPasswordsErrorHandler,
        unknownBlogListener = Optional.of(unknownBlogListener)
    )

    @Test
    fun `given Jetpack site supports app passwords, when making request, then use app passwords network`() = test {
        whenever(jetpackApplicationPasswordsSupport.supportsAppPasswords(testSite)).thenReturn(true)
        val sampleResponse = SampleResponse("value")
        givenAppPasswordsResponse(WPAPIResponse.Success(SampleResponse("value"), emptyList()))

        val response = sut.executeGetGsonRequest(
            site = testSite,
            path = testPath,
            clazz = SampleResponse::class.java
        )

        assertThat((response as WPAPIResponse.Success).data).isEqualTo(sampleResponse)
        verify(jetpackTunnelWPAPINetwork, never()).executeGetGsonRequest(
            site = testSite,
            path = testPath,
            clazz = SampleResponse::class.java
        )
    }

    @Test
    fun `given jetpack site that supports app passwords, when request fails, then fall back to jetpack tunnel`() =
        test {
            whenever(jetpackApplicationPasswordsSupport.supportsAppPasswords(testSite)).thenReturn(true)
            givenAppPasswordsResponse(
                WPAPIResponse.Error(
                    WPAPINetworkError(
                        mock(),
                        "error"
                    )
                )
            )
            val sampleResponse = SampleResponse("value")
            givenJetpackTunnelResponse(WPAPIResponse.Success(sampleResponse, emptyList()))

            val response = sut.executeGetGsonRequest(
                site = testSite,
                path = testPath,
                clazz = SampleResponse::class.java
            )

            assertThat((response as WPAPIResponse.Success).data).isEqualTo(sampleResponse)
        }

    @Test
    fun `given jetpack site that does not support app passwords, when making request, then use jetpack tunnel`() =
        test {
            whenever(jetpackApplicationPasswordsSupport.supportsAppPasswords(testSite)).thenReturn(false)
            val sampleResponse = SampleResponse("value")
            givenJetpackTunnelResponse(WPAPIResponse.Success(sampleResponse, emptyList()))

            val response = sut.executeGetGsonRequest(
                site = testSite,
                path = testPath,
                clazz = SampleResponse::class.java
            )

            assertThat((response as WPAPIResponse.Success).data).isEqualTo(sampleResponse)
        }

    @Test
    fun `when app passwords are disabled for jetpack access, then always use jetpack tunnel`() = test {
        applicationPasswordsConfiguration.isEnabledForJetpackAccessValue = false
        whenever(jetpackApplicationPasswordsSupport.supportsAppPasswords(testSite)).thenReturn(true)
        val sampleResponse = SampleResponse("value")
        givenJetpackTunnelResponse(WPAPIResponse.Success(sampleResponse, emptyList()))

        val response = sut.executeGetGsonRequest(
            site = testSite,
            path = testPath,
            clazz = SampleResponse::class.java
        )

        assertThat((response as WPAPIResponse.Success).data).isEqualTo(sampleResponse)
        verify(applicationPasswordsNetwork, never()).executeGetGsonRequest(
            site = testSite,
            path = testPath,
            clazz = SampleResponse::class.java
        )
    }

    @Test
    fun `when jetpack tunnel fallback succeeds after app passwords failure, then notify error handler`() = test {
        whenever(jetpackApplicationPasswordsSupport.supportsAppPasswords(testSite)).thenReturn(true)
        val appPasswordsNetworkError = WPAPINetworkError(mock(), "error")
        givenAppPasswordsResponse(WPAPIResponse.Error(appPasswordsNetworkError))
        val sampleResponse = SampleResponse("value")
        givenJetpackTunnelResponse(WPAPIResponse.Success(sampleResponse, emptyList()))

        sut.executeGetGsonRequest(
            site = testSite,
            path = testPath,
            clazz = SampleResponse::class.java
        )

        verify(jetpackApplicationPasswordsErrorHandler).handleError(testSite, appPasswordsNetworkError)
    }

    @Test
    fun `when a KeyStore error occurs, then fallback to Jetpack Tunnel`() = test {
        given(jetpackApplicationPasswordsSupport.supportsAppPasswords(testSite)).willReturn(true)
        given(
            applicationPasswordsNetwork.executeGetGsonRequest(
                testSite,
                testPath,
                SampleResponse::class.java
            )
        ).willAnswer {
            throw GeneralSecurityException()
        }
        givenJetpackTunnelResponse(WPAPIResponse.Success(SampleResponse("value"), emptyList()))

        val response = sut.executeGetGsonRequest(
            site = testSite,
            path = testPath,
            clazz = SampleResponse::class.java
        )

        assertThat((response as WPAPIResponse.Success).data).isEqualTo(SampleResponse("value"))
        verify(jetpackApplicationPasswordsErrorHandler).handleError(
            eq(testSite),
            argThat { errorCode == ApplicationPasswordsStore.APPLICATION_PASSWORDS_KEYSTORE_ENCRYPTION_ERROR }
        )
    }

    @Test
    fun `given a request returns unknown_blog, when making request, then notify the unknown blog listener`() = test {
        whenever(jetpackApplicationPasswordsSupport.supportsAppPasswords(testSite)).thenReturn(false)
        givenJetpackTunnelResponse(WPAPIResponse.Error(WPAPINetworkError(mock(), "unknown_blog")))

        sut.executeGetGsonRequest(site = testSite, path = testPath, clazz = SampleResponse::class.java)

        verify(unknownBlogListener).onUnknownBlog(testSite.siteId)
    }

    @Test
    fun `given a successful request, when making request, then do not notify the unknown blog listener`() = test {
        whenever(jetpackApplicationPasswordsSupport.supportsAppPasswords(testSite)).thenReturn(false)
        givenJetpackTunnelResponse(WPAPIResponse.Success(SampleResponse("value"), emptyList()))

        sut.executeGetGsonRequest(site = testSite, path = testPath, clazz = SampleResponse::class.java)

        verify(unknownBlogListener, never()).onUnknownBlog(any())
    }

    private suspend fun givenAppPasswordsResponse(response: WPAPIResponse<SampleResponse>) {
        given(
            applicationPasswordsNetwork.executeGetGsonRequest(
                testSite,
                testPath,
                SampleResponse::class.java
            )
        ).willReturn(response)
    }

    private suspend fun givenJetpackTunnelResponse(response: WPAPIResponse<SampleResponse>) {
        given(
            jetpackTunnelWPAPINetwork.executeGetGsonRequest(
                testSite,
                testPath,
                SampleResponse::class.java
            )
        ).willReturn(response)
    }

    private data class SampleResponse(val value: String)

    private class FakeApplicationPasswordsConfiguration : ApplicationPasswordsConfiguration {
        var isEnabledForJetpackAccessValue: Boolean = true

        override val applicationName: String
            get() = "WooCommerce"

        override suspend fun isEnabledForJetpackAccess(): Boolean = isEnabledForJetpackAccessValue
    }
}
