package com.woocommerce.android.e2e.screens.orders

import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen

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
