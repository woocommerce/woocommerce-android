package com.woocommerce.android.e2e.screens.woopos

import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen

class WooPosHomeScreen : Screen(R.id.point_of_sale) {
    fun waitForLoad(): WooPosHomeScreen {
        Thread.sleep(5000)
        return this
    }

    fun addProductsToCart(): WooPosHomeScreen {
        Thread.sleep(2000)
        return this
    }

    fun proceedToCheckout(): WooPosTotalsScreen {
        Thread.sleep(1000)
        return WooPosTotalsScreen()
    }
}
