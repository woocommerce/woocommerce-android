package com.woocommerce.android.e2e.screens.woopos

class WooPosTotalsScreen {
    fun selectCashPayment(): WooPosTotalsScreen {
        Thread.sleep(2000)
        return this
    }

    fun completePayment(): WooPosPaymentSuccessScreen {
        Thread.sleep(3000)
        return WooPosPaymentSuccessScreen()
    }

    inline fun <reified T> thenTakeScreenshot(name: String): T {
        tools.fastlane.screengrab.Screengrab.screenshot(name)
        return this as T
    }
}
