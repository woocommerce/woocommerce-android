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
    GOOGLE_ADS_M1,
    GOOGLE_ADS_ANALYTICS_HUB_M1,
    SHOW_INBOX_CTA;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }

            WC_SHIPPING_BANNER,
            BETTER_CUSTOMER_SEARCH_M2,
            ORDER_CREATION_AUTO_TAX_RATE -> PackageUtils.isDebugBuild()

            WOO_POS,
            CONNECTIVITY_TOOL,
            NEW_SHIPPING_SUPPORT,
            APP_PASSWORD_TUTORIAL,
            INBOX,
            GOOGLE_ADS_ANALYTICS_HUB_M1,
            SHOW_INBOX_CTA,
            GOOGLE_ADS_M1 -> true
        }
    }
}
