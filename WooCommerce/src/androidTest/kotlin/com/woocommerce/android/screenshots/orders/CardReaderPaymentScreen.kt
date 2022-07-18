package com.woocommerce.android.screenshots.orders

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class CardReaderPaymentScreen : Screen(HEADER) {
    companion object {
        private const val HEADER = R.id.header_label
    }

    fun goBackToPaymentSelection(): PaymentSelectionScreen {
        pressBack()
        return PaymentSelectionScreen()
    }
}
