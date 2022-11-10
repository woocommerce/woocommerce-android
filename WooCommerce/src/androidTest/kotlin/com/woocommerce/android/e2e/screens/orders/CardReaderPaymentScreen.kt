package com.woocommerce.android.e2e.screens.orders

import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen

class CardReaderPaymentScreen : Screen(HEADER) {
    companion object {
        private const val HEADER = R.id.header_label
    }

    fun goBackToPaymentSelection(): PaymentSelectionScreen {
        pressBack()
        return PaymentSelectionScreen()
    }
}
