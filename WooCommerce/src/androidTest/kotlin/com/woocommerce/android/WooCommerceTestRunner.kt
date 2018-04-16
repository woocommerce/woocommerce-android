package com.woocommerce.android

import android.app.Application
import android.support.test.runner.AndroidJUnitRunner

import android.content.Context

/**
 * Custom AndroidJUnitRunner that replaces the original application with [WooCommerceTest].
 */
class WooCommerceTestRunner : AndroidJUnitRunner() {
    override fun newApplication(classLoader: ClassLoader, className: String, context: Context): Application {
        return super.newApplication(classLoader, WooCommerceTest::class.java.name, context)
    }
}
