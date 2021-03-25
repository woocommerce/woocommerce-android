package com.woocommerce.android.util

import android.content.Context

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    SHIPPING_LABELS_M2,
    ADD_EDIT_VARIATIONS,
    DB_DOWNGRADE,
    ORDER_CREATION;
    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            SHIPPING_LABELS_M2 -> PackageUtils.isDebugBuild() || PackageUtils.isTesting()
            ADD_EDIT_VARIATIONS -> PackageUtils.isDebugBuild()
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }
            ORDER_CREATION -> PackageUtils.isDebugBuild() || PackageUtils.isTesting()
        }
    }
}
