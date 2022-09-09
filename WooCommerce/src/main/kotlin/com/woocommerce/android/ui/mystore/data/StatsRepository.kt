package com.woocommerce.android.ui.mystore.data

import com.woocommerce.android.AppConstants
import com.woocommerce.android.WooException
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.DASHBOARD
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
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
import org.wordpress.android.fluxc.store.WooCommerceStore.WooPlugin.WOO_CORE
import javax.inject.Inject

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
        private const val PRODUCT_ONLY_LEADERBOARD_MIN_WC_VERSION = "6.7.0"
        private const val AN_HOUR_IN_MILLIS = 3600000
    }

    suspend fun fetchRevenueStats(
        granularity: StatsGranularity,
        forced: Boolean
    ): Flow<Result<WCRevenueStatsModel?>> = flow {
        val statsPayload = FetchRevenueStatsPayload(selectedSite.get(), granularity, forced = forced)
        val result = wcStatsStore.fetchRevenueStats(statsPayload)

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

    suspend fun fetchVisitorStats(granularity: StatsGranularity, forced: Boolean): Flow<Result<Map<String, Int>>> =
        flow {
            val visitsPayload = FetchNewVisitorStatsPayload(selectedSite.get(), granularity, forced)
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
        granularity: StatsGranularity,
    ): Flow<List<TopPerformerProductEntity>> =
        wcLeaderboardsStore.observeTopPerformerProducts(selectedSite.get().siteId, granularity)

    suspend fun fetchTopPerformerProducts(
        forceRefresh: Boolean,
        granularity: StatsGranularity,
        quantity: Int
    ): Result<Unit> {
        val cachedTopPerformers = wcLeaderboardsStore.getCachedTopPerformerProducts(
            siteId = selectedSite.get().siteId,
            granularity = granularity
        )
        return if (forceRefresh || cachedTopPerformers.isEmpty() || cachedTopPerformers.expired()) {
            val result = wcLeaderboardsStore.fetchTopPerformerProducts(
                site = selectedSite.get(),
                granularity = granularity,
                quantity = quantity,
                addProductsPath = supportsProductOnlyLeaderboardEndpoint(),
                forceRefresh = forceRefresh
            )
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
            wcOrderStore.fetchHasOrders(selectedSite.get(), status = null)
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

    private fun supportsProductOnlyLeaderboardEndpoint(): Boolean {
        val currentWooCoreVersion =
            wooCommerceStore.getSitePlugin(selectedSite.get(), WOO_CORE)?.version ?: "0.0"
        return currentWooCoreVersion.semverCompareTo(PRODUCT_ONLY_LEADERBOARD_MIN_WC_VERSION) >= 0
    }

    data class StatsException(val error: OrderStatsError?) : Exception()
}
