package com.woocommerce.android.screenshots.orders

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class OrderSearchScreen : Screen {
    companion object {
        const val LIST_VIEW = R.id.ordersList
        const val LIST_ITEM = R.id.divider
        const val SEARCH_TEXT_FIELD = R.id.search_src_text
        const val SEARCH_CLOSE_BTN = R.id.search_close_btn
    }

    constructor() : super(SEARCH_TEXT_FIELD)

    // TASKS
    fun selectRandomOrderFromTheSearchResult(): SingleOrderScreen {
        selectOrder(0)
        return SingleOrderScreen()
    }

    fun dismissSearchResults(): OrderSearchScreen {
        clickOn(SEARCH_CLOSE_BTN)
        return OrderSearchScreen()
    }

    fun cancelSearch(): OrderListScreen {
        waitForElementToBeDisplayed(SEARCH_TEXT_FIELD)
        pressBack()
        waitForElementToBeDisplayed(SEARCH_TEXT_FIELD)
        pressBack()
        return OrderListScreen()
    }

    // HELPERS

    private fun selectOrder(index: Int): SingleOrderScreen {
        val randomInteger = (1..5).shuffled().first()
        val correctedIndex = index + randomInteger // account for the header
        waitForElementToBeDisplayedWithoutFailure(LIST_ITEM)
        selectItemAtIndexInRecyclerView(correctedIndex, LIST_VIEW, LIST_ITEM)
        return SingleOrderScreen()
    }
}
