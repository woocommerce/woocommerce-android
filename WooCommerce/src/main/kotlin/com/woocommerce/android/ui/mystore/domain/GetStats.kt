package com.woocommerce.android.ui.mystore.domain

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.revenueStatsGranularity
import com.woocommerce.android.ui.analytics.ranges.visitorStatsGranularity
import com.woocommerce.android.ui.mystore.data.StatsRepository
import com.woocommerce.android.ui.mystore.data.StatsRepository.StatsException
import com.woocommerce.android.ui.mystore.data.asRevenueRangeId
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.DateUtils
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
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

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
            hasOrders(),
            revenueStats(selectedRange, shouldRefreshRevenue),
            visitorStats(selectedRange, shouldRefreshRevenue)
        ).onEach { result ->
            if (result is LoadStatsResult.RevenueStatsSuccess && shouldRefreshRevenue) {
                analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                    rangeSelection = selectedRange,
                    analyticData = AnalyticsUpdateDataStore.AnalyticData.REVENUE
                )
            }
            if (result is LoadStatsResult.VisitorsStatsSuccess && shouldRefreshVisitors) {
                analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                    rangeSelection = selectedRange,
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
        rangeSelection: StatsTimeRangeSelection,
        forceRefresh: Boolean
    ): Flow<LoadStatsResult> {
        val revenueRangeId = rangeSelection.selectionType.identifier.asRevenueRangeId(
            startDate = rangeSelection.currentRange.start,
            endDate = rangeSelection.currentRange.end
        )
        if (forceRefresh.not()) {
            statsRepository.getRevenueStatsById(revenueRangeId)
                .takeIf { it.isSuccess && it.getOrNull() != null }
                ?.let { return flowOf(LoadStatsResult.RevenueStatsSuccess(it.getOrNull())) }
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
                    LoadStatsResult.RevenueStatsSuccess(stats)
                },
                onFailure = {
                    if (isPluginNotActiveError(it)) {
                        appPrefsWrapper.setV4StatsSupported(false)
                        LoadStatsResult.PluginNotActive
                    } else {
                        LoadStatsResult.RevenueStatsError
                    }
                }
            )
        }
        return flowOf(revenueStatsResult)
    }

    private suspend fun visitorStats(
        rangeSelection: StatsTimeRangeSelection,
        forceRefresh: Boolean
    ): Flow<LoadStatsResult> {
        // Visitor stats are only available for Jetpack connected sites
        return when (selectedSite.connectionType) {
            SiteConnectionType.Jetpack -> {
                val result = statsRepository.fetchVisitorStats(
                    range = rangeSelection.currentRange,
                    granularity = rangeSelection.visitorStatsGranularity,
                    forced = forceRefresh
                )
                    .let { result ->
                        result.fold(
                            onSuccess = { stats -> LoadStatsResult.VisitorsStatsSuccess(stats) },
                            onFailure = { LoadStatsResult.VisitorsStatsError }
                        )
                    }
                flowOf(result)
            }

            else -> selectedSite.connectionType?.let {
                flowOf(LoadStatsResult.VisitorStatUnavailable(it))
            } ?: emptyFlow()
        }
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

fun StatsGranularity.asRangeSelection(dateUtils: DateUtils, locale: Locale? = null) =
    StatsTimeRangeSelection.SelectionType.from(this)
        .generateSelectionData(
            calendar = Calendar.getInstance(),
            locale = locale ?: Locale.getDefault(),
            referenceStartDate = dateUtils.getCurrentDateInSiteTimeZone() ?: Date(),
            referenceEndDate = dateUtils.getCurrentDateInSiteTimeZone() ?: Date()
        )
