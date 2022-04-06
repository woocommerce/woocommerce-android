package com.woocommerce.android.screenshots.orders

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class OrderCreationScreen : Screen {
    companion object {
        const val ORDER_CREATION = R.id.order_creation_root
        const val CREATE_BUTTON = R.id.menu_create
    }

    constructor() : super(ORDER_CREATION)

    fun createOrder(): SingleOrderScreen {
        clickOn(CREATE_BUTTON)
        return SingleOrderScreen()
    }
}
