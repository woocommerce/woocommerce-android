package com.woocommerce.android.ui.base


interface ActionErrorResolution<T> {
    fun handleActionError(action: T)
}