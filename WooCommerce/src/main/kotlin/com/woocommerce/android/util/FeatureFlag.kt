package com.woocommerce.android.util

import android.content.Context
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.BuildConfig

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    PRODUCT_RELEASE_TEASER,
    ADD_EDIT_PRODUCT_RELEASE_1,
    DB_DOWNGRADE,
    PRODUCT_IMAGE_CHOOSER;
    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            ADD_EDIT_PRODUCT_RELEASE_1 -> BuildConfig.DEBUG
            PRODUCT_RELEASE_TEASER -> AppPrefs.isProductsFeatureEnabled()
            PRODUCT_IMAGE_CHOOSER -> BuildConfig.DEBUG && AppPrefs.isProductsFeatureEnabled()
            DB_DOWNGRADE -> {
                BuildConfig.DEBUG || context != null && PackageUtils.isBetaBuild(context)
            }
        }
    }
}
