package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.ProductError
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.DUPLICATE_SKU
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.INVALID_IMAGE_ID
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.INVALID_MIN_MAX_QUANTITY
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.INVALID_PARAM
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.INVALID_PRODUCT_ID
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.INVALID_REVIEW_ID
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.INVALID_VARIATION_IMAGE_ID
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.PARSE_ERROR
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.TERM_EXISTS
import javax.inject.Inject

class WooPosSearchByIdentifierProductGetOrFetch @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
    private val productsCache: WooPosProductsCache
) {
    suspend operator fun invoke(productId: Long): WooPosSearchByIdentifierResult {
        productsCache.getProductById(productId)?.let { cachedProduct ->
            return WooPosSearchByIdentifierResult.Success(cachedProduct)
        }

        val result = productStore.fetchSingleProduct(
            WCProductStore.FetchSingleProductPayload(
                site = selectedSite.get(),
                remoteProductId = productId,
            )
        )

        return when {
            result.isError -> WooPosSearchByIdentifierResult.Failure(result.error.mapError())

            else -> {
                val product = productStore.getProduct(selectedSite.get(), productId)?.toAppModel()
                if (product != null) {
                    productsCache.addAll(listOf(product))
                    WooPosSearchByIdentifierResult.Success(product)
                } else {
                    WooPosSearchByIdentifierResult.Failure(
                        WooPosSearchByIdentifierResult.Error.UnknownError(
                            "Product not found for ID: $productId"
                        )
                    )
                }
            }
        }
    }

    fun ProductError.mapError(): WooPosSearchByIdentifierResult.Error =
        when (type) {
            INVALID_PRODUCT_ID -> WooPosSearchByIdentifierResult.Error.NotFound

            INVALID_PARAM -> WooPosSearchByIdentifierResult.Error.ServerError(
                message.ifEmpty { "Invalid parameter" }
            )

            INVALID_REVIEW_ID -> WooPosSearchByIdentifierResult.Error.NotFound

            INVALID_IMAGE_ID -> WooPosSearchByIdentifierResult.Error.NotFound

            DUPLICATE_SKU -> WooPosSearchByIdentifierResult.Error.ServerError(
                message.ifEmpty { "Duplicate SKU" }
            )

            TERM_EXISTS -> WooPosSearchByIdentifierResult.Error.ServerError(
                message.ifEmpty { "Term already exists" }
            )

            INVALID_VARIATION_IMAGE_ID -> WooPosSearchByIdentifierResult.Error.ServerError(
                message.ifEmpty { "Invalid variation image ID" }
            )

            INVALID_MIN_MAX_QUANTITY -> WooPosSearchByIdentifierResult.Error.ServerError(
                message.ifEmpty { "Invalid min/max quantity" }
            )

            PARSE_ERROR -> WooPosSearchByIdentifierResult.Error.ServerError(
                message.ifEmpty { "Parse error occurred" }
            )

            GENERIC_ERROR -> WooPosSearchByIdentifierResult.Error.UnknownError(
                message.ifEmpty { "Generic error occurred" }
            )
        }
}
