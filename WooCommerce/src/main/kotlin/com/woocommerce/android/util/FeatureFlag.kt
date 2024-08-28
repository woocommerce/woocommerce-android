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
    NEW_SHIPPING_SUPPORT,
    GOOGLE_ADS_M1,
    SHOW_INBOX_CTA,
    CUSTOM_FIELDS;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }

            WOO_POS,
            WC_SHIPPING_BANNER,
            BETTER_CUSTOMER_SEARCH_M2,
            ORDER_CREATION_AUTO_TAX_RATE,
            CUSTOM_FIELDS -> PackageUtils.isDebugBuild()

            NEW_SHIPPING_SUPPORT,
            INBOX,
            SHOW_INBOX_CTA,
            GOOGLE_ADS_M1 -> true
        }
    }
}
