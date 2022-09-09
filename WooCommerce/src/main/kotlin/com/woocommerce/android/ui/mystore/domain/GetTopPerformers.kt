package com.woocommerce.android.ui.mystore.domain

import com.woocommerce.android.ui.mystore.data.StatsRepository
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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

    suspend operator fun invoke(
        granularity: WCStatsStore.StatsGranularity,
        topPerformersCount: Int,
        forceRefresh: Boolean = false,
    ): Result<Unit> = statsRepository.fetchTopPerformerProducts(forceRefresh, granularity, topPerformersCount)

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

    data class TopPerformerProduct(
        val productId: Long,
        val name: String,
        val quantity: Int,
        val currency: String,
        val total: Double,
        val imageUrl: String?
    )
}
