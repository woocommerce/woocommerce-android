package com.woocommerce.android.util

import android.content.Context
import com.woocommerce.android.BuildConfig

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    PRODUCT_LIST,
    UPDATE_PRODUCT_DESC,
    DB_DOWNGRADE,
    REFUNDS;
    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            PRODUCT_LIST -> BuildConfig.DEBUG
            UPDATE_PRODUCT_DESC -> BuildConfig.DEBUG
            REFUNDS -> BuildConfig.DEBUG
            DB_DOWNGRADE -> {
                BuildConfig.DEBUG || context != null && PackageUtils.isBetaBuild(context)
            }
        }
    }
}
