package org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCNewVisitorStatsModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComNetwork
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
import javax.inject.Inject

class OrderStatsRestClient @Inject constructor(
    private val dispatcher: Dispatcher,
    private val wooNetwork: WooNetwork,
    private val wpComNetwork: WPComNetwork,
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
     * Fetches revenue stats for the given WooCommerce [SiteModel].
     *
     * Tries the WP.com analytics proxy endpoint first (`/wc/v3/woocommerce-analytics/proxy/...`)
     * with `date_type=created`, which aligns the data with what wp-admin (CIAB) shows — including
     * all order statuses (pending, checkout-draft, etc.) in the aggregation.
     *
     * Falls back to the local `/wc-analytics/reports/revenue/stats` endpoint if the proxy is
     * unavailable (e.g., the `woocommerce-analytics` plugin is not active on the store).
     *
     * @param[site] the site to fetch stats data for
     * @param[granularity] one of 'hour', 'day', 'week', 'month', or 'year'
     * @param[startDate] the start date to include in ISO format (YYYY-MM-dd'T'HH:mm:ss)
     * @param[endDate] the end date to include in ISO format (YYYY-MM-dd'T'HH:mm:ss)
     * @param[perPage] the number of items to return in a paginated response
     * @param[forceRefresh] a boolean value indicating whether we should avoid cached data
     * @param[revenueRangeId] a unique id for this request. We will use this id to save the response in the local db.
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
    ): FetchRevenueStatsResponsePayload {
        val proxyResult = tryFetchFromProxy(
            site, granularity, startDate, endDate, revenueRangeId, forceRefresh
        )
        if (proxyResult != null) return proxyResult

        return fetchFromLocalApi(
            site, granularity, startDate, endDate, perPage, forceRefresh, revenueRangeId
        )
    }

    /**
     * Attempts to fetch revenue stats from the WP.com analytics proxy endpoint.
     * Uses `date_type=created` to match the wp-admin CIAB dashboard behavior, which
     * includes all order statuses (pending, checkout-draft, etc.) in the aggregation.
     *
     * Returns `null` if the proxy endpoint is unavailable so the caller can fall back.
     */
    @Suppress("LongParameterList")
    private suspend fun tryFetchFromProxy(
        site: SiteModel,
        granularity: StatsGranularity,
        startDate: String,
        endDate: String,
        revenueRangeId: String,
        forceRefresh: Boolean,
    ): FetchRevenueStatsResponsePayload? {
        val params = mapOf(
            "from" to startDate,
            "to" to endDate,
            "interval" to OrderStatsApiUnit.fromStatsGranularity(granularity).toString(),
            "date_type" to "created",
        )

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = PROXY_STATS_PATH,
            params = params,
            clazz = ProxyRevenueStatsApiResponse::class.java,
            enableCaching = true,
            forced = forceRefresh
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                try {
                    response.data?.let {
                        val model = mapProxyResponseToModel(
                            it, site, granularity, startDate, endDate, revenueRangeId
                        )
                        FetchRevenueStatsResponsePayload(site, granularity, model)
                    }
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    AppLog.e(AppLog.T.API, "Failed to parse proxy stats response, falling back", e)
                    null
                }
            }
            is WPAPIResponse.Error -> null
        }
    }

    /**
     * Fetches revenue stats from the local `/wc-analytics/reports/revenue/stats` endpoint.
     * This is the original data source that queries the `wc_order_stats` table directly.
     */
    @Suppress("LongParameterList")
    private suspend fun fetchFromLocalApi(
        site: SiteModel,
        granularity: StatsGranularity,
        startDate: String,
        endDate: String,
        perPage: Int,
        forceRefresh: Boolean,
        revenueRangeId: String,
    ): FetchRevenueStatsResponsePayload {
        val url = WOOCOMMERCE.reports.revenue.stats.pathV4Analytics
        val params = mapOf(
            "interval" to OrderStatsApiUnit.fromStatsGranularity(granularity).toString(),
            "after" to startDate,
            "before" to endDate,
            "per_page" to perPage.toString(),
            "order" to "asc",
            "force_cache_refresh" to forceRefresh.toString(),
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
     * Maps the WP.com analytics proxy response to the same [WCRevenueStatsModel] format used
     * by the local API, so the rest of the data pipeline remains unchanged.
     *
     * The proxy uses different field names and string values:
     * - `orders_no` (string) → `orders_count` (int)
     * - `orders_value_net` (string) → `net_revenue` (double)
     * - `time_interval` (string) → `interval` (string)
     * - `average_order_value` (string) → `avg_order_value` (double)
     * - `total_items` (string) → `num_items_sold` (int)
     */
    @Suppress("LongParameterList")
    private fun mapProxyResponseToModel(
        proxyResponse: ProxyRevenueStatsApiResponse,
        site: SiteModel,
        granularity: StatsGranularity,
        startDate: String,
        endDate: String,
        revenueRangeId: String,
    ): WCRevenueStatsModel {
        val totalsJson = proxyResponse.summary?.let {
            mapProxySummaryToTotals(it.asJsonObject)
        } ?: JsonObject()

        val intervalsJson = proxyResponse.data?.let {
            mapProxyDataToIntervals(it.asJsonArray)
        } ?: JsonArray()

        return WCRevenueStatsModel(
            localSiteId = site.localId(),
            interval = granularity.toString(),
            data = intervalsJson.toString(),
            total = totalsJson.toString(),
            startDate = startDate,
            endDate = endDate,
            rangeId = revenueRangeId,
        )
    }

    private fun mapProxySummaryToTotals(summary: JsonObject): JsonObject {
        return JsonObject().apply {
            addProperty("orders_count", summary.getAsString("orders_no").toIntOrNull() ?: 0)
            addProperty("net_revenue", summary.getAsString("orders_value_net").toDoubleOrNull() ?: 0.0)
            addProperty("total_sales", summary.getAsString("total_sales").toDoubleOrNull() ?: 0.0)
            addProperty("avg_order_value", summary.getAsString("average_order_value").toDoubleOrNull() ?: 0.0)
            addProperty("num_items_sold", summary.getAsString("total_items").toIntOrNull() ?: 0)
        }
    }

    private fun mapProxyDataToIntervals(data: JsonArray): JsonArray {
        return JsonArray().apply {
            data.forEach { element ->
                val item = element.asJsonObject
                add(JsonObject().apply {
                    addProperty("interval", item.getAsString("time_interval"))
                    add("subtotals", JsonObject().apply {
                        addProperty("orders_count", item.getAsString("orders_no").toLongOrNull() ?: 0L)
                        addProperty("net_revenue", item.getAsString("orders_value_net").toDoubleOrNull() ?: 0.0)
                        addProperty("total_sales", item.getAsString("total_sales").toDoubleOrNull() ?: 0.0)
                        addProperty(
                            "avg_order_value",
                            item.getAsString("average_order_value").toDoubleOrNull() ?: 0.0
                        )
                    })
                })
            }
        }
    }

    private fun JsonObject.getAsString(key: String): String {
        return get(key)?.let { if (it.isJsonNull) "" else it.asString } ?: ""
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
            is WPComGsonRequestBuilder.Response.Error -> WooPayload(response.error.toWooError())
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

    companion object {
        // WP.com analytics proxy endpoint used by the wp-admin CIAB dashboard.
        // Available on stores with the woocommerce-analytics plugin active.
        private const val PROXY_STATS_PATH =
            "/wc/v3/woocommerce-analytics/proxy/reports/orders/by-date"
    }
}
