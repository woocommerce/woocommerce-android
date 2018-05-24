package com.woocommerce.android.ui.base


interface ErrorResolution<T> {
    fun handleActionError(action: T, msg: String?)
}