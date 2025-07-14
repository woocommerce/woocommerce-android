package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.SitesModel
import org.wordpress.android.fluxc.model.asDomainModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.PARSE_ERROR
import org.wordpress.android.fluxc.network.rest.wpapi.site.SiteWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.site.AllDomainsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.site.Domain
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.site.PlansResponse
import org.wordpress.android.fluxc.network.rest.wpcom.site.PrivateAtomicCookie
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.NewSiteResponsePayload
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.domains.DomainDao
import org.wordpress.android.fluxc.store.SiteStore.AllDomainsError
import org.wordpress.android.fluxc.store.SiteStore.AllDomainsErrorType
import org.wordpress.android.fluxc.store.SiteStore.FetchSitesPayload
import org.wordpress.android.fluxc.store.SiteStore.FetchedAllDomainsPayload
import org.wordpress.android.fluxc.store.SiteStore.FetchedDomainsPayload
import org.wordpress.android.fluxc.store.SiteStore.FetchedPlansPayload
import org.wordpress.android.fluxc.store.SiteStore.NewSiteError
import org.wordpress.android.fluxc.store.SiteStore.NewSiteErrorType.SITE_NAME_INVALID
import org.wordpress.android.fluxc.store.SiteStore.NewSitePayload
import org.wordpress.android.fluxc.store.SiteStore.PlansError
import org.wordpress.android.fluxc.store.SiteStore.PlansErrorType
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.SiteStore.SiteFilter.WPCOM
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility.PUBLIC
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class SiteStoreTest {
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var siteRestClient: SiteRestClient
    @Mock lateinit var siteXMLRPCClient: SiteXMLRPCClient
    @Mock lateinit var siteWPAPIClient: SiteWPAPIRestClient
    @Mock lateinit var privateAtomicCookie: PrivateAtomicCookie
    @Mock lateinit var siteSqlUtils: SiteSqlUtils
    @Mock lateinit var domainsDao: DomainDao
    @Mock lateinit var domainsSuccessResponse: Response.Success<DomainsResponse>
    @Mock lateinit var allDomainsSuccessResponse: Response.Success<AllDomainsResponse>
    @Mock lateinit var plansSuccessResponse: Response.Success<PlansResponse>
    @Mock lateinit var domainsErrorResponse: Response.Error<DomainsResponse>
    @Mock lateinit var allDomainsErrorResponse: Response.Error<AllDomainsResponse>
    @Mock lateinit var plansErrorResponse: Response.Error<PlansResponse>
    private lateinit var siteStore: SiteStore

    @Before
    fun setUp() {
        siteStore = SiteStore(
            dispatcher,
            siteRestClient,
            siteXMLRPCClient,
            siteWPAPIClient,
            privateAtomicCookie,
            siteSqlUtils,
            domainsDao,
            initCoroutineEngine()
        )
    }

    @Test
    fun `fetchSite from WPCom endpoint and stores it to DB`() = test {
        val site = SiteModel()
        site.setIsWPCom(true)
        site.origin = SiteModel.ORIGIN_WPCOM_REST
        val updatedSite = SiteModel()
        whenever(siteRestClient.fetchSite(site)).thenReturn(updatedSite)

        assertSiteFetched(updatedSite, site)
    }

    @Test
    fun `fetchSite error from WPCom endpoint returns error`() = test {
        val site = SiteModel()
        site.setIsWPCom(true)
        site.origin = SiteModel.ORIGIN_WPCOM_REST
        val errorSite = SiteModel()
        errorSite.error = BaseNetworkError(PARSE_ERROR)
        whenever(siteRestClient.fetchSite(site)).thenReturn(errorSite)

        assertSiteFetchError(site)
    }

    @Test
    fun `fetchSite from XMLRPC endpoint and stores it to DB`() = test {
        val site = SiteModel()
        site.setIsWPCom(false)
        val updatedSite = SiteModel()
        whenever(siteXMLRPCClient.fetchSite(site)).thenReturn(updatedSite)

        assertSiteFetched(updatedSite, site)
    }

    @Test
    fun `fetchSite error from XMLRPC endpoint returns error`() = test {
        val site = SiteModel()
        site.setIsWPCom(false)
        val errorSite = SiteModel()
        errorSite.error = BaseNetworkError(PARSE_ERROR)
        whenever(siteXMLRPCClient.fetchSite(site)).thenReturn(errorSite)

        assertSiteFetchError(site)
    }

    private suspend fun assertSiteFetchError(site: SiteModel) {
        val onSiteChanged = siteStore.fetchSite(site)

        assertThat(onSiteChanged.rowsAffected).isEqualTo(0)
        assertThat(onSiteChanged.error).isEqualTo(SiteError(GENERIC_ERROR, null))
        verifyNoInteractions(siteSqlUtils)
    }

    private suspend fun assertSiteFetched(
        updatedSite: SiteModel,
        site: SiteModel
    ) {
        val rowsChanged = 1
        whenever(siteSqlUtils.insertOrUpdateSite(updatedSite)).thenReturn(rowsChanged)

        val onSiteChanged = siteStore.fetchSite(site)

        assertThat(onSiteChanged.rowsAffected).isEqualTo(rowsChanged)
        assertThat(onSiteChanged.error).isNull()
        verify(siteSqlUtils).insertOrUpdateSite(updatedSite)
    }

    @Test
    fun `fetchSites saves fetched sites to DB and removes absent sites`() = test {
        val payload = FetchSitesPayload(listOf(WPCOM))
        val sitesModel = SitesModel()
        val siteA = SiteModel().apply { url = "siteA.com" }
        val siteB = SiteModel().apply { url = "siteB.com" }
        sitesModel.sites = listOf(siteA, siteB)
        whenever(siteRestClient.fetchSites(payload.filters, false)).thenReturn(sitesModel)
        whenever(siteSqlUtils.insertOrUpdateSite(siteA)).thenReturn(1)
        whenever(siteSqlUtils.insertOrUpdateSite(siteB)).thenReturn(1)

        val onSiteChanged = siteStore.fetchSites(payload)

        assertThat(onSiteChanged.rowsAffected).isEqualTo(2)
        assertThat(onSiteChanged.error).isNull()
        val inOrder = inOrder(siteSqlUtils)
        inOrder.verify(siteSqlUtils).insertOrUpdateSite(siteA)
        inOrder.verify(siteSqlUtils).insertOrUpdateSite(siteB)
        inOrder.verify(siteSqlUtils).removeWPComRestSitesAbsentFromList(sitesModel.sites)
    }

    @Test
    fun `fetchSites returns error`() = test {
        val payload = FetchSitesPayload(listOf(WPCOM))
        val sitesModel = SitesModel()
        sitesModel.error = BaseNetworkError(PARSE_ERROR)
        whenever(siteRestClient.fetchSites(payload.filters, false)).thenReturn(sitesModel)

        val onSiteChanged = siteStore.fetchSites(payload)

        assertThat(onSiteChanged.rowsAffected).isEqualTo(0)
        assertThat(onSiteChanged.error).isEqualTo(SiteError(GENERIC_ERROR, null))
        verifyNoInteractions(siteSqlUtils)
    }

    @Test
    fun `creates a new site`() = test {
        val dryRun = false
        val name = "New site"
        val payload = NewSitePayload(name, null, "CZ", "Europe/London", PUBLIC, null, dryRun)
        val newSiteRemoteId: Long = 123
        val url = "new.wp.com"
        val response = NewSiteResponsePayload(newSiteRemoteId, siteUrl = url, dryRun)
        whenever(
                siteRestClient.newSite(
                        name,
                        null,
                        payload.language,
                        payload.timeZoneId,
                        payload.visibility,
                        null,
                        null,
                        null,
                        payload.dryRun
                )
        ).thenReturn(response)

        val result = siteStore.createNewSite(payload)

        assertThat(result.dryRun).isEqualTo(dryRun)
        assertThat(result.newSiteRemoteId).isEqualTo(newSiteRemoteId)
        assertEquals(url, result.url)
    }

    @Test
    fun `fails to create a new site`() = test {
        val dryRun = false
        val payload = NewSitePayload("New site", "CZ", "Europe/London", PUBLIC, dryRun)
        val response = NewSiteResponsePayload()
        val newSiteError = NewSiteError(SITE_NAME_INVALID, "Site name invalid")
        response.error = newSiteError
        whenever(
                siteRestClient.newSite(
                        payload.siteName,
                        null,
                        payload.language,
                        payload.timeZoneId,
                        payload.visibility,
                        null,
                        null,
                        null,
                        payload.dryRun
                )
        ).thenReturn(response)

        val result = siteStore.createNewSite(payload)

        assertThat(result.dryRun).isEqualTo(dryRun)
        assertThat(result.newSiteRemoteId).isEqualTo(0)
        assertThat(result.error).isEqualTo(newSiteError)
    }

    @Test
    fun `fetchSiteDomains from WPCom endpoint`() = test {
        val site = SiteModel()
        site.setIsWPCom(true)

        whenever(siteRestClient.fetchSiteDomains(site)).thenReturn(domainsSuccessResponse)
        whenever(domainsSuccessResponse.data).thenReturn(DomainsResponse(listOf()))

        val onSiteDomainsFetched = siteStore.fetchSiteDomains(site)

        assertThat(onSiteDomainsFetched.domains).isNotNull
        assertThat(onSiteDomainsFetched.error).isNull()
    }

    @Test
    fun `fetchSiteDomains error from WPCom endpoint returns error`() = test {
        val site = SiteModel()
        site.setIsWPCom(true)

        whenever(siteRestClient.fetchSiteDomains(site)).thenReturn(domainsErrorResponse)
        whenever(domainsErrorResponse.error).thenReturn(WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))

        val onSiteDomainsFetched = siteStore.fetchSiteDomains(site)

        verifyNoInteractions(domainsDao)
        assertThat(onSiteDomainsFetched.error).isEqualTo(SiteError(GENERIC_ERROR, null))
        assertThat(onSiteDomainsFetched).isEqualTo(FetchedDomainsPayload(site, onSiteDomainsFetched.domains))
    }

    @Test
    fun `getSiteDomains is backed by DomainsDao`() = test {
        val siteLocalId = 1234
        val domainEntity = DomainDao.DomainEntity(
            siteLocalId = siteLocalId,
            domain = "example.wordpress.com",
            primaryDomain = true,
            wpcomDomain = true
        )

        whenever(domainsDao.getDomains(siteLocalId)).thenReturn(flowOf(listOf(domainEntity)))

        assertEquals(
            domainsDao.getDomains(siteLocalId).first().map(DomainDao.DomainEntity::toDomainModel),
            siteStore.getSiteDomains(siteLocalId).first()
        )
    }

    @Test
    fun `fetchSiteDomains updates stored domains`() = test {
        val siteLocalId = 1234
        val site = SiteModel()
        site.id = siteLocalId
        val domains = listOf(Domain(domain = "example.wordpress.com", primaryDomain = true, wpcomDomain = true))

        whenever(siteRestClient.fetchSiteDomains(site)).thenReturn(Response.Success(DomainsResponse(domains)))

        siteStore.fetchSiteDomains(site)

        verify(domainsDao).insert(siteLocalId, domains.map(Domain::asDomainModel))
    }

    @Test
    fun `fetchSitePlans from WPCom endpoint`() = test {
        val site = SiteModel()
        site.setIsWPCom(true)

        whenever(siteRestClient.fetchSitePlans(site)).thenReturn(plansSuccessResponse)
        whenever(plansSuccessResponse.data).thenReturn(PlansResponse(listOf()))

        val onSitePlansFetched = siteStore.fetchSitePlans(site)

        assertThat(onSitePlansFetched.plans).isNotNull
        assertThat(onSitePlansFetched.error).isNull()
        assertThat(onSitePlansFetched).isEqualTo(FetchedPlansPayload(site, onSitePlansFetched.plans))
    }

    @Test
    fun `fetchSitePlans error from WPCom endpoint returns error`() = test {
        val site = SiteModel()
        site.setIsWPCom(true)

        whenever(siteRestClient.fetchSitePlans(site)).thenReturn(plansErrorResponse)
        whenever(plansErrorResponse.error).thenReturn(WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))

        val onSitePlansFetched = siteStore.fetchSitePlans(site)

        assertThat(onSitePlansFetched.error.type).isEqualTo(PlansError(PlansErrorType.GENERIC_ERROR, null).type)
    }

    @Test
    fun `fetchAllDomains from WPCom endpoint`() = test {
        whenever(siteRestClient.fetchAllDomains()).thenReturn(allDomainsSuccessResponse)
        whenever(allDomainsSuccessResponse.data).thenReturn(AllDomainsResponse(listOf()))

        val onAllDomainsFetched = siteStore.fetchAllDomains()

        assertThat(onAllDomainsFetched.domains).isNotNull
        assertThat(onAllDomainsFetched.error).isNull()
        assertThat(onAllDomainsFetched).isEqualTo(FetchedAllDomainsPayload(onAllDomainsFetched.domains))
    }

    @Test
    fun `fetchAllDomains error from WPCom endpoint returns error`() = test {
        val site = SiteModel()
        site.setIsWPCom(true)

        whenever(siteRestClient.fetchAllDomains()).thenReturn(allDomainsErrorResponse)
        whenever(allDomainsErrorResponse.error).thenReturn(WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))

        val onAllDomainsFetched = siteStore.fetchAllDomains()

        val expectedErrorType = AllDomainsError(AllDomainsErrorType.GENERIC_ERROR, null).type
        assertThat(onAllDomainsFetched.error.type).isEqualTo(expectedErrorType)
    }

    @Test
    fun `when updating site from WPCom REST API, then preserve existing app passwords authorize URL`() = test {
        val siteId = 1234L
        val authorizationUrl = "https://example.com/authorize"
        whenever(siteSqlUtils.getSitesWithRemoteId(siteId)).thenReturn(listOf(SiteModel().apply {
            this.siteId = siteId
            applicationPasswordsAuthorizeUrl = authorizationUrl
        }))
        val fetchedSite = SiteModel().apply {
            this.origin = SiteModel.ORIGIN_WPCOM_REST
            this.siteId = siteId
        }
        whenever(siteRestClient.fetchSite(any())).thenReturn(fetchedSite)

        siteStore.fetchSite(fetchedSite)

        verify(siteSqlUtils).insertOrUpdateSite(argWhere { site ->
            site.applicationPasswordsAuthorizeUrl == authorizationUrl
        })
    }
}
