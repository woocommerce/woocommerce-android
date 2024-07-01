package com.woocommerce.android.util

import android.content.Context

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    WOO_POS,
    DB_DOWNGRADE,
    INBOX,
    WC_SHIPPING_BANNER,
    BETTER_CUSTOMER_SEARCH_M2,
    ORDER_CREATION_AUTO_TAX_RATE,
    CONNECTIVITY_TOOL,
    NEW_SHIPPING_SUPPORT,
    APP_PASSWORD_TUTORIAL,
    EOSL_M1,
    EOSL_M3,
    GOOGLE_ADS_M1;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }

            INBOX,
            WOO_POS,
            WC_SHIPPING_BANNER,
            BETTER_CUSTOMER_SEARCH_M2,
            ORDER_CREATION_AUTO_TAX_RATE,
            GOOGLE_ADS_M1 -> PackageUtils.isDebugBuild()

            CONNECTIVITY_TOOL,
            NEW_SHIPPING_SUPPORT,
            APP_PASSWORD_TUTORIAL,
            EOSL_M1,
            EOSL_M3 -> true
        }
    }
}
