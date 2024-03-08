package com.woocommerce.android.ui.mystore.domain

import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.mystore.data.StatsRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.locale.LocaleProvider
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
    private val analyticsUpdateDataStore: AnalyticsUpdateDataStore,
    private val localeProvider: LocaleProvider,
    private val dateUtils: DateUtils
) {
    private companion object {
        const val NUM_TOP_PERFORMERS = 5
    }

    fun observeTopPerformers(granularity: WCStatsStore.StatsGranularity): Flow<List<TopPerformerProduct>> {
        val selectionRange = granularity.asRangeSelection(
            dateUtils = dateUtils,
            locale = localeProvider.provideLocale()
        )

        return statsRepository.observeTopPerformers(selectionRange.currentRange)
            .map { topPerformersProductEntities ->
                topPerformersProductEntities
                    .map { it.toTopPerformerProduct() }
                    .sortDescByQuantityAndTotal()
            }.flowOn(coroutineDispatchers.computation)
    }

    suspend fun fetchTopPerformers(
        granularity: WCStatsStore.StatsGranularity,
        refresh: Boolean = false,
        topPerformersCount: Int = NUM_TOP_PERFORMERS,
    ): Result<Unit> {
        val selectionRange = granularity.asRangeSelection(
            dateUtils = dateUtils,
            locale = localeProvider.provideLocale()
        )
        val isForcedRefresh = shouldUpdateStats(selectionRange, refresh)
        return statsRepository.fetchTopPerformerProducts(
            forceRefresh = isForcedRefresh,
            range = selectionRange.currentRange,
            quantity = topPerformersCount
        )
            .let { result ->
                if (result.isSuccess && isForcedRefresh) {
                    analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                        rangeSelection = selectionRange,
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
