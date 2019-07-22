package com.woocommerce.android.viewmodel

interface IMvvmCustomViewModel<T: IMvvmViewState> {
    var state: T?
}
