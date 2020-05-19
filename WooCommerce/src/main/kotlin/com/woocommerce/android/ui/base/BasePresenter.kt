package com.woocommerce.android.ui.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

interface BasePresenter<in T> {
    val coroutineScope: CoroutineScope
        get() = CoroutineScope(Job())

    fun takeView(view: T)

    /**
     * This method would need to be called by the inherited classes if
     * [coroutineScope] is being actively used, in order to cancel the coroutine
     *
     * See [OrderDetailPresenter] for more details
     */
    fun dropView() {
        coroutineScope.cancel()
    }
}
