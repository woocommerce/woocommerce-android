package com.woocommerce.android.wear.viewmodel

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

/**
 * A base class for WearOS ViewModels that use coroutines and enforces the data reload behavior for compose views.
 */
abstract class WearViewModel : ViewModel(), CoroutineScope, DefaultLifecycleObserver {
    override val coroutineContext: CoroutineContext
        get() = viewModelScope.coroutineContext

    abstract fun reloadData(withLoading: Boolean = true)

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        reloadData(withLoading = false)
    }
}
