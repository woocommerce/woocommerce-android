package com.woocommerce.android.ui.mystore.data

import com.woocommerce.android.AppConstants
import com.woocommerce.android.WooException
import com.woocommerce.android.extensions.formatToYYYYmmDD
import com.woocommerce.android.extensions.formatToYYYYmmDDhhmmss
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.DASHBOARD
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeoutOrNull
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity
import org.wordpress.android.fluxc.store.WCLeaderboardsStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchNewVisitorStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsError
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.DateUtils
import java.util.Date
import javax.inject.Inject

@Suppress("TooManyFunctions")
class StatsRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wcStatsStore: WCStatsStore,
    @Suppress("UnusedPrivateMember", "Required to ensure the WCOrderStore is initialized!")
    private val wcOrderStore: WCOrderStore,
    private val wcLeaderboardsStore: WCLeaderboardsStore,
    private val wooCommerceStore: WooCommerceStore,
) {
    companion object {
        private val TAG = StatsRepository::class.java

        // Minimum supported version to use /wc-analytics/leaderboards/products instead of slower endpoint
        // /wc-analytics/leaderboards. More info https://github.com/woocommerce/woocommerce-android/issues/6688
        private const val PRODUCT_ONLY_LEADERBOARD_REPORT_MIN_WC_VERSION = "6.7.0"
        private const val AN_HOUR_IN_MILLIS = 3600000
    }

    suspend fun fetchRevenueStats(
        range: AnalyticsHubTimeRange,
        granularity: StatsGranularity,
        forced: Boolean,
        revenueRangeId: String = ""
    ): Flow<Result<WCRevenueStatsModel?>> =
        flow {
            val result = wcStatsStore.fetchRevenueStats(
                FetchRevenueStatsPayload(
                    site = selectedSite.get(),
                    granularity = granularity,
                    startDate = range.start.formatToYYYYmmDDhhmmss(),
                    endDate = range.end.formatToYYYYmmDDhhmmss(),
                    forced = forced,
                    revenueRangeId = revenueRangeId
                )
            )

            if (!result.isError) {
                val revenueStatsModel = wcStatsStore.getRawRevenueStats(
                    selectedSite.get(), result.granularity, result.startDate!!, result.endDate!!
                )
                Result.success(revenueStatsModel)
                emit(Result.success(revenueStatsModel))
            } else {
                val errorMessage = result.error?.message ?: "Timeout"
                WooLog.e(
                    DASHBOARD,
                    "$TAG - Error fetching revenue stats: $errorMessage"
                )
                val exception = StatsException(error = result.error)
                emit(Result.failure(exception))
            }
        }

    suspend fun getRevenueStatsById(
        revenueRangeId: String
    ): Flow<Result<WCRevenueStatsModel?>> = flow {
        wcStatsStore.getRawRevenueStatsFromRangeId(
            selectedSite.get(), revenueRangeId
        ).let { emit(Result.success(it)) }
    }

    suspend fun fetchVisitorStats(
        range: AnalyticsHubTimeRange,
        granularity: StatsGranularity,
        forced: Boolean
    ): Flow<Result<Map<String, Int>>> =
        flow {
            val visitsPayload = FetchNewVisitorStatsPayload(
                site = selectedSite.get(),
                granularity = granularity,
                startDate = range.start.formatToYYYYmmDDhhmmss(),
                endDate = range.end.formatToYYYYmmDDhhmmss(),
                forced = forced
            )
            val result = wcStatsStore.fetchNewVisitorStats(visitsPayload)
            if (!result.isError) {
                val visitorStats = wcStatsStore.getNewVisitorStats(
                    selectedSite.get(), result.granularity, result.quantity, result.date, result.isCustomField
                )
                emit(Result.success(visitorStats))
            } else {
                val errorMessage = result.error?.message ?: "Timeout"
                WooLog.e(
                    DASHBOARD,
                    "$TAG - Error fetching visitor stats: $errorMessage"
                )
                emit(Result.failure(Exception(errorMessage)))
            }
        }

    fun observeTopPerformers(
        range: AnalyticsHubTimeRange,
    ): Flow<List<TopPerformerProductEntity>> {
        val siteModel = selectedSite.get()
        val datePeriod = DateUtils.getDatePeriod(
            startDate = range.start.formatToYYYYmmDDhhmmss(),
            endDate = range.end.formatToYYYYmmDDhhmmss()
        )
        return wcLeaderboardsStore
            .observeTopPerformerProducts(siteModel, datePeriod)
            .flowOn(Dispatchers.IO)
    }

    suspend fun getTopPerformers(
        startDate: String,
        endDate: String
    ): List<TopPerformerProductEntity> {
        val siteModel = selectedSite.get()
        val datePeriod = DateUtils.getDatePeriod(startDate, endDate)
        return wcLeaderboardsStore.getCachedTopPerformerProducts(siteModel, datePeriod)
    }

    suspend fun fetchTopPerformerProducts(
        forceRefresh: Boolean,
        range: AnalyticsHubTimeRange,
        quantity: Int
    ): Result<Unit> {
        return fetchTopPerformerProducts(
            forceRefresh = forceRefresh,
            startDate = range.start.formatToYYYYmmDDhhmmss(),
            endDate = range.end.formatToYYYYmmDDhhmmss(),
            quantity = quantity
        )
    }

    suspend fun fetchTopPerformerProducts(
        forceRefresh: Boolean,
        startDate: String,
        endDate: String,
        quantity: Int
    ): Result<Unit> {
        val siteModel = selectedSite.get()
        val datePeriod = DateUtils.getDatePeriod(startDate, endDate)
        val cachedTopPerformers = wcLeaderboardsStore.getCachedTopPerformerProducts(
            site = siteModel,
            datePeriod = datePeriod
        )
        return if (forceRefresh || cachedTopPerformers.isEmpty() || cachedTopPerformers.expired()) {
            val supportOnlyLegacyEndpoint = supportsProductOnlyLeaderboardAndReportEndpoint().not()
            val result = if (supportOnlyLegacyEndpoint) {
                wcLeaderboardsStore.fetchTopPerformerProductsLegacy(
                    site = siteModel,
                    startDate = startDate,
                    endDate = endDate,
                    quantity = quantity
                )
            } else {
                wcLeaderboardsStore.fetchTopPerformerProducts(
                    site = siteModel,
                    startDate = startDate,
                    endDate = endDate,
                    quantity = quantity
                )
            }
            when {
                result.isError -> Result.failure(WooException(result.error))
                else -> Result.success(Unit)
            }
        } else {
            Result.success(Unit)
        }
    }

    private fun List<TopPerformerProductEntity>.expired(): Boolean =
        any { topPerformerProductEntity ->
            System.currentTimeMillis() - topPerformerProductEntity.millisSinceLastUpdated > AN_HOUR_IN_MILLIS
        }

    suspend fun checkIfStoreHasNoOrders(): Flow<Result<Boolean>> = flow {
        val result = withTimeoutOrNull(AppConstants.REQUEST_TIMEOUT) {
            wcOrderStore.hasOrders(selectedSite.get())
        }

        when (result) {
            is WCOrderStore.HasOrdersResult.Success -> {
                emit(Result.success(!result.hasOrders))
            }

            is WCOrderStore.HasOrdersResult.Failure, null -> {
                val errorMessage = (result as? WCOrderStore.HasOrdersResult.Failure)?.error?.message ?: "Timeout"
                WooLog.e(
                    DASHBOARD,
                    "$TAG - Error fetching whether orders exist: $errorMessage"
                )
                emit(Result.failure(Exception(errorMessage)))
            }
        }
    }

    data class StatsException(val error: OrderStatsError?) : Exception()
    data class SiteStats(
        val revenue: WCRevenueStatsModel?,
        val visitors: Map<String, Int>?,
        val currencyCode: String
    )

    private suspend fun fetchRevenueStats(
        range: AnalyticsHubTimeRange,
        granularity: StatsGranularity,
        forced: Boolean,
        site: SiteModel = selectedSite.get()
    ): WooResult<WCRevenueStatsModel?> {
        val statsPayload = FetchRevenueStatsPayload(
            site = site,
            granularity = granularity,
            startDate = range.start.formatToYYYYmmDD(),
            endDate = range.end.formatToYYYYmmDD(),
            forced = forced
        )

        val result = wcStatsStore.fetchRevenueStats(statsPayload)

        return if (result.isError) {
            val error = result.error.toWooError()
            WooLog.e(
                DASHBOARD,
                "$TAG - Error fetching revenue stats: ${error.message}"
            )
            WooResult(error)
        } else {
            val revenueStatsModel = wcStatsStore.getRawRevenueStats(
                site = site,
                granularity = result.granularity,
                startDate = result.startDate!!,
                endDate = result.endDate!!
            )
            WooResult(revenueStatsModel)
        }
    }

    private suspend fun fetchVisitorStats(
        range: AnalyticsHubTimeRange,
        granularity: StatsGranularity,
        forced: Boolean,
        site: SiteModel = selectedSite.get()
    ): WooResult<Map<String, Int>> {
        val visitsPayload = FetchNewVisitorStatsPayload(
            site = site,
            granularity = granularity,
            startDate = range.start.formatToYYYYmmDDhhmmss(),
            endDate = range.end.formatToYYYYmmDDhhmmss(),
            forced = forced
        )

        val result = wcStatsStore.fetchNewVisitorStats(visitsPayload)

        return if (result.isError) {
            val error = result.error.toWooError()
            WooLog.e(
                DASHBOARD,
                "$TAG - Error fetching visitor stats: ${error.message}"
            )
            WooResult(error)
        } else {
            val visitorStats = wcStatsStore.getNewVisitorStats(
                selectedSite.get(), result.granularity, result.quantity, result.date, result.isCustomField
            )
            WooResult(visitorStats)
        }
    }

    /**
     * This function will return the site stats optional including visitor stats.
     * Even if the includeVisitorStats flag is set to true, errors fetching visitor
     * will be handled as null and only errors fetching the revenue stats will be processed.
     */
    suspend fun fetchStats(
        range: AnalyticsHubTimeRange,
        granularity: StatsGranularity,
        forced: Boolean,
        includeVisitorStats: Boolean,
        site: SiteModel = selectedSite.get()
    ): WooResult<SiteStats> = coroutineScope {
        val fetchVisitorStats = if (includeVisitorStats) {
            async {
                fetchVisitorStats(
                    range = range,
                    granularity = granularity,
                    forced = forced,
                    site = site
                )
            }
        } else {
            CompletableDeferred(WooResult(emptyMap()))
        }

        val fetchRevenueStats = async {
            fetchRevenueStats(
                range = range,
                granularity = granularity,
                forced = forced,
                site = site
            )
        }
        val visitorStats = fetchVisitorStats.await()
        val revenueStats = fetchRevenueStats.await()
        val siteCurrencyCode = wooCommerceStore.getSiteSettings(site)?.currencyCode.orEmpty()

        // If there was an error fetching the visitor stats chances are that is because
        // jetpack is not properly configure to return stats. So we take into account
        // only revenue stats to return process the error response.
        return@coroutineScope if (revenueStats.isError) {
            val error = WooError(
                type = WooErrorType.GENERIC_ERROR,
                original = BaseRequest.GenericErrorType.UNKNOWN,
                message = "Error fetching site stats for site ${site.siteId}"
            )
            WooResult(error)
        } else {
            WooResult(
                SiteStats(
                    revenue = revenueStats.model,
                    visitors = visitorStats.model,
                    currencyCode = siteCurrencyCode
                )
            )
        }
    }

    private fun supportsProductOnlyLeaderboardAndReportEndpoint(): Boolean {
        val currentWooCoreVersion =
            wooCommerceStore.getSitePlugin(selectedSite.get(), WooCommerceStore.WooPlugin.WOO_CORE)?.version ?: "0.0"
        return currentWooCoreVersion.semverCompareTo(PRODUCT_ONLY_LEADERBOARD_REPORT_MIN_WC_VERSION) >= 0
    }

    private fun OrderStatsError?.toWooError(): WooError {
        if (this == null) {
            return WooError(
                type = WooErrorType.TIMEOUT,
                original = BaseRequest.GenericErrorType.TIMEOUT,
                message = "Timeout"
            )
        }

        val type = when (this.type) {
            WCStatsStore.OrderStatsErrorType.RESPONSE_NULL -> WooErrorType.INVALID_RESPONSE
            WCStatsStore.OrderStatsErrorType.INVALID_PARAM -> WooErrorType.INVALID_PARAM
            WCStatsStore.OrderStatsErrorType.PLUGIN_NOT_ACTIVE -> WooErrorType.PLUGIN_NOT_ACTIVE
            WCStatsStore.OrderStatsErrorType.GENERIC_ERROR -> WooErrorType.GENERIC_ERROR
        }

        val original = when (this.type) {
            WCStatsStore.OrderStatsErrorType.RESPONSE_NULL -> BaseRequest.GenericErrorType.INVALID_RESPONSE
            WCStatsStore.OrderStatsErrorType.INVALID_PARAM -> BaseRequest.GenericErrorType.INVALID_RESPONSE
            WCStatsStore.OrderStatsErrorType.PLUGIN_NOT_ACTIVE -> BaseRequest.GenericErrorType.NOT_FOUND
            WCStatsStore.OrderStatsErrorType.GENERIC_ERROR -> BaseRequest.GenericErrorType.UNKNOWN
        }

        return WooError(
            type = type,
            original = original,
            message = this.message
        )
    }
}

fun String.asRevenueRangeId(
    startDate: Date,
    endDate: Date
): String {
    val startDateString = startDate.formatToYYYYmmDD()
    val endDateString = endDate.formatToYYYYmmDD()
    return "$this$startDateString$endDateString"
}
