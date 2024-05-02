package com.woocommerce.android.ui.dashboard.stats

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.analytics.ranges.revenueStatsGranularity
import com.woocommerce.android.ui.analytics.ranges.visitorStatsGranularity
import com.woocommerce.android.ui.analytics.ranges.visitorSummaryStatsGranularity
import com.woocommerce.android.ui.dashboard.data.StatsRepository
import com.woocommerce.android.ui.dashboard.data.StatsRepository.StatsException
import com.woocommerce.android.ui.dashboard.data.asRevenueRangeId
import com.woocommerce.android.ui.dashboard.stats.GetStats.LoadStatsResult.PluginNotActive
import com.woocommerce.android.ui.dashboard.stats.GetStats.LoadStatsResult.RevenueStatsError
import com.woocommerce.android.ui.dashboard.stats.GetStats.LoadStatsResult.RevenueStatsSuccess
import com.woocommerce.android.ui.dashboard.stats.GetStats.LoadStatsResult.VisitorStatUnavailable
import com.woocommerce.android.ui.dashboard.stats.GetStats.LoadStatsResult.VisitorsStatsError
import com.woocommerce.android.ui.dashboard.stats.GetStats.LoadStatsResult.VisitorsStatsSuccess
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

class GetStats @Inject constructor(
    private val selectedSite: SelectedSite,
    private val statsRepository: StatsRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val analyticsUpdateDataStore: AnalyticsUpdateDataStore,
) {
    suspend operator fun invoke(refresh: Boolean, selectedRange: StatsTimeRangeSelection): Flow<LoadStatsResult> {
        val shouldRefreshRevenue =
            shouldUpdateStats(selectedRange, refresh, AnalyticsUpdateDataStore.AnalyticData.REVENUE)
        val shouldRefreshVisitors =
            shouldUpdateStats(selectedRange, refresh, AnalyticsUpdateDataStore.AnalyticData.VISITORS)
        return merge(
            revenueStats(selectedRange, shouldRefreshRevenue),
            visitorStats(selectedRange, shouldRefreshVisitors)
        ).onEach { result ->
            if (result is RevenueStatsSuccess && shouldRefreshRevenue) {
                analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                    rangeSelection = selectedRange,
                    analyticData = AnalyticsUpdateDataStore.AnalyticData.REVENUE
                )
            }
            if (result is VisitorsStatsSuccess && shouldRefreshVisitors) {
                analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                    rangeSelection = selectedRange,
                    analyticData = AnalyticsUpdateDataStore.AnalyticData.VISITORS
                )
            }
        }.flowOn(coroutineDispatchers.computation)
    }

    private fun revenueStats(
        rangeSelection: StatsTimeRangeSelection,
        forceRefresh: Boolean
    ): Flow<LoadStatsResult> = flow {
        val revenueRangeId = rangeSelection.selectionType.identifier.asRevenueRangeId(
            startDate = rangeSelection.currentRange.start,
            endDate = rangeSelection.currentRange.end
        )
        if (forceRefresh.not()) {
            statsRepository.getRevenueStatsById(revenueRangeId)
                .takeIf { it.isSuccess && it.getOrNull() != null }
                ?.let {
                    emit(RevenueStatsSuccess(it.getOrNull()))
                    return@flow
                }
        }

        val revenueStatsResult = statsRepository.fetchRevenueStats(
            range = rangeSelection.currentRange,
            granularity = rangeSelection.revenueStatsGranularity,
            forced = forceRefresh,
            revenueRangeId = revenueRangeId
        ).let { result ->
            result.fold(
                onSuccess = { stats ->
                    appPrefsWrapper.setV4StatsSupported(true)
                    RevenueStatsSuccess(stats)
                },
                onFailure = {
                    if (isPluginNotActiveError(it)) {
                        appPrefsWrapper.setV4StatsSupported(false)
                        PluginNotActive
                    } else {
                        RevenueStatsError
                    }
                }
            )
        }
        emit(revenueStatsResult)
    }

    private fun visitorStats(
        rangeSelection: StatsTimeRangeSelection,
        forceRefresh: Boolean
    ): Flow<LoadStatsResult> {
        // Visitor stats are only available for Jetpack connected sites
        return when (selectedSite.connectionType) {
            SiteConnectionType.Jetpack -> combine(
                totalVisitorStats(rangeSelection, forceRefresh),
                individualVisitorStats(rangeSelection, forceRefresh)
            ) { total, individual ->
                if (total.isFailure || individual.isFailure) {
                    VisitorsStatsError
                } else {
                    VisitorsStatsSuccess(individual.getOrThrow(), total.getOrThrow())
                }
            }

            else -> selectedSite.connectionType?.let {
                flowOf(VisitorStatUnavailable)
            } ?: emptyFlow()
        }
    }

    private fun individualVisitorStats(
        rangeSelection: StatsTimeRangeSelection,
        forceRefresh: Boolean
    ): Flow<Result<Map<String, Int>>> = flow {
        emit(
            statsRepository.fetchVisitorStats(
                range = rangeSelection.currentRange,
                granularity = rangeSelection.visitorStatsGranularity,
                forced = forceRefresh
            )
        )
    }

    private fun totalVisitorStats(
        rangeSelection: StatsTimeRangeSelection,
        forceRefresh: Boolean
    ): Flow<Result<Int?>> = flow {
        if (rangeSelection.selectionType == SelectionType.CUSTOM &&
            rangeSelection.currentRange.end.time - rangeSelection.currentRange.start.time > 1.days.inWholeMilliseconds
        ) {
            // Total visitor stats are not available for custom ranges
            emit(Result.success(null))
            return@flow
        }

        if (!forceRefresh) {
            statsRepository.getTotalVisitorStats(
                date = rangeSelection.currentRange.end,
                granularity = rangeSelection.visitorSummaryStatsGranularity
            )?.let { emit(Result.success(it)) }
        }

        emit(
            statsRepository.fetchTotalVisitorStats(
                date = rangeSelection.currentRange.end,
                granularity = rangeSelection.visitorSummaryStatsGranularity,
                forced = forceRefresh
            )
        )
    }

    private fun isPluginNotActiveError(error: Throwable): Boolean =
        (error as? StatsException)?.error?.type == OrderStatsErrorType.PLUGIN_NOT_ACTIVE

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
            val stats: Map<String, Int>,
            val totalVisitorCount: Int?
        ) : LoadStatsResult()

        data object RevenueStatsError : LoadStatsResult()
        data object VisitorsStatsError : LoadStatsResult()
        data object PluginNotActive : LoadStatsResult()
        data object VisitorStatUnavailable : LoadStatsResult()
    }
}
