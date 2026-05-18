package com.woocommerce.android.aiassistant.tools.analytics

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.settings.CurrencyPosition
import org.wordpress.android.fluxc.model.settings.Settings
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsResponsePayload
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class AIAnalyticsDataSourceTest {
    private val selectedSite: SelectedSite = mock()
    private val orderStatsRestClient: OrderStatsRestClient = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val site = SiteModel().apply { id = SITE_ID }

    private lateinit var dataSource: AIAnalyticsDataSource

    @Before
    fun setUp() {
        whenever(selectedSite.get()).thenReturn(site)
        dataSource = AIAnalyticsDataSource(
            selectedSite = selectedSite,
            orderStatsRestClient = orderStatsRestClient,
            wooCommerceStore = wooCommerceStore,
        )
    }

    @Test
    fun `given orders stats response, when fetched, then REST client params and parsed stats are returned`() =
        runTest {
            // GIVEN
            whenever(
                orderStatsRestClient.fetchOrdersStats(
                    site = site,
                    granularity = AnalyticsInterval.WEEK.statsGranularity,
                    startDate = AFTER,
                    endDate = BEFORE,
                    perPage = PER_PAGE,
                    forceRefresh = false,
                    orderStatsRangeId = ORDERS_RANGE_ID,
                )
            ).thenReturn(
                successResponse(
                    interval = AnalyticsInterval.WEEK,
                    total = ORDERS_TOTAL,
                    data = ORDERS_INTERVALS,
                )
            )

            // WHEN
            val result = dataSource.fetchOrdersStats(
                after = AFTER,
                before = BEFORE,
                interval = AnalyticsInterval.WEEK,
            )

            // THEN
            verify(orderStatsRestClient).fetchOrdersStats(
                site = site,
                granularity = AnalyticsInterval.WEEK.statsGranularity,
                startDate = AFTER,
                endDate = BEFORE,
                perPage = PER_PAGE,
                forceRefresh = false,
                orderStatsRangeId = ORDERS_RANGE_ID,
            )
            assertThat(result.isSuccess).isTrue

            val stats = result.getOrThrow()
            val totals = requireNotNull(stats.totals).jsonObject
            assertThat(totals.getValue("orders_count").jsonPrimitive.content).isEqualTo("7")
            assertThat(totals.getValue("avg_order_value").jsonPrimitive.content).isEqualTo("42.00")

            val intervals = requireNotNull(stats.intervals)
            assertThat(intervals).hasSize(1)
            assertThat(intervals.first().getValue("interval").jsonPrimitive.content).isEqualTo("week-2026-15")
        }

    @Test
    fun `given null stats response, when orders stats are fetched, then failure is returned`() =
        runTest {
            // GIVEN
            whenever(
                orderStatsRestClient.fetchOrdersStats(
                    site = site,
                    granularity = AnalyticsInterval.DAY.statsGranularity,
                    startDate = AFTER,
                    endDate = BEFORE,
                    perPage = PER_PAGE,
                    forceRefresh = false,
                    orderStatsRangeId = ORDERS_RANGE_ID,
                )
            ).thenReturn(
                FetchRevenueStatsResponsePayload(
                    site = site,
                    granularity = AnalyticsInterval.DAY.statsGranularity,
                    stats = null,
                )
            )

            // WHEN
            val result = dataSource.fetchOrdersStats(
                after = AFTER,
                before = BEFORE,
                interval = AnalyticsInterval.DAY,
            )

            // THEN
            assertThat(result.isFailure).isTrue
        }

    @Test
    fun `given selected site settings, when currency code is requested, then site currency is returned`() {
        whenever(wooCommerceStore.getSiteSettings(site)).thenReturn(siteSettings(currencyCode = CURRENCY))

        val currencyCode = dataSource.getSelectedSiteCurrencyCode()

        assertThat(currencyCode).isEqualTo(CURRENCY)
    }

    private fun successResponse(
        interval: AnalyticsInterval,
        total: String,
        data: String,
    ) = FetchRevenueStatsResponsePayload(
        site = site,
        granularity = interval.statsGranularity,
        stats = WCRevenueStatsModel(
            localSiteId = site.localId(),
            interval = interval.value,
            startDate = AFTER,
            endDate = BEFORE,
            data = data,
            total = total,
            rangeId = "unused",
        ),
    )

    private fun siteSettings(currencyCode: String) = Settings(
        currencyCode = currencyCode,
        currencyPosition = CurrencyPosition.LEFT,
        currencyThousandSeparator = ",",
        currencyDecimalSeparator = ".",
        currencyDecimalNumber = 2,
        countryCode = "US",
        stateCode = "CA",
        address = "",
        address2 = "",
        city = "",
        postalCode = "",
        couponsEnabled = true,
    )

    private companion object {
        private const val SITE_ID = 123
        private const val AFTER = "2026-04-01T00:00:00"
        private const val BEFORE = "2026-04-30T23:59:59"
        private const val CURRENCY = "USD"
        private const val PER_PAGE = 100
        private const val ORDERS_RANGE_ID = "ai_orders"

        private const val ORDERS_TOTAL = """
            {"orders_count":7,"avg_order_value":"42.00"}
        """
        private const val ORDERS_INTERVALS = """
            [
                {
                    "interval":"week-2026-15",
                    "subtotals":{"orders_count":7,"avg_order_value":"42.00"}
                }
            ]
        """
    }
}
