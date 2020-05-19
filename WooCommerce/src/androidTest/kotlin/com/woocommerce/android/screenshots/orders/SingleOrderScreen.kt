package com.woocommerce.android.screenshots.orders

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class SingleOrderScreen : Screen {
    companion object {
        const val ORDER_NUMBER_LABEL = R.id.orderStatus_dateAndOrderNum
    }

    constructor(): super(ORDER_NUMBER_LABEL)

    fun goBackToOrdersScreen(): OrderListScreen {
        pressBack()
        return OrderListScreen()
    }
}
