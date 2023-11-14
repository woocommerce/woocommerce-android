package com.woocommerce.android.ui.login.storecreation

sealed interface StoreCreationResult<T> {
    data class Success<T>(val data: T) : StoreCreationResult<T>
    data class Failure<T>(val type: StoreCreationErrorType, val message: String? = null) : StoreCreationResult<T>
}
