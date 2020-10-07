package com.woocommerce.android.util

import android.content.Context
import com.woocommerce.android.BuildConfig

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    PRODUCT_RELEASE_M4,
    PRODUCT_RELEASE_M5,
    SHIPPING_LABELS_M1,
    DB_DOWNGRADE;
    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            SHIPPING_LABELS_M1 -> BuildConfig.DEBUG || isTesting()
            PRODUCT_RELEASE_M4 -> BuildConfig.DEBUG || isTesting()
            PRODUCT_RELEASE_M5 -> BuildConfig.DEBUG || isTesting()
            DB_DOWNGRADE -> {
                BuildConfig.DEBUG || context != null && PackageUtils.isBetaBuild(context)
            }
        }
    }

    private fun isTesting(): Boolean {
        return try {
            Class.forName("com.woocommerce.android.viewmodel.BaseUnitTest")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}
