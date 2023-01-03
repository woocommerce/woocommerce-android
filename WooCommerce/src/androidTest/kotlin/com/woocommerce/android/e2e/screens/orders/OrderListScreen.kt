package com.woocommerce.android.e2e.screens.orders

import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen

class OrderListScreen : Screen {
    companion object {
        const val LIST_VIEW = R.id.ordersList
        const val LIST_ITEM = R.id.linearLayout
        const val SEARCH_BUTTON = R.id.menu_search
        const val CREATE_ORDER_BUTTON = R.id.createOrderButton
    }

    constructor() : super(LIST_VIEW)

    fun selectOrder(index: Int): SingleOrderScreen {
        val correctedIndex = index + 1 // account for the header
        selectItemAtIndexInRecyclerView(correctedIndex, LIST_VIEW, LIST_ITEM)
        return SingleOrderScreen()
    }

    fun openSearchPane(): OrderSearchScreen {
        clickOn(SEARCH_BUTTON)
        return OrderSearchScreen()
    }

    fun createFABTap(): UnifiedOrderScreen {
        clickOn(CREATE_ORDER_BUTTON)
        return UnifiedOrderScreen()
    }
}
