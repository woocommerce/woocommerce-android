package com.woocommerce.android.screenshots.orders

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class OrderSearchScreen : Screen {
    companion object {
        const val SEARCH_FIELD = R.id.search_bar
    }

    constructor(): super(SEARCH_FIELD)

    fun cancel(): OrderListScreen {
        pressBack()
        pressBack()
        return OrderListScreen()
    }
}
