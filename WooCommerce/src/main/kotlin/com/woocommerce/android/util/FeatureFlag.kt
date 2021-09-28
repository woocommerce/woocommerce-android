package com.woocommerce.android.util

import android.content.Context

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    DB_DOWNGRADE,
    ORDER_CREATION,
    CARD_READER,
    WHATS_NEW;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }
            ORDER_CREATION, WHATS_NEW -> PackageUtils.isDebugBuild() || PackageUtils.isTesting()
            CARD_READER -> true
        }
    }
}
