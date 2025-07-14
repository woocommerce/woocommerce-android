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
    WOO_POS_SCANNER_SETUP,
    NEW_SHIPPING_SUPPORT,
    WOO_POS_AS_A_TAB_I2,
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
            WOO_POS_AS_A_TAB_I2,
            ORDER_CREATION_AUTO_TAX_RATE,
            WOO_POS_SCANNER_SETUP -> PackageUtils.isDebugBuild()

            NEW_SHIPPING_SUPPORT,
            BULK_UPDATE_ORDERS_STATUS,
            HIDE_SITES_FROM_SITE_PICKER,
            REVAMP_WOO_SHIPPING -> true
        }
    }
}
