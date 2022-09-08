package com.woocommerce.android.ui.mystore.domain

import com.woocommerce.android.ui.mystore.data.StatsRepository
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity
import org.wordpress.android.fluxc.store.WCStatsStore
import javax.inject.Inject

class GetTopPerformers @Inject constructor(
    private val statsRepository: StatsRepository,
    private val coroutineDispatchers: CoroutineDispatchers,
) {

    fun observeTopPerformers(granularity: WCStatsStore.StatsGranularity): Flow<List<TopPerformerProduct>> =
        statsRepository.observeTopPerformers(granularity)
            .map { topPerformersProductEntities ->
                topPerformersProductEntities
                    .map { it.toTopPerformerProduct() }
                    .sortDescByQuantityAndTotal()
            }.flowOn(coroutineDispatchers.computation)

//    suspend operator fun invoke(
//        granularity: WCStatsStore.StatsGranularity,
//        topPerformersCount: Int,
//        forceRefresh: Boolean = false,
//    ): Flow<TopPerformersResult> =
//        statsRepository.fetchProductLeaderboards(forceRefresh, granularity, topPerformersCount)
//            .transform { result ->
//                result.fold(
//                    onSuccess = { topPerformers ->
//                        val sortedTopPerformers = sortTopPerformers(topPerformers)
//                        emit(TopPerformersResult.TopPerformersSuccess(sortedTopPerformers))
//                    },
//                    onFailure = {
//                        emit(TopPerformersError)
//                    }
//                )
//            }.flowOn(coroutineDispatchers.computation)

    suspend operator fun invoke(
        granularity: WCStatsStore.StatsGranularity,
        topPerformersCount: Int,
        forceRefresh: Boolean = false,
    ): Result<Unit> = statsRepository.fetchProductLeaderboardsNew(forceRefresh, granularity, topPerformersCount)

//    private fun sortTopPerformers(topPerformers: List<WCTopPerformerProductModel>) =
//        topPerformers.sortedWith(
//            compareByDescending(WCTopPerformerProductModel::quantity)
//                .thenByDescending(WCTopPerformerProductModel::total)
//        )

    private fun List<TopPerformerProduct>.sortDescByQuantityAndTotal() =
        sortedWith(
            compareByDescending(TopPerformerProduct::quantity)
                .thenByDescending(TopPerformerProduct::total)
        )

    private fun TopPerformerProductEntity.toTopPerformerProduct() =
        TopPerformerProduct(
            productId = productId,
            name = name,
            quantity = quantity,
            currency = currency,
            total = total,
            imageUrl = imageUrl
        )

    sealed class TopPerformersResult {
        data class TopPerformersSuccess(
            val topPerformers: List<WCTopPerformerProductModel>
        ) : TopPerformersResult()

        object TopPerformersError : TopPerformersResult()
    }

    data class TopPerformerProduct(
        val productId: Long,
        val name: String,
        val quantity: Int,
        val currency: String,
        val total: Double,
        val imageUrl: String?
    )
}
