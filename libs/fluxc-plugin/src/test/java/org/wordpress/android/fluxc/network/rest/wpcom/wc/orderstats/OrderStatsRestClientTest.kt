package org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats

import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.settings.WCAnalyticsOrderDateType
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WPComSiteInvalidationEvent
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WPComSiteInvalidationListener
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WPComSiteInvalidationReason
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.utils.initCoroutineEngine
import java.util.Optional

@OptIn(ExperimentalCoroutinesApi::class)
class OrderStatsRestClientTest {
    private val site = SiteModel().apply { siteId = SITE_ID }
    private val wooNetwork: WooNetwork = mock()
    private val wpComNetwork: WPComNetwork = mock()
    private val wpComSiteInvalidationListener: WPComSiteInvalidationListener = mock()

    private lateinit var sut: OrderStatsRestClient

    @Before
    fun setUp() {
        sut = OrderStatsRestClient(
            dispatcher = mock<Dispatcher>(),
            wooNetwork = wooNetwork,
            wpComNetwork = wpComNetwork,
            wpComSiteInvalidationListener = Optional.of(wpComSiteInvalidationListener),
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

    @Test
    fun `given missing Jetpack signature, when visitor stats are fetched, then notify the site invalidation listener and preserve error mapping`() =
        runTest {
            assertSiteInvalidation(createNetworkError())
        }

    @Test
    fun `given trivial missing Jetpack message variation, when visitor stats are fetched, then notify the site invalidation listener`() = runTest {
        listOf(
            "$JETPACK_CONNECTION_MISSING_MESSAGE.",
            "  $JETPACK_CONNECTION_MISSING_MESSAGE  ",
            "this BLOG does NOT have jetpack CONNECTED"
        ).forEach { message ->
            assertSiteInvalidation(createNetworkError(message = message))
        }
    }

    @Test
    fun `given Stats module is disabled, when visitor stats are fetched, then do not notify the site invalidation listener`() = runTest {
        assertNoSiteInvalidation(
            createNetworkError(message = STATS_MODULE_DISABLED_MESSAGE)
        )
    }

    @Test
    fun `given non-404 missing Jetpack signature, when visitor stats are fetched, then do not notify the site invalidation listener`() = runTest {
        assertNoSiteInvalidation(createNetworkError(statusCode = 400))
    }

    @Test
    fun `given different error code or message, when visitor stats are fetched, then do not notify the site invalidation listener`() = runTest {
        listOf(
            createNetworkError(apiError = "unknown_blog"),
            createNetworkError(message = "This blog does not have Jetpack installed")
        ).forEach { error ->
            assertNoSiteInvalidation(error)
        }
    }

    @Test
    fun `given successful visitor stats response, when visitor stats are fetched, then do not notify the site invalidation listener`() = runTest {
        VisitorStatsEndpoint.entries.forEach { endpoint ->
            clearInvocations(wpComSiteInvalidationListener)
            stubVisitorStatsSuccess(endpoint)

            fetchVisitorStats(endpoint)

            verify(wpComSiteInvalidationListener, never()).onSiteInvalidated(any())
        }
    }

    private suspend fun assertSiteInvalidation(error: WPComGsonNetworkError) {
        VisitorStatsEndpoint.entries.forEach { endpoint ->
            clearInvocations(wpComSiteInvalidationListener)
            stubVisitorStatsResponse(endpoint, error)

            val mapping = fetchVisitorStats(endpoint)

            assertThat(mapping)
                .describedAs(endpoint.name)
                .isEqualTo(
                    when (endpoint) {
                        VisitorStatsEndpoint.VISITS -> ErrorMapping(
                            type = "GENERIC_ERROR",
                            message = error.message
                        )

                        VisitorStatsEndpoint.SUMMARY -> ErrorMapping(
                            type = "INVALID_ID",
                            message = error.message,
                            apiErrorCode = INVALID_BLOG_ERROR_CODE,
                            original = "NOT_FOUND"
                        )
                    }
                )
            verify(wpComSiteInvalidationListener).onSiteInvalidated(
                WPComSiteInvalidationEvent(
                    siteId = SITE_ID,
                    reason = WPComSiteInvalidationReason.JETPACK_CONNECTION_MISSING
                )
            )
        }
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

    private suspend fun assertNoSiteInvalidation(error: WPComGsonNetworkError) {
        VisitorStatsEndpoint.entries.forEach { endpoint ->
            clearInvocations(wpComSiteInvalidationListener)
            stubVisitorStatsResponse(endpoint, error)

            fetchVisitorStats(endpoint)

            verify(wpComSiteInvalidationListener, never()).onSiteInvalidated(any())
        }
    }

    private suspend fun stubVisitorStatsResponse(
        endpoint: VisitorStatsEndpoint,
        error: WPComGsonNetworkError
    ) {
        when (endpoint) {
            VisitorStatsEndpoint.VISITS -> whenever(
                wpComNetwork.executeGetGsonRequest(
                    url = eq(visitorStatsUrl(endpoint)),
                    clazz = eq(VisitorStatsApiResponse::class.java),
                    params = eq(visitorStatsParams(endpoint)),
                    enableCaching = eq(true),
                    cacheTimeToLive = eq(BaseRequest.DEFAULT_CACHE_LIFETIME),
                    forced = eq(false)
                )
            ).thenReturn(WPComGsonRequestBuilder.Response.Error(error))

            VisitorStatsEndpoint.SUMMARY -> whenever(
                wpComNetwork.executeGetGsonRequest(
                    url = eq(visitorStatsUrl(endpoint)),
                    clazz = eq(VisitorStatsSummaryApiResponse::class.java),
                    params = eq(visitorStatsParams(endpoint)),
                    enableCaching = eq(true),
                    cacheTimeToLive = eq(BaseRequest.DEFAULT_CACHE_LIFETIME),
                    forced = eq(false)
                )
            ).thenReturn(WPComGsonRequestBuilder.Response.Error(error))
        }
    }

    private suspend fun stubVisitorStatsSuccess(endpoint: VisitorStatsEndpoint) {
        when (endpoint) {
            VisitorStatsEndpoint.VISITS -> whenever(
                wpComNetwork.executeGetGsonRequest(
                    url = eq(visitorStatsUrl(endpoint)),
                    clazz = eq(VisitorStatsApiResponse::class.java),
                    params = eq(visitorStatsParams(endpoint)),
                    enableCaching = eq(true),
                    cacheTimeToLive = eq(BaseRequest.DEFAULT_CACHE_LIFETIME),
                    forced = eq(false)
                )
            ).thenReturn(WPComGsonRequestBuilder.Response.Success(VisitorStatsApiResponse(), emptyList()))

            VisitorStatsEndpoint.SUMMARY -> whenever(
                wpComNetwork.executeGetGsonRequest(
                    url = eq(visitorStatsUrl(endpoint)),
                    clazz = eq(VisitorStatsSummaryApiResponse::class.java),
                    params = eq(visitorStatsParams(endpoint)),
                    enableCaching = eq(true),
                    cacheTimeToLive = eq(BaseRequest.DEFAULT_CACHE_LIFETIME),
                    forced = eq(false)
                )
            ).thenReturn(
                WPComGsonRequestBuilder.Response.Success(VisitorStatsSummaryApiResponse(), emptyList())
            )
        }
    }

    private fun visitorStatsUrl(endpoint: VisitorStatsEndpoint) = when (endpoint) {
        VisitorStatsEndpoint.VISITS -> WPCOMREST.sites.site(SITE_ID).stats.visits.urlV1_1
        VisitorStatsEndpoint.SUMMARY -> WPCOMREST.sites.site(SITE_ID).stats.summary.urlV1_1
    }

    private fun visitorStatsParams(endpoint: VisitorStatsEndpoint) = when (endpoint) {
        VisitorStatsEndpoint.VISITS -> mapOf(
            "unit" to "day",
            "date" to DATE,
            "quantity" to "1",
            "stat_fields" to "visitors"
        )

        VisitorStatsEndpoint.SUMMARY -> mapOf(
            "period" to "day",
            "date" to DATE
        )
    }

    private suspend fun fetchVisitorStats(endpoint: VisitorStatsEndpoint): ErrorMapping? {
        return when (endpoint) {
            VisitorStatsEndpoint.VISITS -> sut.fetchNewVisitorStats(
                site = site,
                granularity = StatsGranularity.DAYS,
                date = DATE,
                quantity = 1
            ).let { payload ->
                payload.error?.let { ErrorMapping(type = it.type.name, message = it.message) }
            }

            VisitorStatsEndpoint.SUMMARY -> sut.fetchVisitorStatsSummary(
                site = site,
                granularity = StatsGranularity.DAYS,
                date = DATE,
                force = false
            ).let { payload ->
                payload.error?.let {
                    ErrorMapping(
                        type = it.type.name,
                        message = it.message,
                        apiErrorCode = it.apiErrorCode,
                        original = it.original.name
                    )
                }
            }
        }
    }

    private fun createNetworkError(
        statusCode: Int = 404,
        apiError: String = INVALID_BLOG_ERROR_CODE,
        message: String = JETPACK_CONNECTION_MISSING_MESSAGE
    ) = WPComGsonNetworkError(
        BaseNetworkError(
            NOT_FOUND,
            message,
            VolleyError(NetworkResponse(statusCode, byteArrayOf(), emptyMap(), true))
        )
    ).apply {
        this.apiError = apiError
    }

    private enum class VisitorStatsEndpoint {
        VISITS,
        SUMMARY
    }

    private data class ErrorMapping(
        val type: String,
        val message: String?,
        val apiErrorCode: String? = null,
        val original: String? = null
    )

    private companion object {
        const val START_DATE = "2026-01-01T00:00:00"
        const val END_DATE = "2026-12-31T23:59:59"
        const val DATE = "2026-01-01"
        const val PER_PAGE = 100
        const val REVENUE_RANGE_ID = "year-date_created"
        const val SITE_ID = 123L
        const val INVALID_BLOG_ERROR_CODE = "invalid_blog"
        const val JETPACK_CONNECTION_MISSING_MESSAGE = "This blog does not have Jetpack connected"
        const val STATS_MODULE_DISABLED_MESSAGE = "This blog does not have the Stats module enabled"
    }
}
