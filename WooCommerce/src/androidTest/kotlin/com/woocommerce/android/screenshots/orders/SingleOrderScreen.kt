package com.woocommerce.android.screenshots.orders

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class SingleOrderScreen : Screen {
    companion object {
        const val ORDER_NUMBER_LABEL = R.id.orderStatus_dateAndOrderNum
    }

    constructor() : super(ORDER_NUMBER_LABEL)

    // Navigation
    fun scrollToOrderDetails(): SingleOrderScreen {
        waitForElementToBeDisplayedWithoutFailure(R.id.customerInfo_label)
        scrollTo(R.id.customerInfo_label)
        scrollTo(R.id.orderDetail_orderStatus)
        return SingleOrderScreen()
    }

    fun goBackToOrdersScreen(): OrderListScreen {
        pressBack()
        return OrderListScreen()
    }

    fun goBackToSearch(): OrderSearchScreen {
        pressBack()
        return OrderSearchScreen()
    }

    fun checkBillingInfo(): SingleOrderScreen {
        waitForElementToBeDisplayedWithoutFailure(R.id.customerInfo_viewMoreButtonTitle)
        clickOn(R.id.customerInfo_viewMoreButtonTitle)
        waitForElementToBeDisplayedWithoutFailure(R.id.customerInfo_phone)
        clickOn(R.id.customerInfo_viewMoreButtonTitle)
        return SingleOrderScreen()
    }
}
