package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Product

sealed class WooPosSearchByIdentifierResult {
    data class Success(val product: Product) : WooPosSearchByIdentifierResult()
    data class Failure(val error: Error) : WooPosSearchByIdentifierResult()

    sealed class Error {
        object ProductNotFound : Error()
        object NetworkError : Error()
        object RequestCancelled : Error()
        data class UnknownError(val message: String) : Error()
    }

    val isSuccess: Boolean
        get() = this is Success

    val isFailure: Boolean
        get() = this is Failure
}
