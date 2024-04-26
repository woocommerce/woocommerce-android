package com.woocommerce.android.ui.orders.creation.configuration

import com.woocommerce.android.ui.products.GetBundledProducts
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetChildrenProductInfo @Inject constructor(
    private val productDetailRepository: ProductDetailRepository,
    private val getBundledProducts: GetBundledProducts
) {
    operator fun invoke(productId: Long): Flow<Map<Long, ProductInfo>> {
        return productDetailRepository.getProduct(productId)
            ?.takeIf { it.productType == ProductType.BUNDLE }
            ?.let { getBundledProducts(productId) }
            ?.map { products ->
                products.map { bundledProduct ->
                    ProductInfo(
                        id = bundledProduct.id,
                        productId = bundledProduct.bundledProductId,
                        title = bundledProduct.title,
                        imageUrl = bundledProduct.imageUrl
                    )
                }.associateBy { it.id }
            } ?: flowOf(emptyMap())
    }
}
