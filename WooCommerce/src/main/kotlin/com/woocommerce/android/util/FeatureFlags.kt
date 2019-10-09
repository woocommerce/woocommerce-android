package com.woocommerce.android.util

import com.woocommerce.android.BuildConfig

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlags {
    PRODUCT_LIST,
    DB_DOWNGRADE,
    REFUNDS;

    // currently all feature flags are only enabled in debug builds
    fun isEnabled() = BuildConfig.DEBUG
}
