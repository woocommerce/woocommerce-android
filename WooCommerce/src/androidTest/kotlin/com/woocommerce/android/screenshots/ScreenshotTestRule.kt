package com.woocommerce.android.screenshots

import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.ui.main.MainActivity

class ScreenshotTestRule : ActivityTestRule<MainActivity>(MainActivity::class.java, false, false) {
    fun launch() {
        // Start
        super.launchActivity(null)
    }
}
