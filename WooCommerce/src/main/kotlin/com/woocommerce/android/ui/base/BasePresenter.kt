package com.woocommerce.android.ui.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

interface BasePresenter<in T> {
    val coroutineScope: CoroutineScope
        get() = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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
