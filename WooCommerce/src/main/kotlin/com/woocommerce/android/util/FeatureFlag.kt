package com.woocommerce.android.util

import android.content.Context

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    DB_DOWNGRADE,
    JETPACK_CP,
    ANALYTICS_HUB,
    MORE_MENU_INBOX,
    COUPONS_M2,
    WC_SHIPPING_BANNER,
    UNIFIED_ORDER_EDITING,
    ORDER_CREATION_CUSTOMER_SEARCH;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }
            COUPONS_M2,
            JETPACK_CP,
            ORDER_CREATION_CUSTOMER_SEARCH,
            UNIFIED_ORDER_EDITING -> true
            ANALYTICS_HUB,
            MORE_MENU_INBOX,
            WC_SHIPPING_BANNER -> PackageUtils.isDebugBuild()
        }
    }
}
