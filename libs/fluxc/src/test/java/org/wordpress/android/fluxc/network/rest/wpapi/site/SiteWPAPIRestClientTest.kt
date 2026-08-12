package org.wordpress.android.fluxc.network.rest.wpapi.site

import com.android.volley.RequestQueue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.discovery.DiscoveryWPAPIRestClient
import org.wordpress.android.fluxc.network.discovery.RootWPAPIRestResponse
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.store.SiteStore.FetchWPAPISitePayload
import org.wordpress.android.fluxc.test

@Suppress("UnitTestNamingRule")
@RunWith(RobolectricTestRunner::class)
class SiteWPAPIRestClientTest {
    private val wpapiGsonRequestBuilder: WPAPIGsonRequestBuilder = mock()
    private val discoveryWPAPIRestClient: DiscoveryWPAPIRestClient = mock()
    private val dispatcher: Dispatcher = mock()
    private val requestQueue: RequestQueue = mock()
    private val userAgent: UserAgent = mock()

    private lateinit var restClient: SiteWPAPIRestClient

    @Before
    fun setUp() {
        restClient = SiteWPAPIRestClient(
            wpapiGsonRequestBuilder = wpapiGsonRequestBuilder,
            discoveryWPAPIRestClient = discoveryWPAPIRestClient,
            dispatcher = dispatcher,
            requestQueue = requestQueue,
            userAgent = userAgent
        )
    }

    @Test
    fun `given application passwords and empty namespaces, when fetching site, then return invalid response error`() =
        test {
            initResponse(
                RootWPAPIRestResponse(
                    namespaces = emptyList(),
                    authentication = RootWPAPIRestResponse.Authentication(
                        applicationPasswords = RootWPAPIRestResponse.Authentication.ApplicationPasswords(
                            endpoints = RootWPAPIRestResponse.Authentication.ApplicationPasswords.Endpoints(
                                authorization = APPLICATION_PASSWORDS_URL
                            )
                        )
                    )
                )
            )

            val site = restClient.fetchWPAPISite(FetchWPAPISitePayload(SITE_URL))

            assertThat(site.isError).isTrue()
            assertThat(site.error.type).isEqualTo(INVALID_RESPONSE)
            assertThat(site.applicationPasswordsAuthorizeUrl).isNull()
        }

    @Test
    fun `given missing namespaces, when fetching site, then return invalid response error`() = test {
        initResponse(RootWPAPIRestResponse(namespaces = null))

        val site = restClient.fetchWPAPISite(FetchWPAPISitePayload(SITE_URL))

        assertThat(site.isError).isTrue()
        assertThat(site.error.type).isEqualTo(INVALID_RESPONSE)
    }

    @Test
    fun `given non-empty namespaces without WooCommerce, when fetching site, then return non-Woo site`() = test {
        initResponse(RootWPAPIRestResponse(namespaces = listOf("wp/v2")))

        val site = restClient.fetchWPAPISite(FetchWPAPISitePayload(SITE_URL))

        assertThat(site.isError).isFalse()
        assertThat(site.hasWooCommerce).isFalse()
    }

    private suspend fun initResponse(response: RootWPAPIRestResponse) {
        whenever(discoveryWPAPIRestClient.discoverWPAPIBaseURL(SITE_URL)).thenReturn(WP_API_URL)
        whenever(
            wpapiGsonRequestBuilder.syncGetRequest(
                restClient = restClient,
                url = WP_API_URL,
                clazz = RootWPAPIRestResponse::class.java,
                params = mapOf("_fields" to FETCH_API_CALL_FIELDS)
            )
        ).thenReturn(Success(response, emptyList()))
    }

    companion object {
        private const val SITE_URL = "https://example.com"
        private const val WP_API_URL = "$SITE_URL/wp-json/"
        private const val APPLICATION_PASSWORDS_URL = "$SITE_URL/wp-admin/authorize-application.php"
        private const val FETCH_API_CALL_FIELDS =
            "name,description,gmt_offset,url,authentication,namespaces"
    }
}
