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
    NEW_SHIPPING_SUPPORT,
    ENDLESS_CAMPAIGNS_SUPPORT,
    REVAMP_WOO_SHIPPING,
    OBJECTIVE_SECTION,
    POS_NON_SIMPLE_PRODUCT_TYPES,
    POS_CASH_PAYMENTS,
    POS_RECEIPTS,
    PRODUCT_GLOBAL_UNIQUE_IDENTIFIER_SUPPORT;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }

            WC_SHIPPING_BANNER,
            BETTER_CUSTOMER_SEARCH_M2,
            ORDER_CREATION_AUTO_TAX_RATE,
            REVAMP_WOO_SHIPPING,
            POS_NON_SIMPLE_PRODUCT_TYPES,
            POS_CASH_PAYMENTS,
            POS_RECEIPTS,
            PRODUCT_GLOBAL_UNIQUE_IDENTIFIER_SUPPORT -> PackageUtils.isDebugBuild()

            NEW_SHIPPING_SUPPORT,
            ENDLESS_CAMPAIGNS_SUPPORT,
            OBJECTIVE_SECTION -> true
        }
    }
}
