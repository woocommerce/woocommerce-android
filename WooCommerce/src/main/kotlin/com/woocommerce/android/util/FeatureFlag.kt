package com.woocommerce.android.util

import android.content.Context

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    DB_DOWNGRADE,
    MORE_MENU_INBOX,
    WC_SHIPPING_BANNER,
    MIGRATION_SIMPLE_PAYMENTS,
    BETTER_CUSTOMER_SEARCH_M2,
    ORDER_CREATION_AUTO_TAX_RATE,
    BLAZE_I3,
    CUSTOM_RANGE_ANALYTICS,
    CONNECTIVITY_TOOL,
    NEW_SHIPPING_SUPPORT,
    DYNAMIC_DASHBOARD;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }

            MIGRATION_SIMPLE_PAYMENTS,
            MORE_MENU_INBOX,
            WC_SHIPPING_BANNER,
            BETTER_CUSTOMER_SEARCH_M2,
            ORDER_CREATION_AUTO_TAX_RATE -> PackageUtils.isDebugBuild()

            DYNAMIC_DASHBOARD -> false

            CONNECTIVITY_TOOL,
            BLAZE_I3,
            CUSTOM_RANGE_ANALYTICS,
            NEW_SHIPPING_SUPPORT -> true
        }
    }
}
