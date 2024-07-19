package com.woocommerce.android.ui.dashboard.domain

import com.woocommerce.android.extensions.formatToYYYYmmDDhhmmss
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.dashboard.data.StatsRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.ResultWithOutdatedFlag
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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

    suspend operator fun invoke(
        selectedRange: StatsTimeRangeSelection,
        refresh: Boolean = false
    ) = flow {
        val isForcedRefresh = shouldUpdateStats(selectedRange, refresh)
        val startDate = selectedRange.currentRange.start.formatToYYYYmmDDhhmmss()
        val endDate = selectedRange.currentRange.end.formatToYYYYmmDDhhmmss()

        val cachedTopPerformers =
            statsRepository.getTopPerformers(startDate, endDate).map { topPerformersProductEntity ->
                topPerformersProductEntity.toTopPerformerProduct()
            }.sortDescByQuantityAndTotal()

        emit(
            when {
                cachedTopPerformers.isEmpty() && isForcedRefresh.not() ->
                    TopPerformerResult.Success(
                        ResultWithOutdatedFlag(cachedTopPerformers, false)
                    )
                cachedTopPerformers.isEmpty() && isForcedRefresh && refresh.not() -> TopPerformerResult.Loading
                else -> TopPerformerResult.Success(ResultWithOutdatedFlag(cachedTopPerformers, isForcedRefresh))
            }
        )

        if (isForcedRefresh.not()) return@flow

        fetchTopPerformers(
            selectedRange = selectedRange,
            refresh = refresh
        ).fold(
            onFailure = { e -> emit(TopPerformerResult.Error(e)) },
            onSuccess = {
                statsRepository.getTopPerformers(startDate, endDate)
                    .map { topPerformersProductEntity ->
                        topPerformersProductEntity.toTopPerformerProduct()
                    }
                    .sortDescByQuantityAndTotal()
                    .let {
                        emit(
                            TopPerformerResult.Success(
                                ResultWithOutdatedFlag(
                                    value = it,
                                    isOutdated = false
                                )
                            )
                        )
                    }
            }
        )
    }.flowOn(coroutineDispatchers.computation)

    private suspend fun fetchTopPerformers(
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

    sealed class TopPerformerResult {
        data object Loading : TopPerformerResult()
        data class Error(val exception: Throwable) : TopPerformerResult()
        data class Success(val topPerformers: ResultWithOutdatedFlag<List<TopPerformerProduct>>) : TopPerformerResult()
    }
}
