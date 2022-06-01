package com.woocommerce.android.util

import android.content.Context

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    DB_DOWNGRADE,
    JETPACK_CP,
    ANALYTICS_HUB,
    IN_PERSON_PAYMENTS_CANADA, // Keeping the flag for a few sprints so we can quickly disable the feature if needed
    MORE_MENU_INBOX,
    COUPONS_M2,
    IPP_SELECT_PAYMENT_GATEWAY,
    WC_SHIPPING_BANNER,
    UNIFIED_ORDER_EDITING;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }
            JETPACK_CP,
            IN_PERSON_PAYMENTS_CANADA -> true
            ANALYTICS_HUB,
            MORE_MENU_INBOX,
            COUPONS_M2,
            WC_SHIPPING_BANNER,
            IPP_SELECT_PAYMENT_GATEWAY,
            UNIFIED_ORDER_EDITING -> PackageUtils.isDebugBuild()
        }
    }
}
