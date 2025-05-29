package com.woocommerce.android.util

import android.content.Context

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    DB_DOWNGRADE,
    WC_SHIPPING_BANNER,
    BETTER_CUSTOMER_SEARCH_M2,
    ORDER_CREATION_AUTO_TAX_RATE,
    POS_RECEIPTS,
    NEW_SHIPPING_SUPPORT,
    REVAMP_WOO_SHIPPING,
    BULK_UPDATE_ORDERS_STATUS,
    HIDE_SITES_FROM_SITE_PICKER;

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
            POS_RECEIPTS,
            BULK_UPDATE_ORDERS_STATUS,
            HIDE_SITES_FROM_SITE_PICKER -> true
        }
    }
}
