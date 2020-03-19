package com.woocommerce.android.screenshots.orders

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.TabNavComponent
import com.woocommerce.android.screenshots.util.Screen

class OrderListScreen : Screen {
    companion object {
        const val LIST_VIEW = R.id.ordersList
        const val SEARCH_BUTTON = R.id.menu_search
    }

    val tabBar = TabNavComponent()

    constructor(): super(LIST_VIEW)

    fun selectOrder(index: Int): SingleOrderScreen {
        val correctedIndex = index + 1 // account for the header
        selectItemAtIndexInRecyclerView(correctedIndex, LIST_VIEW)
        return SingleOrderScreen()
    }

    fun openSearchPane(): OrderSearchScreen {
        clickOn(SEARCH_BUTTON)
        return OrderSearchScreen()
    }
}