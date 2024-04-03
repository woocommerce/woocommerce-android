package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.AppConstants
import com.woocommerce.android.WooException
import com.woocommerce.android.extensions.formatToYYYYmmDD
import com.woocommerce.android.extensions.formatToYYYYmmDDhhmmss
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.DASHBOARD
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
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
    private val dispatchers: CoroutineDispatchers
) {
    companion object {
        private val TAG = StatsRepository::class.java

        // Minimum supported version to use /wc-analytics/leaderboards/products instead of slower endpoint
        // /wc-analytics/leaderboards. More info https://github.com/woocommerce/woocommerce-android/issues/6688
        private const val PRODUCT_ONLY_LEADERBOARD_REPORT_MIN_WC_VERSION = "6.7.0"
        private const val AN_HOUR_IN_MILLIS = 3600000
    }

    suspend fun fetchRevenueStats(
        range: StatsTimeRange,
        granularity: StatsGranularity,
        forced: Boolean,
        revenueRangeId: String = ""
    ): Result<WCRevenueStatsModel?> = fetchRevenueStats(
        range = range,
        granularity = granularity,
        forced = forced,
        revenueRangeId = revenueRangeId,
        site = selectedSite.get()
    )

    private suspend fun fetchRevenueStats(
        range: StatsTimeRange,
        granularity: StatsGranularity,
        forced: Boolean,
        revenueRangeId: String = "",
        site: SiteModel
    ): Result<WCRevenueStatsModel?> {
        val result = wcStatsStore.fetchRevenueStats(
            FetchRevenueStatsPayload(
                site = site,
                granularity = granularity,
                startDate = range.start.formatToYYYYmmDDhhmmss(),
                endDate = range.end.formatToYYYYmmDDhhmmss(),
                forced = forced,
                revenueRangeId = revenueRangeId
            )
        )

        return if (!result.isError) {
            val revenueStatsModel = withContext(dispatchers.io) {
                wcStatsStore.getRawRevenueStats(
                    site,
                    result.granularity,
                    result.startDate!!,
                    result.endDate!!
                )
            }
            Result.success(revenueStatsModel)
        } else {
            val errorMessage = result.error?.message ?: "Timeout"
            WooLog.e(
                DASHBOARD,
                "$TAG - Error fetching revenue stats: $errorMessage"
            )
            val exception = StatsException(error = result.error)
            Result.failure(exception)
        }
    }

    suspend fun getRevenueStatsById(
        revenueRangeId: String
    ): Result<WCRevenueStatsModel?> = withContext(dispatchers.io) {
        Result.success(
            wcStatsStore.getRawRevenueStatsFromRangeId(
                selectedSite.get(),
                revenueRangeId
            )
        )
    }

    suspend fun fetchVisitorStats(
        range: StatsTimeRange,
        granularity: StatsGranularity,
        forced: Boolean
    ): Result<Map<String, Int>> = fetchVisitorStats(
        range = range,
        granularity = granularity,
        forced = forced,
        site = selectedSite.get()
    )

    private suspend fun fetchVisitorStats(
        range: StatsTimeRange,
        granularity: StatsGranularity,
        forced: Boolean,
        site: SiteModel
    ): Result<Map<String, Int>> {
        val visitsPayload = FetchNewVisitorStatsPayload(
            site = site,
            granularity = granularity,
            startDate = range.start.formatToYYYYmmDDhhmmss(),
            endDate = range.end.formatToYYYYmmDDhhmmss(),
            forced = forced
        )
        val result = wcStatsStore.fetchNewVisitorStats(visitsPayload)
        return if (!result.isError) {
            val visitorStats = withContext(dispatchers.io) {
                wcStatsStore.getNewVisitorStats(
                    site,
                    result.granularity,
                    result.quantity,
                    result.date,
                    result.isCustomField
                )
            }
            Result.success(visitorStats)
        } else {
            val errorMessage = result.error?.message ?: "Timeout"
            WooLog.e(
                DASHBOARD,
                "$TAG - Error fetching visitor stats: $errorMessage"
            )
            Result.failure(Exception(errorMessage))
        }
    }

    fun observeTopPerformers(
        range: StatsTimeRange,
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
        range: StatsTimeRange,
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

    /**
     * This function will return the site stats optional including visitor stats.
     * Even if the includeVisitorStats flag is set to true, errors fetching visitor
     * will be handled as null and only errors fetching the revenue stats will be processed.
     */
    suspend fun fetchStats(
        range: StatsTimeRange,
        revenueStatsGranularity: StatsGranularity,
        visitorStatsGranularity: StatsGranularity,
        forced: Boolean,
        includeVisitorStats: Boolean,
        site: SiteModel = selectedSite.get()
    ): Result<SiteStats> = coroutineScope {
        val fetchVisitorStats = if (includeVisitorStats) {
            async {
                fetchVisitorStats(
                    range = range,
                    granularity = visitorStatsGranularity,
                    forced = forced,
                    site = site
                )
            }
        } else {
            CompletableDeferred(Result.success(emptyMap()))
        }

        val fetchRevenueStats = async {
            fetchRevenueStats(
                range = range,
                granularity = revenueStatsGranularity,
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
        return@coroutineScope if (revenueStats.isFailure) {
            Result.failure(revenueStats.exceptionOrNull()!!)
        } else {
            Result.success(
                SiteStats(
                    revenue = revenueStats.getOrNull(),
                    visitors = visitorStats.getOrNull(),
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
}

fun String.asRevenueRangeId(
    startDate: Date,
    endDate: Date
): String {
    val startDateString = startDate.formatToYYYYmmDD()
    val endDateString = endDate.formatToYYYYmmDD()
    return "$this$startDateString$endDateString"
}
