package org.wordpress.android.fluxc.network.rest.wpcom.wc

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsNetwork
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsStore
import org.wordpress.android.fluxc.network.rest.wpcom.JetpackTunnelWPAPINetwork
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.test

@RunWith(RobolectricTestRunner::class)
class WooExperimentalNetworkTest {
    private val testSite = SiteModel()
    private val testPath = "path"
    private val applicationPasswordsNetwork: ApplicationPasswordsNetwork = mock()
    private val jetpackTunnelWPAPINetwork: JetpackTunnelWPAPINetwork = mock()
    private val applicationPasswordsStore: ApplicationPasswordsStore = mock()
    private val siteSqlUtils: SiteSqlUtils = mock()

    private val sut = WooExperimentalNetwork(
        applicationPasswordsNetwork = applicationPasswordsNetwork,
        jetpackTunnelWPAPINetwork = jetpackTunnelWPAPINetwork,
        applicationPasswordsStore = applicationPasswordsStore,
        siteSqlUtils = siteSqlUtils
    )

    @Test
    fun `given Jetpack site supports app passwords, when making request, then use app passwords network`() = test {
        testSite.origin = SiteModel.ORIGIN_WPCOM_REST
        testSite.applicationPasswordsAuthorizeUrl = "authorize_url"
        val sampleResponse = SampleResponse("value")
        givenAppPasswordsResponse(WPAPIResponse.Success(SampleResponse("value")))

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
            testSite.origin = SiteModel.ORIGIN_WPCOM_REST
            testSite.applicationPasswordsAuthorizeUrl = "authorize_url"
            givenAppPasswordsResponse(WPAPIResponse.Error(WPAPINetworkError(mock(), "error")))
            val sampleResponse = SampleResponse("value")
            givenJetpackTunnelResponse(WPAPIResponse.Success(sampleResponse))

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
            testSite.origin = SiteModel.ORIGIN_WPCOM_REST
            testSite.applicationPasswordsAuthorizeUrl = null
            val sampleResponse = SampleResponse("value")
            givenJetpackTunnelResponse(WPAPIResponse.Success(sampleResponse))

            val response = sut.executeGetGsonRequest(
                site = testSite,
                path = testPath,
                clazz = SampleResponse::class.java
            )

            assertThat((response as WPAPIResponse.Success).data).isEqualTo(sampleResponse)
        }

    @Test
    fun `when detecting that a site doesn't support app passwords, then update cached site with correct status`() =
        test {
            testSite.origin = SiteModel.ORIGIN_WPCOM_REST
            testSite.applicationPasswordsAuthorizeUrl = "authorize_url"
            givenAppPasswordsResponse(
                WPAPIResponse.Error(
                    WPAPINetworkError(
                        mock(),
                        errorCode = ApplicationPasswordsNetwork.APPLICATION_PASSWORDS_NOT_SUPPORT_ERROR_CODE
                    )
                )
            )
            givenJetpackTunnelResponse(
                WPAPIResponse.Success(SampleResponse("value"))
            )

            sut.executeGetGsonRequest(
                site = testSite,
                path = testPath,
                clazz = SampleResponse::class.java
            )

            val expectedSite = testSite.apply {
                applicationPasswordsAuthorizeUrl = null
            }
            verify(siteSqlUtils).insertOrUpdateSite(expectedSite)
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
}
