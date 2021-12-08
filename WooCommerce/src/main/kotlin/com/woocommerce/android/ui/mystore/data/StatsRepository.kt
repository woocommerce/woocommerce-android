package com.woocommerce.android.ui.mystore.data

import com.woocommerce.android.AppConstants
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.DASHBOARD
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
    private val wcLeaderboardsStore: WCLeaderboardsStore,
    private val dateUtils: DateUtils
) {
    companion object {
        private val TAG = StatsRepository::class.java
    }

    suspend fun fetchRevenueStats(
        granularity: StatsGranularity,
        forced: Boolean,
        startDate: String = "",
        endDate: String = ""
    ): Flow<Result<WCRevenueStatsModel?>> =
        flow {
            val statsPayload = FetchRevenueStatsPayload(selectedSite.get(), granularity, startDate, endDate, forced)
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

    suspend fun fetchProductLeaderboards(
        forced: Boolean,
        granularity: StatsGranularity,
        quantity: Int,
        startDate: String = "",
        endDate: String = ""
    ): Flow<Result<List<WCTopPerformerProductModel>>> = flow {
        when (forced) {
            true -> wcLeaderboardsStore.fetchProductLeaderboards(
                site = selectedSite.get(),
                unit = granularity,
                quantity = quantity,
                queryTimeRange = getQueryTimeRange(startDate, endDate)
            )
            false -> wcLeaderboardsStore.fetchCachedProductLeaderboards(
                site = selectedSite.get(),
                unit = granularity
            )
        }.let { result ->
            val model = result.model
            if (result.isError || model == null) {
                val resultError: Result<List<WCTopPerformerProductModel>> = Result.failure(
                    Exception(result.error?.message.orEmpty())
                )
                emit(resultError)
            } else {
                emit(Result.success(model))
            }
        }
    }

    private fun getQueryTimeRange(startDate: String, endDate: String): LongRange? {
        if (startDate.isEmpty() || endDate.isEmpty()) return null
        val startTime = dateUtils.fromIso8601Format(startDate)?.time ?: return null
        val endTime = dateUtils.fromIso8601Format(endDate)?.time ?: return null
        return LongRange(startTime, endTime)
    }

    suspend fun checkIfStoreHasNoOrders(): Flow<Result<Boolean>> = flow {
        val result = withTimeoutOrNull(AppConstants.REQUEST_TIMEOUT) {
            wcOrderStore.fetchHasOrders(selectedSite.get(), status = null)
        }
        if (result?.isError == false) {
            val hasNoOrders = result.rowsAffected == 0
            emit(Result.success(hasNoOrders))
        } else {
            val errorMessage = result?.error?.message ?: "Timeout"
            WooLog.e(
                DASHBOARD,
                "$TAG - Error fetching whether orders exist: $errorMessage"
            )
            emit(Result.failure(Exception(errorMessage)))
        }
    }

    data class StatsException(val error: OrderStatsError?) : Exception()
}
