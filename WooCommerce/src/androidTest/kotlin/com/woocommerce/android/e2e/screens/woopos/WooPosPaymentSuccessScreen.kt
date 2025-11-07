package com.woocommerce.android.e2e.screens.woopos

class WooPosPaymentSuccessScreen {
    inline fun <reified T> thenTakeScreenshot(name: String): T {
        tools.fastlane.screengrab.Screengrab.screenshot(name)
        return this as T
    }
}
