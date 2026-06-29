package org.wordpress.android.fluxc.network.rest.wpcom.wc.settings

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.generated.endpoint.WPAPI
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork

class WPSettingsRestClientTest {
    private val wooNetwork: WooNetwork = mock()

    private lateinit var restClient: WPSettingsRestClient

    @Before
    fun setUp() {
        restClient = WPSettingsRestClient(wooNetwork)
    }

    @Test
    fun `when settings are fetched, then start of week field is requested from wp settings endpoint`() = runTest {
        givenApplicationPasswordsResponse(WPAPIResponse.Success(SiteSettingsResponse(MONDAY), emptyList()))

        restClient.fetchSiteSettings(site)

        verify(wooNetwork).executeGetGsonRequest(
            site = eq(site),
            path = eq(WPAPI.settings.urlV2),
            clazz = eq(SiteSettingsResponse::class.java),
            params = eq(mapOf("_fields" to "start_of_week")),
            enableCaching = any(),
            cacheTimeToLive = any(),
            forced = any(),
            requestTimeout = any(),
            retries = any()
        )
    }

    @Test
    fun `given wpapi error, when settings are fetched, then error is preserved`() = runTest {
        val error = WPAPINetworkError(BaseNetworkError(GenericErrorType.HTTP_AUTH_ERROR))
        givenApplicationPasswordsResponse(WPAPIResponse.Error(error))

        val response = restClient.fetchSiteSettings(site)

        assertThat((response as WPAPIResponse.Error).error).isEqualTo(error)
    }

    private suspend fun givenApplicationPasswordsResponse(response: WPAPIResponse<SiteSettingsResponse>) {
        whenever(
            wooNetwork.executeGetGsonRequest(
                site = eq(site),
                path = eq(WPAPI.settings.urlV2),
                clazz = eq(SiteSettingsResponse::class.java),
                params = eq(mapOf("_fields" to "start_of_week")),
                enableCaching = any(),
                cacheTimeToLive = any(),
                forced = any(),
                requestTimeout = any(),
                retries = any()
            )
        ).thenReturn(response)
    }

    private companion object {
        const val MONDAY = 1

        val site = SiteModel().apply {
            id = 1
            url = "https://example.com"
        }
    }
}
