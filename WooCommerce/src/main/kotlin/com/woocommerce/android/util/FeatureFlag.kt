package com.woocommerce.android.util

import android.content.Context

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    WOO_POS,
    DB_DOWNGRADE,
    MORE_MENU_INBOX,
    WC_SHIPPING_BANNER,
    OTHER_PAYMENT_METHODS,
    BETTER_CUSTOMER_SEARCH_M2,
    ORDER_CREATION_AUTO_TAX_RATE,
    CONNECTIVITY_TOOL,
    NEW_SHIPPING_SUPPORT,
    APP_PASSWORD_TUTORIAL,
    EOSL_M1,
    EOSL_M3,
    DYNAMIC_DASHBOARD_M2;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }

            MORE_MENU_INBOX,
            WOO_POS,
            WC_SHIPPING_BANNER,
            BETTER_CUSTOMER_SEARCH_M2,
            ORDER_CREATION_AUTO_TAX_RATE,
            DYNAMIC_DASHBOARD_M2,
            EOSL_M3 -> PackageUtils.isDebugBuild()

            OTHER_PAYMENT_METHODS,
            CONNECTIVITY_TOOL,
            NEW_SHIPPING_SUPPORT,
            APP_PASSWORD_TUTORIAL,
            EOSL_M1 -> true
        }
    }
}
