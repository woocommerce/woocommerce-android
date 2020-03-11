package com.woocommerce.android.util

import android.content.Context
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.BuildConfig

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    PRODUCT_RELEASE_M1,
    DB_DOWNGRADE,
    PRODUCT_IMAGE_CHOOSER;
    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            PRODUCT_RELEASE_M1 -> AppPrefs.isProductsFeatureEnabled()
            PRODUCT_IMAGE_CHOOSER -> BuildConfig.DEBUG && PRODUCT_RELEASE_M1.isEnabled(context)
            DB_DOWNGRADE -> {
                BuildConfig.DEBUG || context != null && PackageUtils.isBetaBuild(context)
            }
        }
    }
}
