package com.woocommerce.android.ui.base

import androidx.annotation.CallSuper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

interface BasePresenter<in T> {
    val coroutineScope: CoroutineScope
        get() = CoroutineScope(Job())

    fun takeView(view: T)

    @CallSuper
    fun dropView() {
        coroutineScope.cancel()
    }
}
