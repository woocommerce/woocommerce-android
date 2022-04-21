package com.woocommerce.android.screenshots.orders

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class OrderSelectProductScreen : Screen {
    companion object {
        const val SELECT_PRODUCT_ROOT = R.id.selectProduct_root
        const val LIST_VIEW = R.id.products_list
        const val SIMPLE_PRODUCT_NAME = "Akoya Pearl shades"
    }

    constructor() : super(SELECT_PRODUCT_ROOT)

    fun assertOrderSelectProductScreen(): OrderSelectProductScreen {
        Espresso.onView(withId(R.id.collapsing_toolbar))
            .check(matches(hasDescendant(withText(R.string.order_creation_add_products))))
            .check(matches(isDisplayed()))
        return this
    }

    fun selectProduct(productName: String): OrderCreationScreen {
        scrollToListItem(productName, LIST_VIEW)
        selectListItem(productName, LIST_VIEW)
        return OrderCreationScreen()
    }
}
