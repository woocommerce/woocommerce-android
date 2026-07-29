package org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCNewVisitorStatsModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.settings.WCAnalyticsOrderDateType
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WPComSiteInvalidationEvent
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WPComSiteInvalidationListener
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WPComSiteInvalidationReason
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import org.wordpress.android.fluxc.store.WCStatsStore.FetchNewVisitorStatsResponsePayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsAvailabilityResponsePayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsResponsePayload
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsError
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import java.util.Locale
import java.util.Optional
import javax.inject.Inject

class OrderStatsRestClient @Inject constructor(
    private val dispatcher: Dispatcher,
    private val wooNetwork: WooNetwork,
    private val wpComNetwork: WPComNetwork,
    private val wpComSiteInvalidationListener: Optional<WPComSiteInvalidationListener>,
    private val coroutineEngine: CoroutineEngine
) {
    enum class OrderStatsApiUnit {
        HOUR, DAY, WEEK, MONTH, YEAR;

        companion object {
            fun fromStatsGranularity(granularity: StatsGranularity): OrderStatsApiUnit {
                return when (granularity) {
                    StatsGranularity.HOURS -> HOUR
                    StatsGranularity.DAYS -> DAY
                    StatsGranularity.WEEKS -> WEEK
                    StatsGranularity.MONTHS -> MONTH
                    StatsGranularity.YEARS -> YEAR
                }
            }
        }

        override fun toString() = name.lowercase(Locale.getDefault())
    }

    /**
     * Makes a GET call to `/wc-analytics/reports/revenue/stats`, retrieving data for the given
     * WooCommerce [SiteModel].
     *
     * Sends a `date_type` parameter when one is provided. Otherwise, the server uses the store's configured
     * `woocommerce_date_type` setting (default: `date_paid`).
     *
     * @param[site] the site to fetch stats data for
     * @param[granularity] one of 'hour', 'day', 'week', 'month', or 'year'
     * @param[startDate] the start date to include in ISO format (YYYY-MM-dd'T'HH:mm:ss)
     * @param[endDate] the end date to include in ISO format (YYYY-MM-dd'T'HH:mm:ss)
     * @param[perPage] the number of items to return in a paginated response
     * @param[forceRefresh] a boolean value indicating whether we should avoid cached data
     * @param[revenueRangeId] a unique id for this request. We will use this id to save the response in the local db.
     * @param[currency] the currency to use when fetching revenue stats.
     * @param[orderDateType] the order date field to use when grouping revenue stats.
     *
     * Possible non-generic errors:
     * [OrderStatsErrorType.INVALID_PARAM] if [granularity], [startDate], or [endDate] are invalid or incompatible
     */
    @Suppress("LongParameterList")
    suspend fun fetchRevenueStats(
        site: SiteModel,
        granularity: StatsGranularity,
        startDate: String,
        endDate: String,
        perPage: Int,
        forceRefresh: Boolean = false,
        revenueRangeId: String,
        currency: String? = null,
        orderDateType: WCAnalyticsOrderDateType? = null,
    ): FetchRevenueStatsResponsePayload {
        val url = WOOCOMMERCE.reports.revenue.stats.pathV4Analytics
        val params = buildMap {
            put("interval", OrderStatsApiUnit.fromStatsGranularity(granularity).toString())
            put("after", startDate)
            put("before", endDate)
            put("per_page", perPage.toString())
            put("order", "asc")
            put("force_cache_refresh", forceRefresh.toString())
            put("_fields", "totals,intervals")
            currency?.let { put("currency", it) }
            orderDateType?.let { put("date_type", it.value) }
        }

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = RevenueStatsApiResponse::class.java,
            enableCaching = true,
            forced = forceRefresh
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                response.data?.let {
                    val model = WCRevenueStatsModel(
                        localSiteId = site.localId(),
                        interval = granularity.toString(),
                        data = it.intervals.toString(),
                        total = it.totals.toString(),
                        startDate = startDate,
                        endDate = endDate,
                        rangeId = revenueRangeId,
                    )

                    FetchRevenueStatsResponsePayload(site, granularity, model)
                } ?: FetchRevenueStatsResponsePayload(
                    OrderStatsError(
                        type = OrderStatsErrorType.GENERIC_ERROR,
                        message = "Success response with empty data"
                    ),
                    site,
                    granularity
                )
            }

            is WPAPIResponse.Error -> {
                val orderError = response.error.toOrderError()
                FetchRevenueStatsResponsePayload(orderError, site, granularity)
            }
        }
    }

    /**
     * Makes a GET call to `/wc-analytics/reports/orders/stats`, retrieving order analytics for the given
     * WooCommerce [SiteModel].
     */
    @Suppress("LongParameterList")
    suspend fun fetchOrdersStats(
        site: SiteModel,
        granularity: StatsGranularity,
        startDate: String,
        endDate: String,
        perPage: Int,
        forceRefresh: Boolean = false,
        orderStatsRangeId: String,
    ): FetchRevenueStatsResponsePayload {
        val url = WOOCOMMERCE.reports.orders.stats.pathV4Analytics
        val params = mapOf(
            "interval" to OrderStatsApiUnit.fromStatsGranularity(granularity).toString(),
            "after" to startDate,
            "before" to endDate,
            "per_page" to perPage.toString(),
            "order" to "asc",
            "force_cache_refresh" to forceRefresh.toString(),
            "_fields" to "totals,intervals",
        )

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = RevenueStatsApiResponse::class.java,
            enableCaching = true,
            forced = forceRefresh
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                response.data?.let {
                    val model = WCRevenueStatsModel(
                        localSiteId = site.localId(),
                        interval = granularity.toString(),
                        data = it.intervals.toString(),
                        total = it.totals.toString(),
                        startDate = startDate,
                        endDate = endDate,
                        rangeId = orderStatsRangeId,
                    )

                    FetchRevenueStatsResponsePayload(site, granularity, model)
                } ?: FetchRevenueStatsResponsePayload(
                    OrderStatsError(
                        type = OrderStatsErrorType.GENERIC_ERROR,
                        message = "Success response with empty data"
                    ),
                    site,
                    granularity
                )
            }

            is WPAPIResponse.Error -> {
                val orderError = response.error.toOrderError()
                FetchRevenueStatsResponsePayload(orderError, site, granularity)
            }
        }
    }

    /**
     * Makes a GET call to `/wc/v4/reports/revenue/stats`, to check if the site supports the v4 stats api.
     * If v4 stats is not available for the site, returns [OrderStatsErrorType.PLUGIN_NOT_ACTIVE]
     *
     * @param[site] the site to fetch stats data for
     * @param[startDate] the current date to include in ISO format (YYYY-MM-dd'T'HH:mm:ss)
     *
     * Since only the response code is needed to verify if the v4 stats is supported or not,
     * this method has been optimised:
     * The interval param is set to [OrderStatsApiUnit.YEAR] by default
     * The after param is set to the current date by default
     * The _fields param is added to retrieve only the `Totals` field from the api
     */
    fun fetchRevenueStatsAvailability(site: SiteModel, startDate: String) {
        coroutineEngine.launch(AppLog.T.API, this, "fetchRevenueStatsAvailability") {
            val url = WOOCOMMERCE.reports.revenue.stats.pathV4Analytics
            val params = mapOf(
                "interval" to OrderStatsApiUnit.YEAR.toString(),
                "after" to startDate,
                "_fields" to "totals"
            )

            val response = wooNetwork.executeGetGsonRequest(
                site = site,
                path = url,
                params = params,
                clazz = RevenueStatsApiResponse::class.java
            )

            when (response) {
                is WPAPIResponse.Success -> {
                    val payload = FetchRevenueStatsAvailabilityResponsePayload(site)
                    dispatcher.dispatch(WCStatsActionBuilder.newFetchedRevenueStatsAvailabilityAction(payload))
                }
                is WPAPIResponse.Error -> {
                    val orderError = response.error.toOrderError()
                    val payload = FetchRevenueStatsAvailabilityResponsePayload(orderError, site)
                    dispatcher.dispatch(WCStatsActionBuilder.newFetchedRevenueStatsAvailabilityAction(payload))
                }
            }
        }
    }

    suspend fun fetchNewVisitorStats(
        site: SiteModel,
        granularity: StatsGranularity,
        date: String,
        quantity: Int,
        force: Boolean = false,
        startDate: String? = null,
        endDate: String? = null
    ): FetchNewVisitorStatsResponsePayload {
        val url = WPCOMREST.sites.site(site.siteId).stats.visits.urlV1_1
        val params = mapOf(
            "unit" to OrderStatsApiUnit.fromStatsGranularity(granularity).toString(),
            "date" to date,
            "quantity" to quantity.toString(),
            "stat_fields" to "visitors"
        )

        val response = wpComNetwork.executeGetGsonRequest(
            url = url,
            params = params,
            clazz = VisitorStatsApiResponse::class.java,
            enableCaching = true,
            forced = force
        )

        return when (response) {
            is WPComGsonRequestBuilder.Response.Success -> {
                val statsData = response.data
                val model = WCNewVisitorStatsModel(
                    localSiteId = site.localId(),
                    granularity = granularity.toString(),
                    fields = statsData.fields.toString(),
                    data = statsData.data.toString(),
                    quantity = quantity.toString(),
                    date = date,
                    endDate = endDate ?: "",
                    startDate = startDate ?: "",
                )

                FetchNewVisitorStatsResponsePayload(site, granularity, model)
            }

            is WPComGsonRequestBuilder.Response.Error -> {
                notifyIfWPComSiteInvalidated(site, response.error)
                val orderError = response.error.toOrderError()
                FetchNewVisitorStatsResponsePayload(orderError, site, granularity)
            }
        }
    }

    suspend fun fetchVisitorStatsSummary(
        site: SiteModel,
        granularity: StatsGranularity,
        date: String,
        force: Boolean
    ): WooPayload<VisitorStatsSummaryApiResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.summary.urlV1_1
        val params = mapOf(
            "period" to OrderStatsApiUnit.fromStatsGranularity(granularity).toString(),
            "date" to date
        )

        val response = wpComNetwork.executeGetGsonRequest(
            url = url,
            params = params,
            clazz = VisitorStatsSummaryApiResponse::class.java,
            enableCaching = true,
            forced = force
        )

        return when (response) {
            is WPComGsonRequestBuilder.Response.Success -> WooPayload(response.data)
            is WPComGsonRequestBuilder.Response.Error -> {
                notifyIfWPComSiteInvalidated(site, response.error)
                WooPayload(response.error.toWooError())
            }
        }
    }

    private fun notifyIfWPComSiteInvalidated(site: SiteModel, error: WPComGsonNetworkError) {
        if (
            error.volleyError?.networkResponse?.statusCode == HTTP_NOT_FOUND &&
            error.apiError == INVALID_BLOG_ERROR_CODE &&
            error.message == JETPACK_CONNECTION_MISSING_ERROR_MESSAGE
        ) {
            wpComSiteInvalidationListener.ifPresent {
                it.onSiteInvalidated(
                    WPComSiteInvalidationEvent(
                        siteId = site.siteId,
                        reason = WPComSiteInvalidationReason.JETPACK_CONNECTION_MISSING
                    )
                )
            }
        }
    }

    private fun WPAPINetworkError.toOrderError() = networkErrorToOrderError(errorCode, message)
    private fun WPComGsonNetworkError.toOrderError() = networkErrorToOrderError(apiError, message)

    private fun networkErrorToOrderError(errorCode: String?, message: String?): OrderStatsError {
        val orderStatsErrorType = when (errorCode) {
            "rest_invalid_param" -> OrderStatsErrorType.INVALID_PARAM
            "rest_no_route" -> OrderStatsErrorType.PLUGIN_NOT_ACTIVE
            else -> OrderStatsErrorType.fromString(errorCode.orEmpty())
        }
        return OrderStatsError(orderStatsErrorType, message.orEmpty())
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
        const val INVALID_BLOG_ERROR_CODE = "invalid_blog"
        const val JETPACK_CONNECTION_MISSING_ERROR_MESSAGE = "This blog does not have Jetpack connected"
    }
}
