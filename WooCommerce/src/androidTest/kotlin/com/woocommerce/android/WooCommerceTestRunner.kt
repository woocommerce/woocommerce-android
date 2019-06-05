package com.woocommerce.android

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Custom AndroidJUnitRunner that replaces the original application with [WooCommerceTest].
 */
class WooCommerceTestRunner : AndroidJUnitRunner() {
    override fun newApplication(classLoader: ClassLoader, className: String, context: Context): Application {
        return super.newApplication(classLoader, WooCommerceTest::class.java.name, context)
    }
}
