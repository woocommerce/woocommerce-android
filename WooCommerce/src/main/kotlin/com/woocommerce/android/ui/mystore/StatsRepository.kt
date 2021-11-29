package com.woocommerce.android.ui.mystore

import com.woocommerce.android.AppConstants
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.DASHBOARD
import kotlinx.coroutines.withTimeoutOrNull
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.store.WCLeaderboardsStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.*
import javax.inject.Inject

class StatsRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wcStatsStore: WCStatsStore,
    @Suppress("UnusedPrivateMember", "Required to ensure the WCOrderStore is initialized!")
    private val wcOrderStore: WCOrderStore,
    private val wcLeaderboardsStore: WCLeaderboardsStore
) {
    companion object {
        private val TAG = MyStorePresenter::class.java
    }

    suspend fun fetchRevenueStats(granularity: StatsGranularity, forced: Boolean): Result<WCRevenueStatsModel?> {
        val statsPayload = FetchRevenueStatsPayload(selectedSite.get(), granularity, forced = forced)
        val result = wcStatsStore.fetchRevenueStats(statsPayload)

        return if (!result.isError) {
            val revenueStatsModel = wcStatsStore.getRawRevenueStats(
                selectedSite.get(), result.granularity, result.startDate!!, result.endDate!!
            )
            Result.success(revenueStatsModel)
        } else {
            val errorMessage = result?.error?.message ?: "Timeout"
            WooLog.e(
                DASHBOARD,
                "$TAG - Error fetching revenue stats: $errorMessage"
            )
            var exception = StatsException(
                error = result.error
            )
            Result.failure(exception)
        }
    }

    suspend fun fetchVisitorStats(granularity: StatsGranularity, forced: Boolean): Result<Map<String, Int>> {
        val visitsPayload = FetchNewVisitorStatsPayload(selectedSite.get(), granularity, forced)
        val result = wcStatsStore.fetchNewVisitorStats(visitsPayload)

        return if (!result.isError) {
            val visitorStats = wcStatsStore.getNewVisitorStats(
                selectedSite.get(), result.granularity, result.quantity, result.date, result.isCustomField
            )
            Result.success(visitorStats)
        } else {
            val errorMessage = result?.error?.message ?: "Timeout"
            WooLog.e(
                DASHBOARD,
                "$TAG - Error fetching visitor stats: $errorMessage"
            )
            Result.failure(Exception(errorMessage))
        }
    }

    suspend fun fetchProductLeaderboards(granularity: StatsGranularity, quantity: Int, forced: Boolean):
        Result<List<WCTopPerformerProductModel>> {
        return when (forced) {
            true -> wcLeaderboardsStore.fetchProductLeaderboards(
                site = selectedSite.get(),
                unit = granularity,
                quantity = quantity
            )
            false -> wcLeaderboardsStore.fetchCachedProductLeaderboards(
                site = selectedSite.get(),
                unit = granularity
            )
        }.let { result ->
            val model = result.model
            if (result.isError || model == null) {
                Result.failure(Exception(result.error?.message.orEmpty()))
            } else {
                Result.success(model)
            }
        }
    }

    suspend fun checkIfStoreHasNoOrders(): Result<Boolean> {
        val result = withTimeoutOrNull(AppConstants.REQUEST_TIMEOUT) {
            wcOrderStore.fetchHasOrders(selectedSite.get(), status = null)
        }
        return if (result?.isError == false) {
            val hasNoOrders = result.rowsAffected == 0
            Result.success(hasNoOrders)
        } else {
            val errorMessage = result?.error?.message ?: "Timeout"
            WooLog.e(
                DASHBOARD,
                "$TAG - Error fetching whether orders exist: $errorMessage"
            )

            Result.failure(Exception(errorMessage))
        }
    }

    data class StatsException(val error: OrderStatsError?) : Exception()
}
