package com.woocommerce.android

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Custom AndroidJUnitRunner that replaces the original application with [WooCommerceTest].
 */
class WooCommerceTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        return super.newApplication(cl, WooCommerceTest_Application::class.java.name, context)
    }
}
