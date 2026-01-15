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
    BOOKINGS_MVP,
    CLIENT_SIDE_POS_BANNER,
    POS_REFUNDS,
    WOO_POS_LOCAL_CATALOG_FILE_APPROACH,
    WOO_PUSH_NOTIFICATIONS_SYSTEM,
    WOO_POS_PRODUCT_VISIBILITY_FILTERING;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }

            WC_SHIPPING_BANNER,
            BETTER_CUSTOMER_SEARCH_M2,
            ORDER_CREATION_AUTO_TAX_RATE,
            CLIENT_SIDE_POS_BANNER,
            BOOKINGS_MVP,
            POS_REFUNDS,
            WOO_POS_PRODUCT_VISIBILITY_FILTERING -> PackageUtils.isDebugBuild()

            WOO_POS_LOCAL_CATALOG_FILE_APPROACH,
            WOO_PUSH_NOTIFICATIONS_SYSTEM -> false
        }
    }
}
