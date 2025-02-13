package org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats

import java.util.Locale
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
     * Makes a GET call to `/wc/v4/reports/revenue/stats`, retrieving data for the given
     * WooCommerce [SiteModel].
     *
     * @param[site] the site to fetch stats data for
     * @param[granularity] one of 'hour', 'day', 'week', 'month', or 'year'
     * @param[startDate] the start date to include in ISO format (YYYY-MM-dd'T'HH:mm:ss)
     * @param[endDate] the end date to include in ISO format (YYYY-MM-dd'T'HH:mm:ss)
     * @param[perPage] the number of items to return in a paginated response
     * @param[forceRefresh] a boolean value indicating whether we should avoid cached data
     * @param[revenueRangeId] a unique id for this request. We will use this id to save the response in the local db.
     * @param[dateType] override the "woocommerce_date_type" option that is used for revenue reports.
     * Product stats are based on the order creation date, while the order/revenue
     * stats are based on a store option in the analytics settings with the order paid date as the default.
     * In WC version 8.6+, a new parameter `date_type` is available to override the date type so that we can
     * show the order/revenue and product stats based on the same date column, order creation date.
     *
     * Possible non-generic errors:
     * [OrderStatsErrorType.INVALID_PARAM] if [granularity], [startDate], or [endDate] are invalid or incompatible
     */
    suspend fun fetchRevenueStats(
        site: SiteModel,
        granularity: StatsGranularity,
        startDate: String,
        endDate: String,
        perPage: Int,
        forceRefresh: Boolean = false,
        revenueRangeId: String = "",
        dateType: String = "date_created",
    ): FetchRevenueStatsResponsePayload {
        val url = WOOCOMMERCE.reports.revenue.stats.pathV4Analytics
        val params = mapOf(
            "interval" to OrderStatsApiUnit.fromStatsGranularity(granularity).toString(),
            "after" to startDate,
            "before" to endDate,
            "per_page" to perPage.toString(),
            "order" to "asc",
            "force_cache_refresh" to forceRefresh.toString(),
            "date_type" to dateType
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
                        val model = WCRevenueStatsModel().apply {
                            this.localSiteId = site.id
                            this.interval = granularity.toString()
                            this.data = it.intervals.toString()
                            this.total = it.totals.toString()
                            this.startDate = startDate
                            this.endDate = endDate
                            this.rangeId = revenueRangeId
                        }

                    FetchRevenueStatsResponsePayload(site, granularity, model)
                } ?: FetchRevenueStatsResponsePayload(
                        OrderStatsError(type = OrderStatsErrorType.GENERIC_ERROR,
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
                    val payload = FetchRevenueStatsAvailabilityResponsePayload(site, true)
                    dispatcher.dispatch(WCStatsActionBuilder.newFetchedRevenueStatsAvailabilityAction(payload))
                }
                is WPAPIResponse.Error -> {
                    val orderError = response.error.toOrderError()
                    val payload = FetchRevenueStatsAvailabilityResponsePayload(orderError, site, false)
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
                val model = WCNewVisitorStatsModel().apply {
                    this.localSiteId = site.id
                    this.granularity = granularity.toString()
                    this.fields = statsData.fields.toString()
                    this.data = statsData.data.toString()
                    this.quantity = quantity.toString()
                    this.date = date
                    endDate?.let { this.endDate = it }
                    startDate?.let {
                        this.startDate = startDate
                        this.isCustomField = true
                    }
                }

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
}
