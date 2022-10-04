package com.woocommerce.android.ui.mystore.data

import com.woocommerce.android.AppConstants
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.DASHBOARD
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
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

    suspend fun fetchProductLeaderboards(
        forceRefresh: Boolean,
        granularity: StatsGranularity,
        quantity: Int
    ): Flow<Result<List<WCTopPerformerProductModel>>> = flow {
        when (forceRefresh) {
            true -> wcLeaderboardsStore.fetchProductLeaderboards(
                site = selectedSite.get(),
                unit = granularity,
                quantity = quantity,
                addProductsPath = supportsProductOnlyLeaderboardEndpoint(),
                forceRefresh = forceRefresh
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
    data class SiteStats(val revenue: WCRevenueStatsModel?, val visitors: Map<String, Int>?)

    private suspend fun fetchRevenueStats(
        granularity: StatsGranularity,
        forced: Boolean,
        site: SiteModel = selectedSite.get()
    ): WooResult<WCRevenueStatsModel?> {
        val statsPayload = FetchRevenueStatsPayload(
            site = site,
            granularity = granularity,
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
        granularity: StatsGranularity,
        forced: Boolean,
        site: SiteModel = selectedSite.get()
    ): WooResult<Map<String, Int>> {
        val visitsPayload = FetchNewVisitorStatsPayload(
            site = site,
            granularity = granularity,
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

    suspend fun fetchStats(
        granularity: StatsGranularity,
        forced: Boolean,
        site: SiteModel = selectedSite.get()
    ): WooResult<SiteStats> = coroutineScope {
        val fetchVisitorStats = async {
            fetchVisitorStats(
                granularity = granularity,
                forced = forced,
                site = site
            )
        }
        val fetchRevenueStats = async {
            fetchRevenueStats(
                granularity = granularity,
                forced = forced,
                site = site
            )
        }
        val visitorStats = fetchVisitorStats.await()
        val revenueStats = fetchRevenueStats.await()

        return@coroutineScope if (visitorStats.isError || revenueStats.isError) {
            val error = WooError(
                type = WooErrorType.GENERIC_ERROR,
                original = BaseRequest.GenericErrorType.UNKNOWN,
                message = "Error fetching site stats for site ${site.siteId}"
            )
            WooResult(error)
        } else {
            WooResult(SiteStats(revenue = revenueStats.model, visitors = visitorStats.model))
        }
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
