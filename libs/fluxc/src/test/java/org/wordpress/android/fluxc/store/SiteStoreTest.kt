package org.wordpress.android.fluxc.store

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
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
import org.wordpress.android.fluxc.persistence.SiteMapper
import org.wordpress.android.fluxc.persistence.SiteStorePersistence
import org.wordpress.android.fluxc.persistence.dao.SiteDao
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

@Suppress("DoNotMockDataClass", "UnitTestNamingRule")
class SiteStoreTest {
    private val dispatcher: Dispatcher = mock()
    private val siteRestClient: SiteRestClient = mock()
    private val siteXMLRPCClient: SiteXMLRPCClient = mock()
    private val siteWPAPIClient: SiteWPAPIRestClient = mock()
    private val siteDao: SiteDao = mock()
    private val siteStorePersistence: SiteStorePersistence = mock()
    private val domainsDao: DomainDao = mock()
    private val domainsSuccessResponse: Response.Success<DomainsResponse> = mock()
    private val plansSuccessResponse: Response.Success<PlansResponse> = mock()
    private val domainsErrorResponse: Response.Error<DomainsResponse> = mock()
    private val plansErrorResponse: Response.Error<PlansResponse> = mock()
    private val siteMapper = SiteMapper()
    private lateinit var siteStore: SiteStore

    @Before
    fun setUp() {
        test {
            whenever(siteStorePersistence.insertOrUpdateSite(any())).thenReturn(1)
            whenever(siteDao.getByRemoteId(any())).thenReturn(emptyList())
        }

        siteStore = SiteStore(
            dispatcher,
            siteRestClient,
            siteXMLRPCClient,
            siteWPAPIClient,
            siteDao,
            siteMapper,
            siteStorePersistence,
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
        verifyNoInteractions(siteDao)
    }

    private suspend fun assertSiteFetched(
        updatedSite: SiteModel,
        site: SiteModel
    ) {
        updatedSite.url = "https://test.wordpress.com"
        updatedSite.xmlRpcUrl = "https://test.wordpress.com/xmlrpc.php"

        val onSiteChanged = siteStore.fetchSite(site)

        assertThat(onSiteChanged.rowsAffected).isEqualTo(1)
        assertThat(onSiteChanged.error).isNull()
        verify(siteStorePersistence).insertOrUpdateSite(any())
    }

    @Test
    fun `fetchSites saves fetched sites to DB and removes absent sites`() = test {
        val payload = FetchSitesPayload(listOf(WPCOM))
        val sitesModel = SitesModel()
        val siteA = SiteModel().apply {
            url = "siteA.com"
            xmlRpcUrl = "siteA.com/xmlrpc.php"
        }
        val siteB = SiteModel().apply {
            url = "siteB.com"
            xmlRpcUrl = "siteB.com/xmlrpc.php"
        }
        sitesModel.sites = listOf(siteA, siteB)
        whenever(siteRestClient.fetchSites(payload.filters, false)).thenReturn(sitesModel)

        val onSiteChanged = siteStore.fetchSites(payload)

        assertThat(onSiteChanged.rowsAffected).isEqualTo(2)
        assertThat(onSiteChanged.error).isNull()
        verify(siteDao).deleteByOriginNotInList(
            eq(SiteModel.ORIGIN_WPCOM_REST),
            eq(listOf(siteA.siteId, siteB.siteId))
        )
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
        verifyNoInteractions(siteDao)
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
