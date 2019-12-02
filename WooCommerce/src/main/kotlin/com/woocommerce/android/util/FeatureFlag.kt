package com.woocommerce.android.util

import android.content.Context
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.BuildConfig

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    PRODUCT_RELEASE_TEASER,
    DB_DOWNGRADE,
    REFUNDS;
    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            PRODUCT_RELEASE_TEASER -> AppPrefs.isProductsFeatureEnabled()
            REFUNDS -> BuildConfig.DEBUG
            DB_DOWNGRADE -> {
                BuildConfig.DEBUG || context != null && PackageUtils.isBetaBuild(context)
            }
        }
    }
}
