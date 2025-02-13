package org.wordpress.android.fluxc.store

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.google.WCGoogleAdsCampaignMapper
import org.wordpress.android.fluxc.model.google.WCGoogleAdsProgramTotals
import org.wordpress.android.fluxc.model.google.WCGoogleAdsPrograms
import org.wordpress.android.fluxc.model.google.WCGoogleAdsProgramsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.google.GoogleAdsCampaignDTO
import org.wordpress.android.fluxc.network.rest.wpcom.wc.google.GoogleAdsTotalsDTO
import org.wordpress.android.fluxc.network.rest.wpcom.wc.google.WCGoogleAdsProgramsDTO
import org.wordpress.android.fluxc.network.rest.wpcom.wc.google.WCGoogleRestClient
import org.wordpress.android.fluxc.store.WCGoogleStore.TotalsType.CLICKS
import org.wordpress.android.fluxc.store.WCGoogleStore.TotalsType.SALES
import org.wordpress.android.fluxc.utils.initCoroutineEngine

@ExperimentalCoroutinesApi
class WCGoogleStoreTest {
    @Mock
    lateinit var restClient: WCGoogleRestClient

    @Mock
    lateinit var wcGoogleAdsCampaignMapper: WCGoogleAdsCampaignMapper

    private lateinit var wcGoogleStore: WCGoogleStore
    private val siteModel = SiteModel()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        wcGoogleStore = WCGoogleStore(
            restClient = restClient,
            coroutineEngine = initCoroutineEngine(),
            campaignMapper = wcGoogleAdsCampaignMapper,
            programsMapper = WCGoogleAdsProgramsMapper()
        )
    }

    @Test
    fun `when programs response is a single page, then return the data directly`() = runBlockingTest {
        // Given
        val mockPrograms = createProgramsPage()
        val expectedModel = WCGoogleAdsProgramsMapper().mapToModel(mockPrograms)

        whenever(restClient.fetchAllPrograms(
            siteModel,
            "2020-01-01",
            "2020-12-31",
            "sales,clicks",
            "sales",
            null
        )).thenReturn(WooPayload(mockPrograms))

        // When
        val result = wcGoogleStore.fetchAllPrograms(
            site = siteModel,
            startDate = "2020-01-01",
            endDate = "2020-12-31",
            totals = listOf(SALES, CLICKS)
        )

        // Then
        assertFalse(result.isError)
        assertNotNull(result.model)
        assertEquals(result.model, expectedModel)
    }

    @Test
    fun `when programs response is paginated, requested everything and return the sum`() = runBlockingTest  {
        // Given
        val mapper = WCGoogleAdsProgramsMapper()
        val firstPage = createProgramsPage(pageNumber = 1, hasNextPage = true)
        val pageTwo = createProgramsPage(pageNumber = 2)
        val mappedFirstPage = mapper.mapToModel(firstPage)
        val mappedSecondPage = mapper.mapToModel(pageTwo)
        val expectedModel = listOf(mappedFirstPage, mappedSecondPage).let { pages ->
            WCGoogleAdsPrograms(
                campaigns = pages.flatMap { it.campaigns.orEmpty() },
                intervals = pages.flatMap { it.intervals.orEmpty() },
                totals = WCGoogleAdsProgramTotals(
                    sales = pages.sumOf { it.totals?.sales ?: 0.0 },
                    spend = pages.sumOf { it.totals?.spend ?: 0.0 },
                    clicks = pages.sumOf { it.totals?.clicks ?: 0.0 },
                    impressions = pages.sumOf { it.totals?.impressions ?: 0.0 },
                    conversions = pages.sumOf { it.totals?.conversions ?: 0.0 }
                )

            )
        }

        whenever(restClient.fetchAllPrograms(
            siteModel,
            "2020-01-01",
            "2020-12-31",
            "sales,clicks",
            "sales",
            null
        )).thenReturn(WooPayload(firstPage))

        whenever(restClient.fetchAllPrograms(
            siteModel,
            "2020-01-01",
            "2020-12-31",
            "sales,clicks",
            "sales",
            "nextPageToken"
        )).thenReturn(WooPayload(pageTwo))

        // When
        val result = wcGoogleStore.fetchAllPrograms(
            site = siteModel,
            startDate = "2020-01-01",
            endDate = "2020-12-31",
            totals = listOf(SALES, CLICKS)
        )

        // Then
        assertFalse(result.isError)
        assertNotNull(result.model)
        assertEquals(result.model, expectedModel)
    }

    private fun createProgramsPage(
        pageNumber: Int = 1,
        hasNextPage: Boolean = false
    ) = WCGoogleAdsProgramsDTO(
        campaigns = listOf(
            GoogleAdsCampaignDTO(
                id = pageNumber.toLong(),
                name = "campaign${pageNumber}",
                status = "enabled",
                subtotals = GoogleAdsTotalsDTO(
                    sales = pageNumber * 100.0,
                    spend = pageNumber * 100.0,
                    impressions = pageNumber * 100.0,
                    clicks = pageNumber * 100.0,
                    conversions = pageNumber * 100.0
                )
            ),
            GoogleAdsCampaignDTO(
                id = (pageNumber + 1).toLong(),
                name = "campaign${pageNumber + 1}",
                status = "enabled",
                subtotals = GoogleAdsTotalsDTO(
                    sales = pageNumber * 200.0,
                    spend = pageNumber * 200.0,
                    impressions = pageNumber * 200.0,
                    clicks = pageNumber * 200.0,
                    conversions = pageNumber * 200.0
                )
            )
        ),
        intervals = emptyList(),
        totals = GoogleAdsTotalsDTO(
            sales = pageNumber * 100.0,
            spend = pageNumber * 100.0,
            impressions = pageNumber * 100.0,
            clicks = pageNumber * 100.0,
            conversions = pageNumber * 100.0
        ),
        nextPageToken = if (hasNextPage) "nextPageToken" else null
    )
}
