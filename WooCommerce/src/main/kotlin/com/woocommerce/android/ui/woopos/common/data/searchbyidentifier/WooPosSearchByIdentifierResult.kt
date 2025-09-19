package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.ui.woopos.common.data.WooPosVariation
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel

sealed class WooPosSearchByIdentifierResult {
    data class Success(val product: WooPosProductModel) : WooPosSearchByIdentifierResult()
    data class VariationSuccess(val variation: WooPosVariation, val parentProduct: WooPosProductModel) :
        WooPosSearchByIdentifierResult()

    data class Failure(val error: Error) : WooPosSearchByIdentifierResult()

    sealed class Error {
        data object NotFound : Error()
        data class UnsupportedProduct(val productName: String) : Error()
        data object NetworkError : Error()
        data class ServerError(val message: String) : Error()
        data class UnknownError(val message: String) : Error()
    }

    val isSuccess: Boolean
        get() = this is Success || this is VariationSuccess
}
