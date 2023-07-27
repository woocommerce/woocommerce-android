package com.woocommerce.android.ui.mystore.domain

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.extensions.formatToYYYYmmDDhhmmss
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.mystore.data.StatsRepository
import com.woocommerce.android.ui.mystore.data.StatsRepository.StatsException
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType
import javax.inject.Inject

class GetStats @Inject constructor(
    private val selectedSite: SelectedSite,
    private val statsRepository: StatsRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val analyticsUpdateDataStore: AnalyticsUpdateDataStore
) {
    suspend operator fun invoke(
        refresh: Boolean,
        timeRangeSelection: StatsTimeRangeSelection
    ): Flow<LoadStatsResult> {
        val shouldRefreshRevenue =
            shouldUpdateStats(timeRangeSelection, refresh, AnalyticsUpdateDataStore.AnalyticData.REVENUE)
        val shouldRefreshVisitors =
            shouldUpdateStats(timeRangeSelection, refresh, AnalyticsUpdateDataStore.AnalyticData.VISITORS)
        return merge(
            hasOrders(),
            revenueStats(shouldRefreshRevenue, timeRangeSelection),
            visitorStats(shouldRefreshVisitors, timeRangeSelection)
        ).onEach { result ->
            if (result is LoadStatsResult.RevenueStatsSuccess && shouldRefreshRevenue) {
                analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                    rangeSelection = timeRangeSelection,
                    analyticData = AnalyticsUpdateDataStore.AnalyticData.REVENUE
                )
            }
            if (result is LoadStatsResult.VisitorsStatsSuccess && shouldRefreshVisitors) {
                analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                    rangeSelection = timeRangeSelection,
                    analyticData = AnalyticsUpdateDataStore.AnalyticData.VISITORS
                )
            }
        }.flowOn(coroutineDispatchers.computation)
    }

    private suspend fun hasOrders(): Flow<LoadStatsResult.HasOrders> =
        statsRepository.checkIfStoreHasNoOrders()
            .transform {
                if (it.getOrNull() == true) {
                    emit(LoadStatsResult.HasOrders(false))
                } else {
                    emit(LoadStatsResult.HasOrders(true))
                }
            }

    private suspend fun revenueStats(
        forceRefresh: Boolean,
        timeRangeSelection: StatsTimeRangeSelection
    ): Flow<LoadStatsResult> {
        val (startDate, endDate) = timeRangeSelection.formattedDateRanges
        return statsRepository.fetchRevenueStats(
            timeRangeSelection.selectionType.toStatsGranularity(),
            forceRefresh,
            startDate,
            endDate
        ).transform { result ->
            result.fold(
                onSuccess = { stats ->
                    appPrefsWrapper.setV4StatsSupported(true)
                    emit(LoadStatsResult.RevenueStatsSuccess(stats))
                },
                onFailure = {
                    if (isPluginNotActiveError(it)) {
                        appPrefsWrapper.setV4StatsSupported(false)
                        emit(LoadStatsResult.PluginNotActive)
                    } else {
                        emit(LoadStatsResult.RevenueStatsError)
                    }
                }
            )
        }
    }

    private suspend fun visitorStats(
        forceRefresh: Boolean,
        timeRangeSelection: StatsTimeRangeSelection
    ): Flow<LoadStatsResult> {
        val (startDate, endDate) = timeRangeSelection.formattedDateRanges
        // Visitor stats are only available for Jetpack connected sites
        return when (selectedSite.connectionType) {
            SiteConnectionType.Jetpack -> {
                statsRepository.fetchVisitorStats(
                    timeRangeSelection.selectionType.toStatsGranularity(),
                    forceRefresh,
                    startDate,
                    endDate
                ).transform { result ->
                        result.fold(
                            onSuccess = { stats -> emit(LoadStatsResult.VisitorsStatsSuccess(stats)) },
                            onFailure = { emit(LoadStatsResult.VisitorsStatsError) }
                        )
                    }
            }

            else -> selectedSite.connectionType?.let {
                flowOf(LoadStatsResult.VisitorStatUnavailable(it))
            } ?: emptyFlow()
        }
    }

    private fun isPluginNotActiveError(error: Throwable): Boolean =
        (error as? StatsException)?.error?.type == OrderStatsErrorType.PLUGIN_NOT_ACTIVE

    private val StatsTimeRangeSelection.formattedDateRanges: Pair<String, String>
        get() = this.currentRange.let {
            Pair(
                it.start.formatToYYYYmmDDhhmmss(),
                it.end.formatToYYYYmmDDhhmmss()
            )
        }

    private suspend fun shouldUpdateStats(
        selectionRange: StatsTimeRangeSelection,
        refresh: Boolean,
        analyticData: AnalyticsUpdateDataStore.AnalyticData
    ): Boolean {
        if (refresh) return true
        return analyticsUpdateDataStore
            .shouldUpdateAnalytics(
                rangeSelection = selectionRange,
                analyticData = analyticData
            )
            .firstOrNull() ?: true
    }

    sealed class LoadStatsResult {
        data class RevenueStatsSuccess(
            val stats: WCRevenueStatsModel?
        ) : LoadStatsResult()

        data class VisitorsStatsSuccess(
            val stats: Map<String, Int>
        ) : LoadStatsResult()

        data class HasOrders(
            val hasOrder: Boolean
        ) : LoadStatsResult()

        object RevenueStatsError : LoadStatsResult()
        object VisitorsStatsError : LoadStatsResult()
        object PluginNotActive : LoadStatsResult()
        data class VisitorStatUnavailable(
            val connectionType: SiteConnectionType
        ) : LoadStatsResult()
    }
}
