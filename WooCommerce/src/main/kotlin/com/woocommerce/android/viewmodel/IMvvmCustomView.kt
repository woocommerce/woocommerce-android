package com.woocommerce.android.viewmodel

import androidx.lifecycle.LifecycleOwner

interface IMvvmCustomView<V: IMvvmViewState, T: IMvvmCustomViewModel<V>> {
    val viewModel: T

    fun onLifecycleOwnerAttached(lifecycleOwner: LifecycleOwner)
}
