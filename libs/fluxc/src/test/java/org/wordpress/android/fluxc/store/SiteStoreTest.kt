package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
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
import org.wordpress.android.fluxc.network.rest.wpcom.site.Domain
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.site.PlansResponse
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.domains.DomainDao
import org.wordpress.android.fluxc.store.SiteStore.FetchSitesPayload
import org.wordpress.android.fluxc.store.SiteStore.FetchedDomainsPayload
import org.wordpress.android.fluxc.store.SiteStore.FetchedPlansPayload
import org.wordpress.android.fluxc.store.SiteStore.PlansError
import org.wordpress.android.fluxc.store.SiteStore.PlansErrorType
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.SiteStore.SiteFilter.WPCOM
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals

@Suppress("DoNotMockDataClass", "UnitTestNamingRule")
class SiteStoreTest {
    private val dispatcher: Dispatcher = mock()
    private val siteRestClient: SiteRestClient = mock()
    private val siteXMLRPCClient: SiteXMLRPCClient = mock()
    private val siteWPAPIClient: SiteWPAPIRestClient = mock()
    private val siteSqlUtils: SiteSqlUtils = mock()
    private val domainsDao: DomainDao = mock()
    private val domainsSuccessResponse: Response.Success<DomainsResponse> = mock()
    private val plansSuccessResponse: Response.Success<PlansResponse> = mock()
    private val domainsErrorResponse: Response.Error<DomainsResponse> = mock()
    private val plansErrorResponse: Response.Error<PlansResponse> = mock()
    private lateinit var siteStore: SiteStore

    @Before
    fun setUp() {
        siteStore = SiteStore(
            dispatcher,
            siteRestClient,
            siteXMLRPCClient,
            siteWPAPIClient,
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

        whenever(siteRestClient.fetchSiteDomains(site)).thenReturn(
            Response.Success(
                DomainsResponse(domains),
                emptyList()
            )
        )

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
}
