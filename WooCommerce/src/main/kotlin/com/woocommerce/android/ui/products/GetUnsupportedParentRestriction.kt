package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import javax.inject.Inject

class GetUnsupportedParentRestriction @Inject constructor(
    private val productDetailRepository: ProductDetailRepository,
    private val productRestrictions: OrderCreationProductRestrictions
) {
    suspend operator fun invoke(variation: Product): Result {
        val parent = productDetailRepository.getProduct(variation.parentId)
            ?: productDetailRepository.fetchAndGetProduct(variation.parentId)
            ?: return Result.ParentUnavailable
        return when (val restriction = productRestrictions.getUnsupportedRestriction(parent)) {
            null -> Result.NotRestricted
            else -> Result.Restricted(restriction)
        }
    }

    sealed interface Result {
        data object NotRestricted : Result
        data class Restricted(val restriction: ProductRestriction.Unsupported) : Result
        data object ParentUnavailable : Result
    }
}
