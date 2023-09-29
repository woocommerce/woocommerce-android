package com.woocommerce.android.ui.orders.creation.configuration

import com.woocommerce.android.ui.products.GetBundledProducts
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.ProductType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetChildrenProductInfo @Inject constructor(
    private val productDetailRepository: ProductDetailRepository,
    private val getBundledProducts: GetBundledProducts
) {
    operator fun invoke(productId: Long): Flow<Map<Long, ProductInfo>> {
        val product = productDetailRepository.getProduct(productId) ?: return flowOf(emptyMap())
        return when (product.productType) {
            ProductType.BUNDLE -> {
                getBundledProducts(productId).map { list ->
                    val result = mutableMapOf<Long, ProductInfo>()
                    list.onEach { bundledProduct ->
                        result[bundledProduct.id] = ProductInfo(
                            id = bundledProduct.id,
                            title = bundledProduct.title,
                            imageUrl = bundledProduct.imageUrl
                        )
                    }
                    result
                }
            }
            else -> flowOf(emptyMap())
        }
    }
}
