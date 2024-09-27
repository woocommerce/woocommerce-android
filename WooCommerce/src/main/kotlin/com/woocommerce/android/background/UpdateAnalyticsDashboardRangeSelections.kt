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
    suspend operator fun invoke(): Result<Unit> {
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

            val errors = asyncCalls.awaitAll().filter { it.isFailure }.map {
                it.exceptionOrNull()
                    ?: Exception("${UpdateAnalyticsDashboardRangeSelections::class.java.name} Unknown error")
            }

            when {
                errors.isEmpty() -> {
                    Result.success(Unit)
                }
                errors.size == 1 -> {
                    Result.failure(errors.first())
                }
                else -> {
                    Result.failure(MultipleErrorsException(errors))
                }
            }
        }
    }
}
