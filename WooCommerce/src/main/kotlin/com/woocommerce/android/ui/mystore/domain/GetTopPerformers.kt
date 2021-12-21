package com.woocommerce.android.ui.mystore.domain

import com.woocommerce.android.ui.mystore.data.StatsRepository
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers.LoadTopPerformersResult.TopPerformersLoadedError
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers.LoadTopPerformersResult.TopPerformersLoadedSuccess
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.store.WCStatsStore
import javax.inject.Inject

class GetTopPerformers @Inject constructor(
    private val statsRepository: StatsRepository,
    private val coroutineDispatchers: CoroutineDispatchers,
) {
    suspend operator fun invoke(
        forceRefresh: Boolean,
        granularity: WCStatsStore.StatsGranularity,
        topPerformersCount: Int
    ): Flow<LoadTopPerformersResult> =
        statsRepository.fetchProductLeaderboards(forceRefresh, granularity, topPerformersCount)
            .transform { result ->
                result.fold(
                    onSuccess = { topPerformers ->
                        val sortedTopPerformers = sortTopPerformers(topPerformers)
                        emit(TopPerformersLoadedSuccess(sortedTopPerformers))
                    },
                    onFailure = {
                        emit(TopPerformersLoadedError)
                    }
                )
            }.flowOn(coroutineDispatchers.computation)

    private fun sortTopPerformers(topPerformers: List<WCTopPerformerProductModel>) =
        topPerformers.sortedWith(
            compareByDescending(WCTopPerformerProductModel::quantity)
                .thenByDescending(WCTopPerformerProductModel::total)
        )

    sealed class LoadTopPerformersResult {
        data class TopPerformersLoadedSuccess(
            val topPerformers: List<WCTopPerformerProductModel>
        ) : LoadTopPerformersResult()

        object TopPerformersLoadedError : LoadTopPerformersResult()
    }
}
