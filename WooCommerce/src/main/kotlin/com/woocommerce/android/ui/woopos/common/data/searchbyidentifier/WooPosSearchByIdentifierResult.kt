package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation

sealed class WooPosSearchByIdentifierResult {
    data class Success(val product: Product) : WooPosSearchByIdentifierResult()
    data class VariationSuccess(val variation: ProductVariation) : WooPosSearchByIdentifierResult()
    data class Failure(val error: Error) : WooPosSearchByIdentifierResult()

    sealed class Error {
        data object ProductNotFound : Error()
        data class UnsupportedProduct(val productName: String) : Error()
        data object NetworkError : Error()
        data object RequestCancelled : Error()
        data class UnknownError(val message: String) : Error()
    }

    val isSuccess: Boolean
        get() = this is Success || this is VariationSuccess

    val isFailure: Boolean
        get() = this is Failure
}
