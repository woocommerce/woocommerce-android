package com.woocommerce.android.ui.dashboard.domain

import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.dashboard.data.StatsRepository
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity
import javax.inject.Inject

class GetTopPerformers @Inject constructor(
    private val statsRepository: StatsRepository,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val analyticsUpdateDataStore: AnalyticsUpdateDataStore
) {
    private companion object {
        const val NUM_TOP_PERFORMERS = 5
    }

    fun observeTopPerformers(selectedRange: StatsTimeRangeSelection): Flow<List<TopPerformerProduct>> {
        return statsRepository.observeTopPerformers(selectedRange.currentRange)
            .map { topPerformersProductEntities ->
                topPerformersProductEntities
                    .map { it.toTopPerformerProduct() }
                    .sortDescByQuantityAndTotal()
            }.flowOn(coroutineDispatchers.computation)
    }

    suspend fun fetchTopPerformers(
        selectedRange: StatsTimeRangeSelection,
        refresh: Boolean = false,
        topPerformersCount: Int = NUM_TOP_PERFORMERS,
    ): Result<Unit> {
        val isForcedRefresh = shouldUpdateStats(selectedRange, refresh)
        return statsRepository.fetchTopPerformerProducts(
            forceRefresh = isForcedRefresh,
            range = selectedRange.currentRange,
            quantity = topPerformersCount
        )
            .let { result ->
                if (result.isSuccess && isForcedRefresh) {
                    analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                        rangeSelection = selectedRange,
                        analyticData = AnalyticsUpdateDataStore.AnalyticData.TOP_PERFORMERS
                    )
                }
                result
            }
    }

    private suspend fun shouldUpdateStats(
        selectionRange: StatsTimeRangeSelection,
        refresh: Boolean
    ): Boolean {
        if (refresh) return true
        return analyticsUpdateDataStore
            .shouldUpdateAnalytics(
                rangeSelection = selectionRange,
                analyticData = AnalyticsUpdateDataStore.AnalyticData.TOP_PERFORMERS
            )
            .firstOrNull() ?: true
    }

    private fun List<TopPerformerProduct>.sortDescByQuantityAndTotal() =
        sortedWith(
            compareByDescending(TopPerformerProduct::quantity)
                .thenByDescending(TopPerformerProduct::total)
        )

    private fun TopPerformerProductEntity.toTopPerformerProduct() =
        TopPerformerProduct(
            productId = productId.value,
            name = name,
            quantity = quantity,
            currency = currency,
            total = total,
            imageUrl = imageUrl
        )

    data class TopPerformerProduct(
        val productId: Long,
        val name: String,
        val quantity: Int,
        val currency: String,
        val total: Double,
        val imageUrl: String?
    )
}
