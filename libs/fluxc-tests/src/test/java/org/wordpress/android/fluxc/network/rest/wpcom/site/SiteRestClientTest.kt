package org.wordpress.android.fluxc.network.rest.wpcom.site

import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mockStatic
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.discovery.RootWPAPIRestResponse
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse
import org.wordpress.android.fluxc.network.rest.wpcom.site.NewSiteResponse.BlogDetails
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteWPComRestResponse.SitesResponse
import org.wordpress.android.fluxc.network.rest.wpcom.site.StatusType.ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.site.StatusType.SUCCESS
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import org.wordpress.android.fluxc.store.SiteStore.SiteFilter.WPCOM
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility.COMING_SOON
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility.PUBLIC
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.UrlUtils
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class SiteRestClientTest {
    @Mock
    private lateinit var dispatcher: Dispatcher

    @Mock
    private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder

    @Mock
    private lateinit var jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder

    @Mock
    private lateinit var requestQueue: RequestQueue

    @Mock
    private lateinit var accessToken: AccessToken

    @Mock
    private lateinit var userAgent: UserAgent

    @Mock
    private lateinit var appSecrets: AppSecrets
    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var bodyCaptor: KArgumentCaptor<Map<String, Any>>
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
        bodyCaptor = argumentCaptor()
        restClient = SiteRestClient(
            appContext = null,
            dispatcher = dispatcher,
            requestQueue = requestQueue,
            appSecrets = appSecrets,
            wpComGsonRequestBuilder = wpComGsonRequestBuilder,
            jetpackTunnelGsonRequestBuilder = jetpackTunnelGsonRequestBuilder,
            coroutineEngine = initCoroutineEngine(),
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
    fun `creates new site with all params`() = test {
        val data = NewSiteResponse()
        val blogDetails = BlogDetails()
        blogDetails.blogid = siteId.toString()
        data.blog_details = blogDetails
        val appId = "app_id"
        whenever(appSecrets.appId).thenReturn(appId)
        val appSecret = "app_secret"
        whenever(appSecrets.appSecret).thenReturn(appSecret)

        initNewSiteResponse(data)

        val dryRun = false
        val siteName = "Site name"
        val siteTitle = "site title"
        val language = "CZ"
        val visibility = PUBLIC
        val segmentId = 123L
        val siteDesign = "design"
        val timeZoneId = "Europe/London"
        val findAvailableUrl = true

        val result = restClient.newSite(
            siteName,
            siteTitle,
            language,
            timeZoneId,
            visibility,
            segmentId,
            siteDesign,
            findAvailableUrl,
            dryRun
        )

        assertThat(result.newSiteRemoteId).isEqualTo(siteId)
        assertThat(result.dryRun).isEqualTo(dryRun)

        assertThat(urlCaptor.lastValue)
            .isEqualTo("https://public-api.wordpress.com/rest/v1.1/sites/new/")
        assertThat(bodyCaptor.lastValue).isEqualTo(
            mapOf(
                "blog_name" to siteName,
                "blog_title" to siteTitle,
                "lang_id" to language,
                "public" to "1",
                "validate" to "0",
                "find_available_url" to findAvailableUrl.toString(),
                "client_id" to appId,
                "client_secret" to appSecret,
                "options" to mapOf<String, Any>(
                    "site_segment" to segmentId,
                    "template" to siteDesign,
                    "timezone_string" to timeZoneId,
                )
            )
        )
    }

    @Test
    fun `creates new site without a site name`() = test {
        val data = NewSiteResponse()
        val blogDetails = BlogDetails()
        blogDetails.blogid = siteId.toString()
        data.blog_details = blogDetails
        val appId = "app_id"
        whenever(appSecrets.appId).thenReturn(appId)
        val appSecret = "app_secret"
        whenever(appSecrets.appSecret).thenReturn(appSecret)

        initNewSiteResponse(data)

        val dryRun = false
        val siteName = null
        val siteTitle = "site title"
        val language = "CZ"
        val visibility = PUBLIC
        val segmentId = 123L
        val siteDesign = "design"
        val timeZoneId = "Europe/London"

        val result = restClient.newSite(
            siteName,
            siteTitle,
            language,
            timeZoneId,
            visibility,
            segmentId,
            siteDesign,
            null,
            dryRun
        )

        assertThat(result.newSiteRemoteId).isEqualTo(siteId)
        assertThat(result.dryRun).isEqualTo(dryRun)

        assertThat(urlCaptor.lastValue)
            .isEqualTo("https://public-api.wordpress.com/rest/v1.1/sites/new/")
        assertThat(bodyCaptor.lastValue).isEqualTo(
            mapOf(
                "blog_name" to siteTitle,
                "blog_title" to siteTitle,
                "lang_id" to language,
                "public" to "1",
                "validate" to "0",
                "find_available_url" to "1",
                "client_id" to appId,
                "client_secret" to appSecret,
                "options" to mapOf<String, Any>(
                    "site_segment" to segmentId,
                    "template" to siteDesign,
                    "site_creation_flow" to "with-design-picker",
                    "timezone_string" to timeZoneId,
                )
            )
        )
    }

    @Test
    fun `creates new site without a site name and site title`() = test {
        val data = NewSiteResponse()
        val blogDetails = BlogDetails()
        blogDetails.blogid = siteId.toString()
        data.blog_details = blogDetails
        val appId = "app_id"
        whenever(appSecrets.appId).thenReturn(appId)
        val appSecret = "app_secret"
        whenever(appSecrets.appSecret).thenReturn(appSecret)

        initNewSiteResponse(data)

        val dryRun = false
        val siteName = null
        val siteTitle = null
        val language = "CZ"
        val visibility = PUBLIC
        val segmentId = 123L
        val siteDesign = "design"
        val timeZoneId = "Europe/London"

        val result = restClient.newSite(
            siteName,
            siteTitle,
            language,
            timeZoneId,
            visibility,
            segmentId,
            siteDesign,
            null,
            dryRun
        )

        assertThat(result.newSiteRemoteId).isEqualTo(siteId)
        assertThat(result.dryRun).isEqualTo(dryRun)

        assertThat(urlCaptor.lastValue)
            .isEqualTo("https://public-api.wordpress.com/rest/v1.1/sites/new/")
        assertThat(bodyCaptor.lastValue).isEqualTo(
            mapOf(
                "blog_name" to "",
                "lang_id" to language,
                "public" to "1",
                "validate" to "0",
                "find_available_url" to "1",
                "client_id" to appId,
                "client_secret" to appSecret,
                "options" to mapOf<String, Any>(
                    "site_segment" to segmentId,
                    "template" to siteDesign,
                    "site_creation_flow" to "with-design-picker",
                    "timezone_string" to timeZoneId,
                )
            )
        )
    }

    @Test
    fun `creates new site without params with dry run`() = test {
        val data = NewSiteResponse()
        val blogDetails = BlogDetails()
        blogDetails.blogid = siteId.toString()
        data.blog_details = blogDetails
        val appId = "app_id"
        whenever(appSecrets.appId).thenReturn(appId)
        val appSecret = "app_secret"
        whenever(appSecrets.appSecret).thenReturn(appSecret)

        initNewSiteResponse(data)

        val dryRun = true
        val siteName = "Site name"
        val siteTitle = null
        val language = "CZ"
        val visibility = SiteVisibility.PRIVATE
        val timeZoneId = "Europe/London"

        val result = restClient.newSite(
            siteName,
            siteTitle,
            language,
            timeZoneId,
            visibility,
            null,
            null,
            null,
            dryRun
        )

        assertThat(result.newSiteRemoteId).isEqualTo(siteId)
        assertThat(result.dryRun).isEqualTo(dryRun)

        assertThat(urlCaptor.lastValue)
            .isEqualTo("https://public-api.wordpress.com/rest/v1.1/sites/new/")
        assertThat(bodyCaptor.lastValue).isEqualTo(
            mapOf(
                "blog_name" to siteName,
                "lang_id" to language,
                "public" to "-1",
                "validate" to "1",
                "client_id" to appId,
                "client_secret" to appSecret,
                "options" to mapOf<String, Any>(
                    "timezone_string" to timeZoneId,
                )
            )
        )
    }

    @Test
    fun `creates new site in coming soon state`() = test {
        // given
        whenever(appSecrets.appId).thenReturn("")
        whenever(appSecrets.appSecret).thenReturn("")
        initNewSiteResponse()

        // when
        restClient.newSite("", "", "", "", visibility = COMING_SOON, null, null, null, false)

        // then
        val body = bodyCaptor.lastValue

        @Suppress("UNCHECKED_CAST")
        val options = body["options"] as Map<String, String>

        assertThat(body).containsEntry("public", "0")
        assertThat(options).containsEntry("wpcom_public_coming_soon", "1")
    }

    @Test
    fun `creates new site with override site creation flow if specified`() = test {
        // given
        whenever(appSecrets.appId).thenReturn("")
        whenever(appSecrets.appSecret).thenReturn("")
        initNewSiteResponse()

        val siteCreationFlow = "sample_creation_flow"

        // when
        restClient.newSite(
            null,
            "",
            "",
            "",
            visibility = COMING_SOON,
            null,
            null,
            null,
            false,
            siteCreationFlow = siteCreationFlow
        )

        // then
        val body = bodyCaptor.lastValue

        @Suppress("UNCHECKED_CAST")
        val options = body["options"] as Map<String, String>

        assertThat(options).containsEntry("site_creation_flow", siteCreationFlow)
    }

    @Test
    fun `when all domains are requested, then the correct response is built`() = test {
        val allDomainsJson = "wp/all-domains/all-domains.json"
        val json = UnitTestUtils.getStringFromResourceFile(javaClass, allDomainsJson)
        val responseType = object : TypeToken<AllDomainsResponse>() {}.type
        val response = GsonBuilder().create().fromJson(json, responseType) as AllDomainsResponse

        initAllDomainsResponse(data = response)

        val responseModel = restClient.fetchAllDomains()
        assert(responseModel is Success)
        with((responseModel as Success).data) {
            assertThat(domains).hasSize(4)
            assertThat(domains[0].domain).isEqualTo("some.test.domain")
            assertThat(domains[0].wpcomDomain).isFalse
            assertThat(domains[0].registrationDate).isEqualTo(
                DateTimeUtils.dateUTCFromIso8601("2009-03-26T21:20:53+00:00")
            )
            assertThat(domains[0].expiry).isEqualTo(
                DateTimeUtils.dateUTCFromIso8601("2024-03-24T00:00:00+00:00")
            )
            assertThat(domains[0].domainStatus).isNotNull
            assertThat(domains[0].domainStatus?.status).isEqualTo("Active")
            assertThat(domains[0].domainStatus?.statusType).isEqualTo(SUCCESS)
            assertThat(domains[1].domain).isEqualTo("some.test.domain.with.status.weight")
            assertThat(domains[1].wpcomDomain).isTrue
            assertThat(domains[1].domainStatus).isNotNull
            assertThat(domains[1].domainStatus?.status).isEqualTo("Expiring soon")
            assertThat(domains[1].domainStatus?.statusType).isEqualTo(ERROR)
            assertThat(domains[1].domainStatus?.statusWeight).isEqualTo(1000)
            assertThat(domains[2].domain).isEqualTo("some.test.domain.with.action.required")
            assertThat(domains[2].domainStatus).isNotNull
            assertThat(domains[2].domainStatus?.actionRequired).isTrue
            assertThat(domains[3].domain).isEqualTo("some.test.domain.no.domain.status")
            assertThat(domains[3].domainStatus).isNull()
        }
    }

    @Test
    fun `given a network error, when all domains are requested, then return api error`() = test {
        val error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.NETWORK_ERROR))
        initAllDomainsResponse(error = error)

        val response = restClient.fetchAllDomains()
        assert(response is Response.Error)
        with((response as Response.Error).error) {
            assertThat(type).isEqualTo(GenericErrorType.NETWORK_ERROR)
            assertThat(message).isNull()
        }
    }

    @Test
    fun `given timeout, when all domains are requested, then return timeout error`() = test {
        val error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.TIMEOUT))
        initAllDomainsResponse(error = error)

        val response = restClient.fetchAllDomains()
        assert(response is Response.Error)
        with((response as Response.Error).error) {
            assertThat(type).isEqualTo(GenericErrorType.TIMEOUT)
            assertThat(message).isNull()
        }
    }

    @Test
    fun `given not authenticated, when all domains are requested, then retun auth required error`() = test {
        val tokenErrorMessage = "An active access token must be used to query information about the current user."
        val error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.NOT_AUTHENTICATED, tokenErrorMessage))
        initAllDomainsResponse(error = error)

        val response = restClient.fetchAllDomains()
        assert(response is Response.Error)
        with((response as Response.Error).error) {
            assertThat(type).isEqualTo(GenericErrorType.NOT_AUTHENTICATED)
            assertThat(message).isEqualTo(tokenErrorMessage)
        }
    }

    @Test
    fun `given a suspended WPCom website, when fetching site info, then return correct error`() = test {
        val urlUtilsMock = mockStatic(UrlUtils::class.java)
        whenever(UrlUtils.addUrlSchemeIfNeeded(any(), any())).thenAnswer { it.arguments[0] as String }
        val error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.INVALID_RESPONSE, "")).apply {
            apiError = "connection_disabled"
        }
        initGetResponse(ConnectSiteInfoResponse::class.java, null, error)

        val result = restClient.fetchConnectSiteInfoSync("test.com")

        assertThat(result.error).isNotNull
        assertThat(result.error!!.type).isEqualTo(SiteErrorType.WPCOM_SITE_SUSPENDED)
        urlUtilsMock.close()
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
        assertThat(result.description).isEqualTo(response.description)
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
        assertThat(site.description).isEqualTo(rootResponse.description)
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

    private suspend fun initNewSiteResponse(
        data: NewSiteResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<NewSiteResponse> {
        return initPostResponse(NewSiteResponse::class.java, data ?: mock(), error)
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

    private suspend fun initAllDomainsResponse(
        data: AllDomainsResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<AllDomainsResponse> {
        return initGetResponse(AllDomainsResponse::class.java, data ?: AllDomainsResponse(emptyList()), error)
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

    private suspend fun <T> initPostResponse(
        clazz: Class<T>,
        data: T,
        error: WPComGsonNetworkError? = null
    ): Response<T> {
        val response = if (error != null) Response.Error(error) else Success(data, emptyList())
        whenever(
            wpComGsonRequestBuilder.syncPostRequest(
                eq(restClient),
                urlCaptor.capture(),
                paramsCaptor.capture(),
                bodyCaptor.capture(),
                eq(clazz),
                anyOrNull(),
                anyOrNull(),
                eq(true),
            )
        ).thenReturn(response)
        return response
    }
}
