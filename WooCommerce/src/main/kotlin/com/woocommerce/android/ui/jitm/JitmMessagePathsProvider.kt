package com.woocommerce.android.ui.jitm

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JitmMessagePathsProvider @Inject constructor() {
    val paths: List<String> get() = privatePaths
    private val privatePaths: MutableList<String> = mutableListOf()

    init {
        privatePaths.add(MY_STORE)
        privatePaths.add(ORDER_LIST)
    }

    companion object {
        const val MY_STORE = "woomobile:my_store:admin_notices"
        const val ORDER_LIST = "woomobile:order_list:admin_notices"
    }
}
