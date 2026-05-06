package org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.settings.WCAnalyticsOrderDateType
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.WPComNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.utils.initCoroutineEngine

@OptIn(ExperimentalCoroutinesApi::class)
class OrderStatsRestClientTest {
    private val site = SiteModel()
    private val wooNetwork: WooNetwork = mock()
    private val wpComNetwork: WPComNetwork = mock()

    private lateinit var sut: OrderStatsRestClient

    @Before
    fun setUp() {
        sut = OrderStatsRestClient(
            dispatcher = mock<Dispatcher>(),
            wooNetwork = wooNetwork,
            wpComNetwork = wpComNetwork,
            coroutineEngine = initCoroutineEngine()
        )
    }

    @Test
    fun `when order date type is provided, then date type parameter is sent to API`() = runTest {
        stubRevenueStatsResponse()

        sut.fetchRevenueStats(
            site = site,
            granularity = StatsGranularity.DAYS,
            startDate = START_DATE,
            endDate = END_DATE,
            perPage = PER_PAGE,
            forceRefresh = true,
            revenueRangeId = REVENUE_RANGE_ID,
            orderDateType = WCAnalyticsOrderDateType.CREATED
        )

        assertThat(captureRevenueStatsParams())
            .containsEntry("date_type", WCAnalyticsOrderDateType.CREATED.value)
            .containsEntry("force_cache_refresh", "true")
    }

    @Test
    fun `when order date type is not provided, then date type parameter is not sent to API`() = runTest {
        stubRevenueStatsResponse()

        sut.fetchRevenueStats(
            site = site,
            granularity = StatsGranularity.DAYS,
            startDate = START_DATE,
            endDate = END_DATE,
            perPage = PER_PAGE,
            revenueRangeId = REVENUE_RANGE_ID
        )

        assertThat(captureRevenueStatsParams()).doesNotContainKey("date_type")
    }

    private suspend fun stubRevenueStatsResponse() {
        whenever(
            wooNetwork.executeGetGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(RevenueStatsApiResponse::class.java),
                params = any(),
                enableCaching = any(),
                cacheTimeToLive = any(),
                forced = any(),
                requestTimeout = any(),
                retries = any()
            )
        ).thenReturn(WPAPIResponse.Success(null, emptyList()))
    }

    private suspend fun captureRevenueStatsParams(): Map<String, String> {
        val paramsCaptor = argumentCaptor<Map<String, String>>()
        verify(wooNetwork).executeGetGsonRequest(
            site = eq(site),
            path = eq(WOOCOMMERCE.reports.revenue.stats.pathV4Analytics),
            clazz = eq(RevenueStatsApiResponse::class.java),
            params = paramsCaptor.capture(),
            enableCaching = eq(true),
            cacheTimeToLive = any(),
            forced = any(),
            requestTimeout = any(),
            retries = any()
        )
        return paramsCaptor.firstValue
    }

    private companion object {
        const val START_DATE = "2026-01-01T00:00:00"
        const val END_DATE = "2026-12-31T23:59:59"
        const val PER_PAGE = 100
        const val REVENUE_RANGE_ID = "year-date_created"
    }
}
