package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Component
import com.woocommerce.android.model.QueryType
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetComponentProducts @Inject constructor(
    private val dispatchers: CoroutineDispatchers
) {
    suspend operator fun invoke(productId: Long): List<Component> {
        println(productId)
        return withContext(dispatchers.io) {
            delay(500)
            List(6) { n ->
                Component(
                    id = n.toLong(),
                    title = "Component $n",
                    description = "This component $n is very helpful",
                    queryType = if (n % 2 == 0) QueryType.PRODUCT else QueryType.CATEGORY,
                    queryIds = (0..n + 1).toList().map { it * 1L },
                    defaultOption = n.toLong(),
                    thumbnailUrl = null
                )
            }
        }
    }
}
