package org.wordpress.android.fluxc.network.rest.wpapi.site

import com.android.volley.RequestQueue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.discovery.DiscoveryWPAPIRestClient
import org.wordpress.android.fluxc.network.discovery.RootWPAPIRestResponse
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.test
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SiteWPAPIRestClientTest {
    private val requestBuilder: WPAPIGsonRequestBuilder = mock()
    private val discoveryClient: DiscoveryWPAPIRestClient = mock()
    private val subject = SiteWPAPIRestClient(
        wpapiGsonRequestBuilder = requestBuilder,
        discoveryWPAPIRestClient = discoveryClient,
        dispatcher = mock<Dispatcher>(),
        requestQueue = mock<RequestQueue>(),
        userAgent = mock<UserAgent>()
    )

    @Before
    fun setUp() {
        whenever(discoveryClient.discoverWPAPIBaseURL(SITE_URL)).thenReturn(WP_API_URL)
    }

    @Test
    fun `given stale inferred admin base, when refreshing a WPAPI site, then replace it with fresh REST metadata`() =
        test {
            val existingSite = existingSite().apply {
                adminUrl = STALE_REST_ADMIN_URL.trimEnd('/')
                applicationPasswordsAuthorizeUrl = STALE_REST_ADMIN_AUTHORIZATION_URL
            }
            givenSiteResponse(REST_ADMIN_AUTHORIZATION_URL)

            val refreshedSite = subject.fetchWPAPISite(existingSite)

            assertThat(refreshedSite.adminUrl).isEqualTo(REST_ADMIN_URL)
            assertThat(refreshedSite.applicationPasswordsAuthorizeUrl).isEqualTo(REST_ADMIN_AUTHORIZATION_URL)
        }

    @Test
    fun `given verified admin differs from old inferred base, when refreshing, then preserve verified value`() = test {
        val existingSite = existingSite().apply {
            loginUrl = VERIFIED_LOGIN_URL
            adminUrl = VERIFIED_ADMIN_URL
            applicationPasswordsAuthorizeUrl = STALE_REST_ADMIN_AUTHORIZATION_URL
        }
        givenSiteResponse(REST_ADMIN_AUTHORIZATION_URL)

        val refreshedSite = subject.fetchWPAPISite(existingSite)

        assertThat(refreshedSite.loginUrl).isEqualTo(VERIFIED_LOGIN_URL)
        assertThat(refreshedSite.adminUrl).isEqualTo(VERIFIED_ADMIN_URL)
        assertThat(refreshedSite.applicationPasswordsAuthorizeUrl).isEqualTo(REST_ADMIN_AUTHORIZATION_URL)
    }

    @Test
    fun `given manual admin without old derivation metadata, when refreshing, then preserve manual value`() = test {
        val existingSite = SiteModel().apply {
            url = SITE_URL
            username = USERNAME
            password = PASSWORD
            origin = SiteModel.ORIGIN_WPAPI
            loginUrl = VERIFIED_LOGIN_URL
            adminUrl = VERIFIED_ADMIN_URL
        }
        givenSiteResponse(REST_ADMIN_AUTHORIZATION_URL)

        val refreshedSite = subject.fetchWPAPISite(existingSite)

        assertThat(refreshedSite.loginUrl).isEqualTo(VERIFIED_LOGIN_URL)
        assertThat(refreshedSite.adminUrl).isEqualTo(VERIFIED_ADMIN_URL)
        assertThat(refreshedSite.applicationPasswordsAuthorizeUrl).isEqualTo(REST_ADMIN_AUTHORIZATION_URL)
    }

    @Test
    fun `given no verified admin base, when refreshing a WPAPI site, then retain REST inferred metadata`() = test {
        val existingSite = existingSite()
        givenSiteResponse(REST_ADMIN_AUTHORIZATION_URL)

        val refreshedSite = subject.fetchWPAPISite(existingSite)

        assertThat(refreshedSite.adminUrl).isEqualTo(REST_ADMIN_URL)
    }

    private fun existingSite() = SiteModel().apply {
        url = SITE_URL
        username = USERNAME
        password = PASSWORD
        origin = SiteModel.ORIGIN_WPAPI
    }

    private suspend fun givenSiteResponse(applicationPasswordsAuthorizationUrl: String) {
        val response = RootWPAPIRestResponse(
            authentication = RootWPAPIRestResponse.Authentication(
                applicationPasswords = RootWPAPIRestResponse.Authentication.ApplicationPasswords(
                    endpoints = RootWPAPIRestResponse.Authentication.ApplicationPasswords.Endpoints(
                        authorization = applicationPasswordsAuthorizationUrl
                    )
                )
            ),
            namespaces = listOf("wc/v3")
        )
        whenever(
            requestBuilder.syncGetRequest(
                restClient = subject,
                url = WP_API_URL,
                clazz = RootWPAPIRestResponse::class.java,
                params = mapOf("_fields" to "name,description,gmt_offset,url,authentication,namespaces")
            )
        ).thenReturn(WPAPIResponse.Success(response, emptyList()))
    }

    private companion object {
        const val SITE_URL = "https://site.example"
        const val WP_API_URL = "$SITE_URL/wp-json/"
        const val USERNAME = "merchant"
        const val PASSWORD = "password"
        const val VERIFIED_LOGIN_URL = "$SITE_URL/private-login"
        const val VERIFIED_ADMIN_URL = "$SITE_URL/private-admin/"
        const val STALE_REST_ADMIN_URL = "$SITE_URL/old-wp-admin/"
        const val STALE_REST_ADMIN_AUTHORIZATION_URL = "${STALE_REST_ADMIN_URL}authorize-application.php"
        const val REST_ADMIN_URL = "$SITE_URL/wp-admin/"
        const val REST_ADMIN_AUTHORIZATION_URL = "${REST_ADMIN_URL}authorize-application.php"
    }
}
