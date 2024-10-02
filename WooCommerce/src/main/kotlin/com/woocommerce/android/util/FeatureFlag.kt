package com.woocommerce.android.util

import android.content.Context

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    DB_DOWNGRADE,
    INBOX,
    WC_SHIPPING_BANNER,
    BETTER_CUSTOMER_SEARCH_M2,
    ORDER_CREATION_AUTO_TAX_RATE,
    NEW_SHIPPING_SUPPORT,
    GOOGLE_ADS_M1,
    SHOW_INBOX_CTA,
    ENDLESS_CAMPAIGNS_SUPPORT,
    CUSTOM_FIELDS,
    REVAMP_WOO_SHIPPING;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }

            WC_SHIPPING_BANNER,
            BETTER_CUSTOMER_SEARCH_M2,
            ORDER_CREATION_AUTO_TAX_RATE,
            REVAMP_WOO_SHIPPING -> PackageUtils.isDebugBuild()

            NEW_SHIPPING_SUPPORT,
            INBOX,
            SHOW_INBOX_CTA,
            GOOGLE_ADS_M1,
            CUSTOM_FIELDS,
            ENDLESS_CAMPAIGNS_SUPPORT -> true
        }
    }
}
