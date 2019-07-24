package com.woocommerce.android.viewmodel.view

interface IMvvmCustomViewModel<T: IMvvmViewState> {
    var state: T
}
