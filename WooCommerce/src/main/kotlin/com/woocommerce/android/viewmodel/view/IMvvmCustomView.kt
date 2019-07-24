package com.woocommerce.android.viewmodel.view

import androidx.lifecycle.LifecycleOwner

interface IMvvmCustomView<V: IMvvmViewState, T: IMvvmCustomViewModel<V>> {
    val viewModel: T

    fun onLifecycleOwnerAttached(lifecycleOwner: LifecycleOwner)
}
