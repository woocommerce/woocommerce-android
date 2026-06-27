package org.wordpress.android.fluxc.network.rest.wpapi.settings

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
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceWPAPINetwork
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsNetwork
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsStore
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.JetpackApplicationPasswordsErrorHandler
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.JetpackApplicationPasswordsSupport
import org.wordpress.android.fluxc.network.rest.wpcom.JetpackTunnelWPAPINetwork

class WPSettingsRestClientTest {
    private val cookieNonceWPAPINetwork: CookieNonceWPAPINetwork = mock()
    private val applicationPasswordsConfiguration: ApplicationPasswordsConfiguration = mock()
    private val applicationPasswordsNetwork: ApplicationPasswordsNetwork = mock()
    private val jetpackTunnelWPAPINetwork: JetpackTunnelWPAPINetwork = mock()
    private val jetpackApplicationPasswordsSupport: JetpackApplicationPasswordsSupport = mock()
    private val jetpackApplicationPasswordsErrorHandler: JetpackApplicationPasswordsErrorHandler = mock()
    private val applicationPasswordsStore: ApplicationPasswordsStore = mock()

    private lateinit var restClient: WPSettingsRestClient

    @Before
    fun setUp() {
        restClient = WPSettingsRestClient(
            cookieNonceWPAPINetwork = cookieNonceWPAPINetwork,
            applicationPasswordsConfiguration = applicationPasswordsConfiguration,
            applicationPasswordsNetwork = applicationPasswordsNetwork,
            jetpackTunnelWPAPINetwork = jetpackTunnelWPAPINetwork,
            jetpackApplicationPasswordsSupport = jetpackApplicationPasswordsSupport,
            jetpackApplicationPasswordsErrorHandler = jetpackApplicationPasswordsErrorHandler,
            applicationPasswordsStore = applicationPasswordsStore
        )
    }

    @Test
    fun `when settings are fetched, then start of week field is requested from wp settings endpoint`() = runTest {
        givenApplicationPasswordsResponse(WPAPIResponse.Success(SiteSettingsResponse(MONDAY), emptyList()))

        restClient.fetchSiteSettings(site)

        verify(applicationPasswordsNetwork).executeGetGsonRequest(
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
    fun `given integer response, when settings are fetched, then integer is parsed`() = runTest {
        givenApplicationPasswordsResponse(WPAPIResponse.Success(SiteSettingsResponse(MONDAY), emptyList()))

        val response = restClient.fetchSiteSettings(site)

        assertThat((response as WPAPIResponse.Success).data?.startOfWeek).isEqualTo(MONDAY)
    }

    @Test
    fun `given null response, when settings are fetched, then null is preserved`() = runTest {
        givenApplicationPasswordsResponse(WPAPIResponse.Success(SiteSettingsResponse(null), emptyList()))

        val response = restClient.fetchSiteSettings(site)

        assertThat((response as WPAPIResponse.Success).data?.startOfWeek).isNull()
    }

    @Test
    fun `given wpapi error, when settings are fetched, then error is preserved`() = runTest {
        val error = WPAPINetworkError(BaseNetworkError(GenericErrorType.HTTP_AUTH_ERROR))
        givenApplicationPasswordsResponse(WPAPIResponse.Error(error))

        val response = restClient.fetchSiteSettings(site)

        assertThat((response as WPAPIResponse.Error).error).isEqualTo(error)
    }

    private suspend fun givenApplicationPasswordsResponse(response: WPAPIResponse<SiteSettingsResponse>) {
        whenever(applicationPasswordsStore.hasCredentials(site)).thenReturn(true)
        whenever(
            applicationPasswordsNetwork.executeGetGsonRequest(
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
