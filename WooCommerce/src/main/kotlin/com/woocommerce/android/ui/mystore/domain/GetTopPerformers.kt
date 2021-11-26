package com.woocommerce.android.ui.mystore.domain

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.di.DefaultDispatcher
import com.woocommerce.android.ui.mystore.data.StatsRepository
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers.LoadTopPerformersResult.IsLoadingTopPerformers
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers.LoadTopPerformersResult.TopPerformersLoadedError
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers.LoadTopPerformersResult.TopPerformersLoadedSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.store.WCStatsStore
import javax.inject.Inject

class GetTopPerformers @Inject constructor(
    private val statsRepository: StatsRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        forceRefresh: Boolean,
        granularity: WCStatsStore.StatsGranularity,
        topPerformersCount: Int
    ): Flow<LoadTopPerformersResult> =
        statsRepository.fetchProductLeaderboards(forceRefresh, granularity, topPerformersCount)
            .transform { result ->
                emit(IsLoadingTopPerformers(false))
                result.fold(
                    onSuccess = { topPerformers ->
                        topPerformers
                            .sortedWith(
                                compareByDescending(WCTopPerformerProductModel::quantity)
                                    .thenByDescending(WCTopPerformerProductModel::total)
                            ).let {
                                AnalyticsTracker.track(
                                    AnalyticsTracker.Stat.DASHBOARD_TOP_PERFORMERS_LOADED,
                                    mapOf(AnalyticsTracker.KEY_RANGE to granularity.name.lowercase())
                                )
                                emit(TopPerformersLoadedSuccess(it))
                            }
                    },
                    onFailure = {
                        emit(TopPerformersLoadedError)
                    }
                )
            }.flowOn(dispatcher)

    sealed class LoadTopPerformersResult {
        data class TopPerformersLoadedSuccess(
            val topPerformers: List<WCTopPerformerProductModel>
        ) : LoadTopPerformersResult()

        data class IsLoadingTopPerformers(
            val isLoading: Boolean
        ) : LoadTopPerformersResult()

        object TopPerformersLoadedError : LoadTopPerformersResult()
    }
}
