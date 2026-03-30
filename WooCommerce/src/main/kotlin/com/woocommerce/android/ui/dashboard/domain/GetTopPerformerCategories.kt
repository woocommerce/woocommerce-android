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
import org.wordpress.android.fluxc.persistence.entity.TopPerformerCategoryEntity
import javax.inject.Inject

class GetTopPerformerCategories @Inject constructor(
    private val statsRepository: StatsRepository,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val analyticsUpdateDataStore: AnalyticsUpdateDataStore
) {
    private companion object {
        const val NUM_TOP_CATEGORIES = 5
    }

    suspend operator fun invoke(
        selectedRange: StatsTimeRangeSelection,
        refresh: Boolean = false
    ) = flow {
        val isForcedRefresh = shouldUpdateStats(selectedRange, refresh)
        val startDate = selectedRange.currentRange.start.formatToYYYYmmDDhhmmss()
        val endDate = selectedRange.currentRange.end.formatToYYYYmmDDhhmmss()

        val cachedCategories =
            statsRepository.getTopPerformerCategories(startDate, endDate).map { entity ->
                entity.toTopPerformerCategory()
            }.sortDescByQuantityAndTotal()

        emit(
            when {
                cachedCategories.isEmpty() && isForcedRefresh.not() ->
                    TopPerformerCategoryResult.Success(
                        ResultWithOutdatedFlag(cachedCategories, false)
                    )
                cachedCategories.isEmpty() && isForcedRefresh && refresh.not() ->
                    TopPerformerCategoryResult.Loading
                else -> TopPerformerCategoryResult.Success(
                    ResultWithOutdatedFlag(cachedCategories, isForcedRefresh)
                )
            }
        )

        if (isForcedRefresh.not()) return@flow

        fetchTopPerformerCategories(
            selectedRange = selectedRange,
            refresh = refresh
        ).fold(
            onFailure = { e -> emit(TopPerformerCategoryResult.Error(e)) },
            onSuccess = {
                statsRepository.getTopPerformerCategories(startDate, endDate)
                    .map { entity -> entity.toTopPerformerCategory() }
                    .sortDescByQuantityAndTotal()
                    .let {
                        emit(
                            TopPerformerCategoryResult.Success(
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

    private suspend fun fetchTopPerformerCategories(
        selectedRange: StatsTimeRangeSelection,
        refresh: Boolean = false,
        topCategoriesCount: Int = NUM_TOP_CATEGORIES,
    ): Result<Unit> {
        val isForcedRefresh = shouldUpdateStats(selectedRange, refresh)
        return statsRepository.fetchTopPerformerCategories(
            forceRefresh = isForcedRefresh,
            range = selectedRange.currentRange,
            quantity = topCategoriesCount
        )
            .let { result ->
                if (result.isSuccess && isForcedRefresh) {
                    analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                        rangeSelection = selectedRange,
                        analyticData = AnalyticsUpdateDataStore.AnalyticData.TOP_PERFORMER_CATEGORIES
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
                analyticData = AnalyticsUpdateDataStore.AnalyticData.TOP_PERFORMER_CATEGORIES
            )
            .firstOrNull() ?: true
    }

    private fun List<TopPerformerCategory>.sortDescByQuantityAndTotal() =
        sortedWith(
            compareByDescending(TopPerformerCategory::quantity)
                .thenByDescending(TopPerformerCategory::total)
        )

    private fun TopPerformerCategoryEntity.toTopPerformerCategory() =
        TopPerformerCategory(
            categoryId = categoryId.value,
            name = name,
            quantity = quantity,
            currency = currency,
            total = total
        )

    data class TopPerformerCategory(
        val categoryId: Long,
        val name: String,
        val quantity: Int,
        val currency: String,
        val total: Double
    )

    sealed class TopPerformerCategoryResult {
        data object Loading : TopPerformerCategoryResult()
        data class Error(val exception: Throwable) : TopPerformerCategoryResult()
        data class Success(
            val topCategories: ResultWithOutdatedFlag<List<TopPerformerCategory>>
        ) : TopPerformerCategoryResult()
    }
}
