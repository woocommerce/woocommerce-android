package com.woocommerce.android.e2e.screens.woopos

class WooPosHomeScreen {
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

    inline fun <reified T> thenTakeScreenshot(name: String): T {
        tools.fastlane.screengrab.Screengrab.screenshot(name)
        return this as T
    }
}
