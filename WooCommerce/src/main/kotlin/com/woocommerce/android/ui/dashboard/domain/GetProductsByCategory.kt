package com.woocommerce.android.ui.dashboard.domain

import com.woocommerce.android.ui.dashboard.data.StatsRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.commons.stats.StatsTimeRange
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetProductsByCategory @Inject constructor(
    private val statsRepository: StatsRepository,
    private val coroutineDispatchers: CoroutineDispatchers
) {
    private companion object {
        const val NUM_PRODUCTS = 20
    }

    suspend operator fun invoke(
        range: StatsTimeRange,
        categoryId: Long,
        quantity: Int = NUM_PRODUCTS
    ): Result<List<GetTopPerformers.TopPerformerProduct>> = withContext(coroutineDispatchers.io) {
        statsRepository.fetchTopPerformerProductsByCategory(
            range = range,
            categoryId = categoryId,
            quantity = quantity
        ).map { entities ->
            entities.map { entity ->
                GetTopPerformers.TopPerformerProduct(
                    productId = entity.productId.value,
                    name = entity.name,
                    quantity = entity.quantity,
                    currency = entity.currency,
                    total = entity.total,
                    imageUrl = entity.imageUrl
                )
            }
        }
    }
}
