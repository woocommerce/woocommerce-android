package com.woocommerce.android.ui.products

import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.variations.VariationRepository
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import javax.inject.Inject

class DuplicateProduct @Inject constructor(
    private val productDetailRepository: ProductDetailRepository,
    private val variationRepository: VariationRepository,
    private val resourceProvider: ResourceProvider,
) {

    suspend operator fun invoke(product: Product): Result<Long> {
        val newProduct = product.copy(
            remoteId = 0,
            name = resourceProvider.getString(R.string.product_duplicate_copied_product_name, product.name),
            sku = "",
            status = ProductStatus.DRAFT
        )

        val (duplicateProductSuccess, duplicatedProductRemoteId) = productDetailRepository.addProduct(newProduct)

        return if (duplicateProductSuccess) {
            if (product.numVariations > 0) {
                duplicateVariations(product, duplicatedProductRemoteId)
            } else {
                Result.success(duplicatedProductRemoteId)
            }
        } else {
            Result.failure(
                WooException(
                    WooError(
                        GENERIC_ERROR,
                        NETWORK_ERROR,
                        "Couldn't publish product $duplicatedProductRemoteId"
                    )
                )
            )
        }
    }

    @Suppress("MagicNumber")
    private suspend fun duplicateVariations(
        product: Product,
        duplicatedProductRemoteId: Long
    ): Result<Long> {
        var isLoadingMore = false
        val duplicatedVariations = buildList {
            do {
                val variations = variationRepository.fetchProductVariations(product.remoteId, isLoadingMore)
                    .map { it.copy(remoteProductId = duplicatedProductRemoteId, sku = "") }
                addAll(variations)
                isLoadingMore = true
            } while (variationRepository.canLoadMoreProductVariations)
        }.also {
            if (it.size > 100) {
                // The API doesn't allow to create more than 100 variations at once
                WooLog.w(
                    WooLog.T.PRODUCTS,
                    "Variations list is too big: ${it.size}, the duplicate will have only the first 100 variations"
                )
            }
        }.take(100)

        val variationsDuplicationResult = variationRepository.createVariations(
            duplicatedProductRemoteId,
            duplicatedVariations
        )

        return variationsDuplicationResult.fold(
            onSuccess = {
                Result.success(duplicatedProductRemoteId)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }
}
