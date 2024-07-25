package com.woocommerce.android.background

import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.ui.dashboard.stats.GetSelectedRangeForDashboardStats
import com.woocommerce.android.ui.dashboard.topperformers.GetSelectedRangeForTopPerformers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateAnalyticsDashboardRangeSelections @Inject constructor(
    private val getSelectedRangeForTopPerformers: GetSelectedRangeForTopPerformers,
    private val getSelectedRangeForDashboardStats: GetSelectedRangeForDashboardStats,
    private val updateAnalyticsData: UpdateAnalyticsDataByRangeSelection
) {
    suspend operator fun invoke(): Boolean {
        val dashboardRangeSelections = buildSet {
            getSelectedRangeForTopPerformers.invoke().first().let { add(it) }
            getSelectedRangeForDashboardStats.invoke().first().let { add(it) }
        }
        val forceCardUpdates = listOf(AnalyticsCards.Products, AnalyticsCards.Revenue, AnalyticsCards.Session)
        return coroutineScope {
            val asyncCalls = dashboardRangeSelections.map { selectedRange ->
                async {
                    updateAnalyticsData(
                        selectedRange = selectedRange,
                        forceCardUpdates = forceCardUpdates
                    )
                }
            }
            asyncCalls.awaitAll().all { it }
        }
    }
}
