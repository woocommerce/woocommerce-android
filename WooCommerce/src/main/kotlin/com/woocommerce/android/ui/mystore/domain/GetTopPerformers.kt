package com.woocommerce.android.ui.mystore.domain

import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.mystore.data.StatsRepository
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity
import org.wordpress.android.fluxc.store.WCStatsStore
import javax.inject.Inject

class GetTopPerformers @Inject constructor(
    private val statsRepository: StatsRepository,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val analyticsUpdateDataStore: AnalyticsUpdateDataStore
) {
    private companion object {
        const val NUM_TOP_PERFORMERS = 5
    }

    fun observeTopPerformers(granularity: WCStatsStore.StatsGranularity): Flow<List<TopPerformerProduct>> =
        statsRepository.observeTopPerformers(granularity)
            .map { topPerformersProductEntities ->
                topPerformersProductEntities
                    .map { it.toTopPerformerProduct() }
                    .sortDescByQuantityAndTotal()
            }.flowOn(coroutineDispatchers.computation)

    suspend fun fetchTopPerformers(
        granularity: WCStatsStore.StatsGranularity,
        refresh: Boolean = false,
        topPerformersCount: Int = NUM_TOP_PERFORMERS,
    ): Result<Unit> {
        val selectionType = StatsTimeRangeSelection.SelectionType.from(granularity)
        val isForcedRefresh = shouldUpdateStats(selectionType, refresh)
        return statsRepository.fetchTopPerformerProducts(isForcedRefresh, granularity, topPerformersCount)
            .let { result ->
                if (result.isSuccess && isForcedRefresh) {
                    analyticsUpdateDataStore.storeLastAnalyticsUpdate(selectionType)
                }
                result
            }
    }

    private suspend fun shouldUpdateStats(
        selectionType: StatsTimeRangeSelection.SelectionType,
        refresh: Boolean
    ): Boolean {
        if (refresh) return true
        return analyticsUpdateDataStore
            .shouldUpdateAnalytics(selectionType)
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
