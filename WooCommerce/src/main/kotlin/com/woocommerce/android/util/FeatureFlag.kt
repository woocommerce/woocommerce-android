package com.woocommerce.android.util

import android.content.Context
import com.woocommerce.android.BuildConfig

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    PRODUCT_RELEASE_M3,
    APP_FEEDBACK,
    DB_DOWNGRADE;
    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            // This feature will live switched on from the
            // setting screen. i.e. check AppPrefs.isProductsFeatureEnabled() method
            PRODUCT_RELEASE_M3, APP_FEEDBACK -> BuildConfig.DEBUG
            DB_DOWNGRADE -> {
                BuildConfig.DEBUG || context != null && PackageUtils.isBetaBuild(context)
            }
        }
    }
}
