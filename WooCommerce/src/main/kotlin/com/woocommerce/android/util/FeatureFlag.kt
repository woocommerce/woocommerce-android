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
    WOO_POS_SURVEYS,
    WOO_POS_HISTORICAL_ORDERS_M1,
    WOO_POS_LOCAL_CATALOG_M1,
    BOOKINGS_MVP;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }

            WOO_POS_HISTORICAL_ORDERS_M1,
            WC_SHIPPING_BANNER,
            WOO_POS_SURVEYS,
            BETTER_CUSTOMER_SEARCH_M2,
            ORDER_CREATION_AUTO_TAX_RATE,
            WOO_POS_LOCAL_CATALOG_M1,
            BOOKINGS_MVP -> PackageUtils.isDebugBuild()
        }
    }
}
