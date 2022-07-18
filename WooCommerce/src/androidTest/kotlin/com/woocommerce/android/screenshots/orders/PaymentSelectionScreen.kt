package com.woocommerce.android.screenshots.orders

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class PaymentSelectionScreen : Screen {
    companion object {
        const val CARD_BUTTON = R.id.textCard
    }

    constructor() : super(CARD_BUTTON)

    fun chooseCardPayment(): CardReaderPaymentScreen {
        clickOn(CARD_BUTTON)
        return CardReaderPaymentScreen()
    }

    fun goBackToOrderDetails(): SingleOrderScreen {
        pressBack()
        return SingleOrderScreen()
    }
}
