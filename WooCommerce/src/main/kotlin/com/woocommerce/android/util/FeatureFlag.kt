package com.woocommerce.android.util

import android.content.Context
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.BuildConfig

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    PRODUCT_RELEASE_M4,
    SHIPPING_LABELS_M1,
    DB_DOWNGRADE;
    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            // This feature will live switched on from the
            // setting screen. i.e. check AppPrefs.isProductsFeatureEnabled() method
            // Also, turn on the feature during testing
            SHIPPING_LABELS_M1 -> BuildConfig.DEBUG || isTesting()
            PRODUCT_RELEASE_M4 -> BuildConfig.DEBUG && AppPrefs.isProductsFeatureEnabled() || isTesting()
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
