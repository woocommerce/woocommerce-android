package org.wordpress.android.fluxc.network.rest.wpcom.site

import com.android.volley.NoConnectionError
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.discovery.DiscoveryWPAPIRestClient
import org.wordpress.android.fluxc.network.discovery.RootWPAPIRestResponse
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteWPComRestResponse.SitesResponse
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import org.wordpress.android.fluxc.store.SiteStore.SiteFilter.WPCOM
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.util.UrlUtils
import java.net.UnknownHostException
import java.security.cert.CertificateException
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import javax.net.ssl.SSLHandshakeException
import kotlin.test.assertNotNull

@Suppress("UnitTestNamingRule")
@RunWith(RobolectricTestRunner::class)
class SiteRestClientTest {
    private val dispatcher: Dispatcher = mock()

    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder = mock()

    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder = mock()

    private val discoveryWPAPIRestClient: DiscoveryWPAPIRestClient = mock()

    private val requestQueue: RequestQueue = mock()

    private val accessToken: AccessToken = mock()

    private val userAgent: UserAgent = mock()

    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var restClient: SiteRestClient

    private val siteId: Long = 12
    private val site = SiteModel().apply {
        siteId = this@SiteRestClientTest.siteId
        setIsJetpackConnected(true)
    }

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        restClient = SiteRestClient(
            appContext = null,
            dispatcher = dispatcher,
            requestQueue = requestQueue,
            wpComGsonRequestBuilder = wpComGsonRequestBuilder,
            jetpackTunnelGsonRequestBuilder = jetpackTunnelGsonRequestBuilder,
            coroutineEngine = initCoroutineEngine(),
            discoveryWPAPIRestClient = discoveryWPAPIRestClient,
            accessToken = accessToken,
            userAgent = userAgent
        )
    }

    @Test
    fun `returns fetched site`() = test {
        val response = SiteWPComRestResponse()
        response.ID = siteId
        val name = "Updated name"
        response.name = name
        response.URL = "site.com"

        initSiteResponse(response)

        val responseModel = restClient.fetchSite(site)
        assertThat(responseModel.name).isEqualTo(name)
        assertThat(responseModel.siteId).isEqualTo(siteId)
        assertThat(urlCaptor.lastValue)
            .isEqualTo("https://public-api.wordpress.com/rest/v1.1/sites/12")
        assertThat(paramsCaptor.lastValue).isEqualTo(mapOf("fields" to SiteRestClient.SITE_FIELDS))
    }

    @Test
    fun `fetchSite returns error when API call fails`() = test {
        val errorMessage = "message"
        initSiteResponse(
            error = WPComGsonNetworkError(
                BaseNetworkError(
                    GenericErrorType.NETWORK_ERROR,
                    errorMessage,
                    VolleyError(errorMessage)
                )
            )
        )
        val errorResponse = restClient.fetchSite(site)

        assertNotNull(errorResponse.error)
        assertThat(errorResponse.error.type).isEqualTo(GenericErrorType.NETWORK_ERROR)
        assertThat(errorResponse.error.message).isEqualTo(errorMessage)
    }

    @Test
    fun `returns fetched sites using filter`() = test {
        val response = SiteWPComRestResponse()
        response.ID = siteId
        val name = "Updated name"
        response.name = name
        response.URL = "site.com"

        val sitesResponse = SitesResponse()
        sitesResponse.sites = listOf(response)

        initSitesResponse(data = sitesResponse)
        initSitesFeaturesResponse(data = SitesFeaturesRestResponse(emptyMap()))

        val responseModel = restClient.fetchSites(listOf(WPCOM), false)
        assertThat(responseModel.sites).hasSize(1)
        assertThat(responseModel.sites[0].name).isEqualTo(name)
        assertThat(responseModel.sites[0].siteId).isEqualTo(siteId)

        assertThat(urlCaptor.firstValue)
            .isEqualTo("https://public-api.wordpress.com/rest/v1.2/me/sites/")
        assertThat(urlCaptor.lastValue)
            .isEqualTo("https://public-api.wordpress.com/rest/v1.1/me/sites/features/")
        assertThat(paramsCaptor.firstValue).isEqualTo(
            mapOf(
                "filters" to "wpcom",
                "fields" to SiteRestClient.SITE_FIELDS
            )
        )
    }

    @Test
    fun `returns fetched sites when not filtering`() = test {
        val response = SiteWPComRestResponse()
        response.ID = siteId
        val name = "Updated name"
        response.name = name
        response.URL = "site.com"

        val sitesResponse = SitesResponse()
        sitesResponse.sites = listOf(response)

        initSitesResponse(data = sitesResponse)

        val responseModel = restClient.fetchSites(emptyList(), false)
        assertThat(responseModel.sites).hasSize(1)
        assertThat(responseModel.sites[0].name).isEqualTo(name)
        assertThat(responseModel.sites[0].siteId).isEqualTo(siteId)

        assertThat(urlCaptor.firstValue)
            .isEqualTo("https://public-api.wordpress.com/rest/v1.1/me/sites/")
        assertThat(paramsCaptor.firstValue).isEqualTo(mapOf("fields" to SiteRestClient.SITE_FIELDS))
    }

    @Test
    fun `fetched sites can filter JP connected package sites`() = test {
        val response = SiteWPComRestResponse()
        response.ID = siteId
        val name = "Updated name"
        response.name = name
        response.URL = "site.com"
        response.jetpack = false
        response.jetpack_connection = true

        val sitesResponse = SitesResponse()
        sitesResponse.sites = listOf(response)

        initRootEndpointResponse()
        initSitesResponse(data = sitesResponse)
        initSitesFeaturesResponse(data = SitesFeaturesRestResponse(features = emptyMap()))

        val responseModel = restClient.fetchSites(listOf(WPCOM), true)

        assertThat(responseModel.sites).hasSize(0)
    }

    @Test
    fun `fetchSites returns error when API call fails`() = test {
        val errorMessage = "message"
        initSitesResponse(
            error = WPComGsonNetworkError(
                BaseNetworkError(
                    GenericErrorType.NETWORK_ERROR,
                    errorMessage,
                    VolleyError(errorMessage)
                )
            )
        )
        val errorResponse = restClient.fetchSites(listOf(), false)

        assertNotNull(errorResponse.error)
        assertThat(errorResponse.error.type).isEqualTo(GenericErrorType.NETWORK_ERROR)
        assertThat(errorResponse.error.message).isEqualTo(errorMessage)
    }

    @Test
    fun `given a suspended WPCom website, when fetching site info, then return correct error`() = test {
        val urlUtilsMock = mockStatic(UrlUtils::class.java)
        try {
            whenever(UrlUtils.addUrlSchemeIfNeeded(any(), any())).thenAnswer { it.arguments[0] as String }
            val error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.INVALID_RESPONSE, "")).apply {
                apiError = "connection_disabled"
            }
            initGetResponse(ConnectSiteInfoResponse::class.java, null, error)

            val result = restClient.fetchConnectSiteInfoSync("test.com", discoverWPAPIOnFailure = true)

            assertThat(result.error).isNotNull
            assertThat(result.error!!.type).isEqualTo(SiteErrorType.WPCOM_SITE_SUSPENDED)
            assertThat(result.error!!.message).isNotNull
            assertThat(result.error!!.wpApiDiscovery).isNull()
            verifyNoInteractions(discoveryWPAPIRestClient)
        } finally {
            urlUtilsMock.close()
        }
    }

    @Test
    fun `given malformed URL, when fetching site info, then return invalid site error`() = test {
        val urlUtilsMock = mockStatic(UrlUtils::class.java)
        try {
            whenever(UrlUtils.addUrlSchemeIfNeeded(any(), any())).thenReturn("https://[")

            val result = restClient.fetchConnectSiteInfoSync("https://[")

            assertThat(result.error).isNotNull
            assertThat(result.error!!.type).isEqualTo(SiteErrorType.INVALID_SITE)
            assertThat(result.error!!.wpApiDiscovery).isNull()
            verifyNoInteractions(discoveryWPAPIRestClient)
        } finally {
            urlUtilsMock.close()
        }
    }

    @Test
    fun `given ordinary site info error, when fetching site info, then WP API discovery state is attached`() = test {
        val urlUtilsMock = mockStatic(UrlUtils::class.java)
        try {
            whenever(UrlUtils.addUrlSchemeIfNeeded(any(), any())).thenAnswer { it.arguments[0] as String }
            val error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.INVALID_RESPONSE, "origin failed")).apply {
                apiError = "origin_failed"
            }
            initGetResponse(ConnectSiteInfoResponse::class.java, null, error)

            val result = restClient.fetchConnectSiteInfoSync("test.com", discoverWPAPIOnFailure = true)

            assertThat(result.error).isNotNull
            assertThat(result.error!!.type).isEqualTo(SiteErrorType.INVALID_SITE)
            assertThat(result.error!!.message).contains("origin failed")
            assertThat(result.error!!.wpApiDiscovery).isNotNull
            assertThat(result.error!!.wpApiDiscovery!!.connectSiteInfoApiError).isEqualTo("origin_failed")
        } finally {
            urlUtilsMock.close()
        }
    }

    @Test
    fun `given follow redirects failed certificate error, when fetching site info, then return remote certificate error`() =
        test {
            val error = WPComGsonNetworkError(
                BaseNetworkError(
                    GenericErrorType.INVALID_RESPONSE,
                    "cURL error 60: SSL certificate problem: certificate has expired"
                )
            ).apply {
                apiError = "follow_redirects_failed"
            }

            val result = fetchConnectSiteInfoWithError(error, discoverWPAPIOnFailure = true)

            assertThat(result.error).isNotNull
            assertThat(result.error!!.type).isEqualTo(SiteErrorType.REMOTE_SITE_CERTIFICATE_ERROR)
            assertThat(result.error!!.wpApiDiscovery).isNull()
            verifyNoInteractions(discoveryWPAPIRestClient)
        }

    @Test
    fun `given follow redirects failed non-certificate error, when fetching site info, then return invalid site error`() =
        test {
            val error = WPComGsonNetworkError(
                BaseNetworkError(
                    GenericErrorType.INVALID_RESPONSE,
                    "follow_redirects_failed: exceeded maximum number of redirects"
                )
            ).apply {
                apiError = "follow_redirects_failed"
            }

            val result = fetchConnectSiteInfoWithError(error, discoverWPAPIOnFailure = true)

            assertThat(result.error).isNotNull
            assertThat(result.error!!.type).isEqualTo(SiteErrorType.INVALID_SITE)
            assertThat(result.error!!.wpApiDiscovery).isNotNull
            assertThat(result.error!!.wpApiDiscovery!!.connectSiteInfoApiError).isEqualTo("follow_redirects_failed")
        }

    @Test
    fun `given follow redirects failed timeout, when fetching site info, then WP API discovery state is attached`() =
        test {
            val error = WPComGsonNetworkError(
                BaseNetworkError(
                    GenericErrorType.INVALID_RESPONSE,
                    "cURL error 28: Operation timed out"
                )
            ).apply {
                apiError = "follow_redirects_failed"
            }

            val result = fetchConnectSiteInfoWithError(error, discoverWPAPIOnFailure = true)

            assertThat(result.error).isNotNull
            assertThat(result.error!!.type).isEqualTo(SiteErrorType.INVALID_SITE)
            assertThat(result.error!!.wpApiDiscovery).isNotNull
            assertThat(result.error!!.wpApiDiscovery!!.connectSiteInfoApiError).isEqualTo("follow_redirects_failed")
        }

    @Test
    fun `given eligible site info error and successful probe, when fetching site info, then WP API base URL is attached`() =
        test {
            val error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.INVALID_RESPONSE, "origin failed"))
            whenever(discoveryWPAPIRestClient.discoverWPAPIBaseURL("test.com")).thenReturn("https://test.com/wp-json/")
            whenever(discoveryWPAPIRestClient.verifyWPAPIV2Support("https://test.com/wp-json/"))
                .thenReturn("https://test.com/wp-json/")

            val result = fetchConnectSiteInfoWithError(error, discoverWPAPIOnFailure = true)

            assertThat(result.error!!.wpApiDiscovery!!.wpApiBaseUrl).isEqualTo("https://test.com/wp-json/")
            verify(discoveryWPAPIRestClient).discoverWPAPIBaseURL("test.com")
            verify(discoveryWPAPIRestClient).verifyWPAPIV2Support("https://test.com/wp-json/")
        }

    @Test
    fun `given eligible site info error and missing link header, when fetching site info, then WP API base URL is null`() =
        test {
            val error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.INVALID_RESPONSE, "origin failed"))
            whenever(discoveryWPAPIRestClient.discoverWPAPIBaseURL("test.com")).thenReturn(null)

            val result = fetchConnectSiteInfoWithError(error, discoverWPAPIOnFailure = true)

            assertThat(result.error!!.wpApiDiscovery).isNotNull
            assertThat(result.error!!.wpApiDiscovery!!.wpApiBaseUrl).isNull()
            verify(discoveryWPAPIRestClient).discoverWPAPIBaseURL("test.com")
        }

    @Test
    fun `given eligible site info error and unsupported WP API, when fetching site info, then WP API base URL is null`() =
        test {
            val error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.INVALID_RESPONSE, "origin failed"))
            whenever(discoveryWPAPIRestClient.discoverWPAPIBaseURL("test.com")).thenReturn("https://test.com/wp-json/")
            whenever(discoveryWPAPIRestClient.verifyWPAPIV2Support("https://test.com/wp-json/")).thenReturn(null)

            val result = fetchConnectSiteInfoWithError(error, discoverWPAPIOnFailure = true)

            assertThat(result.error!!.wpApiDiscovery).isNotNull
            assertThat(result.error!!.wpApiDiscovery!!.wpApiBaseUrl).isNull()
            verify(discoveryWPAPIRestClient).discoverWPAPIBaseURL("test.com")
            verify(discoveryWPAPIRestClient).verifyWPAPIV2Support("https://test.com/wp-json/")
        }

    @Test
    fun `given eligible site info error and discovery opt-out, when fetching site info, then discovery is not attempted`() =
        test {
            val error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.INVALID_RESPONSE, "origin failed"))

            val result = fetchConnectSiteInfoWithError(error, discoverWPAPIOnFailure = false)

            assertThat(result.error!!.wpApiDiscovery).isNull()
            verifyNoInteractions(discoveryWPAPIRestClient)
        }

    @Test
    fun `given non-date SSL site info error, when fetching site info, then return invalid site error`() = test {
        val urlUtilsMock = mockStatic(UrlUtils::class.java)
        try {
            whenever(UrlUtils.addUrlSchemeIfNeeded(any(), any())).thenAnswer { it.arguments[0] as String }
            val sslException = SSLHandshakeException("self signed certificate").apply {
                initCause(CertificateException("self signed"))
            }
            val error = WPComGsonNetworkError(
                BaseNetworkError(
                    GenericErrorType.INVALID_SSL_CERTIFICATE,
                    VolleyError(sslException)
                )
            )
            initGetResponse(ConnectSiteInfoResponse::class.java, null, error)

            val result = restClient.fetchConnectSiteInfoSync("test.com", discoverWPAPIOnFailure = true)

            assertThat(result.error).isNotNull
            assertThat(result.error!!.type).isEqualTo(SiteErrorType.INVALID_SITE)
        } finally {
            urlUtilsMock.close()
        }
    }

    @Test
    fun `given expired SSL certificate site info error, when fetching site info, then return certificate validity error`() =
        test {
            val sslException = SSLHandshakeException("certificate expired").apply {
                initCause(CertificateExpiredException("expired"))
            }

            val result = fetchConnectSiteInfoWithInvalidSslError(sslException, discoverWPAPIOnFailure = true)

            assertThat(result.error).isNotNull
            assertThat(result.error!!.type).isEqualTo(SiteErrorType.TLS_CERTIFICATE_VALIDITY_ERROR)
            assertThat(result.error!!.wpApiDiscovery).isNull()
            verifyNoInteractions(discoveryWPAPIRestClient)
        }

    @Test
    fun `given not yet valid SSL certificate site info error, when fetching site info, then return certificate validity error`() =
        test {
            val sslException = SSLHandshakeException("certificate not yet valid").apply {
                initCause(CertificateNotYetValidException("not yet valid"))
            }

            val result = fetchConnectSiteInfoWithInvalidSslError(sslException, discoverWPAPIOnFailure = true)

            assertThat(result.error).isNotNull
            assertThat(result.error!!.type).isEqualTo(SiteErrorType.TLS_CERTIFICATE_VALIDITY_ERROR)
            assertThat(result.error!!.wpApiDiscovery).isNull()
            verifyNoInteractions(discoveryWPAPIRestClient)
        }

    @Test
    fun `given message-only SSL certificate validity site info error, when fetching site info, then return certificate validity error`() =
        test {
            val result = fetchConnectSiteInfoWithInvalidSslError(
                SSLHandshakeException("certificate has expired"),
                discoverWPAPIOnFailure = true
            )

            assertThat(result.error).isNotNull
            assertThat(result.error!!.type).isEqualTo(SiteErrorType.TLS_CERTIFICATE_VALIDITY_ERROR)
            assertThat(result.error!!.wpApiDiscovery).isNull()
            verifyNoInteractions(discoveryWPAPIRestClient)
        }

    @Test
    fun `given network error with SSL certificate validity cause, when fetching site info, then return certificate validity error`() =
        test {
            val sslException = SSLHandshakeException("certificate expired").apply {
                initCause(CertificateExpiredException("expired"))
            }
            val error = WPComGsonNetworkError(
                BaseNetworkError(
                    GenericErrorType.NETWORK_ERROR,
                    VolleyError(sslException)
                )
            )

            val result = fetchConnectSiteInfoWithError(error, discoverWPAPIOnFailure = true)

            assertThat(result.error).isNotNull
            assertThat(result.error!!.type).isEqualTo(SiteErrorType.TLS_CERTIFICATE_VALIDITY_ERROR)
            assertThat(result.error!!.wpApiDiscovery).isNull()
            verifyNoInteractions(discoveryWPAPIRestClient)
        }

    @Test
    fun `given Android no connection certificate validity error, when fetching site info, then return certificate validity error`() =
        test {
            val certificateException = CertificateException("Unacceptable certificate: CN=E8").apply {
                stackTrace = arrayOf(
                    StackTraceElement(
                        "com.android.org.conscrypt.OpenSSLX509Certificate",
                        "checkValidity",
                        "OpenSSLX509Certificate.java",
                        266
                    )
                )
            }
            val sslException = SSLHandshakeException("Unacceptable certificate").apply {
                initCause(certificateException)
            }
            val volleyError = NoConnectionError(sslException)
            val error = WPComGsonNetworkError(
                BaseNetworkError(
                    GenericErrorType.NO_CONNECTION,
                    volleyError
                )
            )

            val result = fetchConnectSiteInfoWithError(error, discoverWPAPIOnFailure = true)

            assertThat(result.error).isNotNull
            assertThat(result.error!!.type).isEqualTo(SiteErrorType.TLS_CERTIFICATE_VALIDITY_ERROR)
            assertThat(result.error!!.wpApiDiscovery).isNull()
            verifyNoInteractions(discoveryWPAPIRestClient)
        }

    @Test
    fun `given WordPress com no connection error, when fetching site info, then return connectivity error`() =
        test {
            val error = WPComGsonNetworkError(
                BaseNetworkError(
                    GenericErrorType.NO_CONNECTION,
                    VolleyError(UnknownHostException("public-api.wordpress.com"))
                )
            )

            val result = fetchConnectSiteInfoWithError(error, discoverWPAPIOnFailure = true)

            assertThat(result.error).isNotNull
            assertThat(result.error!!.type).isEqualTo(SiteErrorType.WORDPRESS_COM_CONNECTIVITY_ERROR)
            assertThat(result.error!!.wpApiDiscovery).isNull()
            verifyNoInteractions(discoveryWPAPIRestClient)
        }

    @Test
    fun `given WordPress com network error, when fetching site info, then return connectivity error`() =
        test {
            val error = WPComGsonNetworkError(
                BaseNetworkError(
                    GenericErrorType.NETWORK_ERROR,
                    VolleyError("Failed to connect to public-api.wordpress.com")
                )
            )

            val result = fetchConnectSiteInfoWithError(error, discoverWPAPIOnFailure = true)

            assertThat(result.error).isNotNull
            assertThat(result.error!!.type).isEqualTo(SiteErrorType.WORDPRESS_COM_CONNECTIVITY_ERROR)
            assertThat(result.error!!.wpApiDiscovery).isNull()
            verifyNoInteractions(discoveryWPAPIRestClient)
        }

    @Test
    fun `given WordPress com timeout error, when fetching site info, then return connectivity error`() =
        test {
            val error = WPComGsonNetworkError(
                BaseNetworkError(
                    GenericErrorType.TIMEOUT,
                    VolleyError("Timed out connecting to public-api.wordpress.com")
                )
            )

            val result = fetchConnectSiteInfoWithError(error, discoverWPAPIOnFailure = true)

            assertThat(result.error).isNotNull
            assertThat(result.error!!.type).isEqualTo(SiteErrorType.WORDPRESS_COM_CONNECTIVITY_ERROR)
            assertThat(result.error!!.wpApiDiscovery).isNull()
            verifyNoInteractions(discoveryWPAPIRestClient)
        }

    @Test
    fun `given a Jetpack site, when fetching site info, then fetch app passwords url from root endpoint`() = test {
        initSiteResponse(
            SiteWPComRestResponse().apply {
                ID = siteId
                URL = "site.com"
                jetpack = true
                jetpack_connection = true
            }
        )
        val response = RootWPAPIRestResponse(
            authentication = RootWPAPIRestResponse.Authentication(
                applicationPasswords = RootWPAPIRestResponse.Authentication.ApplicationPasswords(
                    endpoints = RootWPAPIRestResponse.Authentication.ApplicationPasswords.Endpoints(
                        authorization = "https://test.com/application-passwords"
                    )
                )
            )
        )

        initRootEndpointResponse(response)

        val result = restClient.fetchSite(site)

        assertThat(result.applicationPasswordsAuthorizeUrl).isEqualTo(
            response.authentication?.applicationPasswords?.endpoints?.authorization
        )
    }

    @Test
    fun `given a Jetpack CP site, when fetching site info, then use root endpoint`() = test {
        val jetpackCPSite = SiteModel().apply {
            setIsJetpackConnected(false)
            setIsJetpackCPConnected(false)
        }
        val response = RootWPAPIRestResponse(
            name = "Test site",
            description = "Description",
            authentication = RootWPAPIRestResponse.Authentication(
                applicationPasswords = RootWPAPIRestResponse.Authentication.ApplicationPasswords(
                    endpoints = RootWPAPIRestResponse.Authentication.ApplicationPasswords.Endpoints(
                        authorization = "https://test.com/application-passwords"
                    )
                )
            )
        )

        initRootEndpointResponse(response)

        val result = restClient.fetchSite(jetpackCPSite)

        assertThat(result.name).isEqualTo(response.name)
        assertThat(result.applicationPasswordsAuthorizeUrl).isEqualTo(
            response.authentication?.applicationPasswords?.endpoints?.authorization
        )
    }

    @Test
    fun `given Jetpack CP sites, when fetching sites, then use root endpoint for additional data`() = test {
        val wpComSiteResponse = SiteWPComRestResponse()
        wpComSiteResponse.ID = siteId
        val name = "Test site"
        wpComSiteResponse.name = name
        wpComSiteResponse.URL = "site.com"
        wpComSiteResponse.jetpack = false
        wpComSiteResponse.jetpack_connection = true

        val sitesResponse = SitesResponse()
        sitesResponse.sites = listOf(wpComSiteResponse)
        initSitesResponse(sitesResponse)

        val rootResponse = RootWPAPIRestResponse(
            name = "Updated name",
            description = "Description",
            authentication = RootWPAPIRestResponse.Authentication(
                applicationPasswords = RootWPAPIRestResponse.Authentication.ApplicationPasswords(
                    endpoints = RootWPAPIRestResponse.Authentication.ApplicationPasswords.Endpoints(
                        authorization = "https://test.com/application-passwords"
                    )
                )
            )
        )
        initRootEndpointResponse(rootResponse)

        val result = restClient.fetchSites(emptyList(), false)

        assertThat(result.sites).hasSize(1)
        val site = result.sites[0]
        assertThat(site.name).isEqualTo(rootResponse.name)
        assertThat(site.applicationPasswordsAuthorizeUrl).isEqualTo(
            rootResponse.authentication?.applicationPasswords?.endpoints?.authorization
        )
    }

    @Test
    fun `given CIAB site with woocommerce inactive, when fetching site, then hasWooCommerce is true`() = test {
        val response = SiteWPComRestResponse().apply {
            ID = siteId
            URL = "site.com"
            options = SiteWPComRestResponse.Options().apply {
                woocommerce_is_active = false
            }
            is_garden = true
            garden_name = SiteModel.CIAB_GARDEN_NAME
        }

        initSiteResponse(response)

        val responseModel = restClient.fetchSite(site)
        assertThat(responseModel.hasWooCommerce).isTrue()
    }

    @Test
    fun `given non-CIAB garden site with woocommerce inactive, when fetching site, then hasWooCommerce is false`() =
        test {
            val response = SiteWPComRestResponse().apply {
                ID = siteId
                URL = "site.com"
                options = SiteWPComRestResponse.Options().apply {
                    woocommerce_is_active = false
                }
                is_garden = true
                garden_name = "other"
            }

            initSiteResponse(response)

            val responseModel = restClient.fetchSite(site)
            assertThat(responseModel.hasWooCommerce).isFalse()
        }

    private suspend fun initSiteResponse(
        data: SiteWPComRestResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<SiteWPComRestResponse> {
        return initGetResponse(SiteWPComRestResponse::class.java, data ?: mock(), error)
    }

    private suspend fun initSitesResponse(
        data: SitesResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<SitesResponse> {
        return initGetResponse(SitesResponse::class.java, data ?: mock(), error)
    }

    private suspend fun initSitesFeaturesResponse(
        data: SitesFeaturesRestResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<SitesFeaturesRestResponse> {
        return initGetResponse(
            clazz = SitesFeaturesRestResponse::class.java,
            data = data ?: SitesFeaturesRestResponse(emptyMap()),
            error = error
        )
    }

    private suspend fun initRootEndpointResponse(
        data: RootWPAPIRestResponse = RootWPAPIRestResponse(),
        error: WPComGsonNetworkError? = null
    ): JetpackResponse<RootWPAPIRestResponse> {
        val response = if (error != null) {
            JetpackResponse.JetpackError(error)
        } else {
            JetpackResponse.JetpackSuccess(data, emptyList())
        }

        whenever(
            jetpackTunnelGsonRequestBuilder.syncGetRequest(
                restClient = eq(restClient),
                site = any(),
                url = eq("/"),
                params = any(),
                clazz = eq(RootWPAPIRestResponse::class.java),
                enableCaching = any(),
                cacheTimeToLive = any(),
                forced = any(),
                retryPolicy = anyOrNull()
            )
        ).thenReturn(response)

        return response
    }

    private suspend fun <T> initGetResponse(
        clazz: Class<T>,
        data: T?,
        error: WPComGsonNetworkError? = null
    ): Response<T> {
        if (error == null && data == null) {
            error("Either data or error must be provided")
        }
        val response = if (error != null) Response.Error(error) else Success<T>(data!!, emptyList())
        whenever(
            wpComGsonRequestBuilder.syncGetRequest(
                eq(restClient),
                urlCaptor.capture(),
                paramsCaptor.capture(),
                eq(clazz),
                any(),
                any(),
                any(),
                customGsonBuilder = anyOrNull(),
                authenticatedRequest = any()
            )
        ).thenReturn(response)
        return response
    }

    private suspend fun fetchConnectSiteInfoWithInvalidSslError(
        sslException: SSLHandshakeException,
        discoverWPAPIOnFailure: Boolean = false
    ): ConnectSiteInfoPayload {
        val error = WPComGsonNetworkError(
            BaseNetworkError(
                GenericErrorType.INVALID_SSL_CERTIFICATE,
                VolleyError(sslException)
            )
        )
        return fetchConnectSiteInfoWithError(error, discoverWPAPIOnFailure)
    }

    private suspend fun fetchConnectSiteInfoWithError(
        error: WPComGsonNetworkError,
        discoverWPAPIOnFailure: Boolean = false
    ): ConnectSiteInfoPayload {
        val urlUtilsMock = mockStatic(UrlUtils::class.java)
        try {
            whenever(UrlUtils.addUrlSchemeIfNeeded(any(), any())).thenAnswer { it.arguments[0] as String }
            initGetResponse(ConnectSiteInfoResponse::class.java, null, error)

            return restClient.fetchConnectSiteInfoSync("test.com", discoverWPAPIOnFailure)
        } finally {
            urlUtilsMock.close()
        }
    }
}
