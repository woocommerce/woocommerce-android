package com.woocommerce.android.e2e.screens.woopos

import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen

class WooPosTotalsScreen : Screen(R.id.point_of_sale) {
    fun selectCashPayment(): WooPosTotalsScreen {
        Thread.sleep(2000)
        return this
    }

    fun completePayment(): WooPosPaymentSuccessScreen {
        Thread.sleep(3000)
        return WooPosPaymentSuccessScreen()
    }
}
