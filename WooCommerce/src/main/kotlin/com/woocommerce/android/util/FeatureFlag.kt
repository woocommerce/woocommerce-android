package com.woocommerce.android.util

import android.content.Context

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    DB_DOWNGRADE,
    MORE_MENU_INBOX,
    WC_SHIPPING_BANNER,
    OTHER_PAYMENT_METHODS,
    BETTER_CUSTOMER_SEARCH_M2,
    ORDER_CREATION_AUTO_TAX_RATE,
    CUSTOM_RANGE_ANALYTICS,
    CONNECTIVITY_TOOL,
    NEW_SHIPPING_SUPPORT,
    DYNAMIC_DASHBOARD,
    APP_PASSWORD_TUTORIAL,
    EOSL_M1;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }

            OTHER_PAYMENT_METHODS,
            MORE_MENU_INBOX,
            WC_SHIPPING_BANNER,
            BETTER_CUSTOMER_SEARCH_M2,
            ORDER_CREATION_AUTO_TAX_RATE,
            EOSL_M1 -> PackageUtils.isDebugBuild()

            DYNAMIC_DASHBOARD -> false

            CONNECTIVITY_TOOL,
            CUSTOM_RANGE_ANALYTICS,
            NEW_SHIPPING_SUPPORT,
            APP_PASSWORD_TUTORIAL -> true
        }
    }
}
