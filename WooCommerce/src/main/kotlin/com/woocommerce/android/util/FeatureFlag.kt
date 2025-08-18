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
    REVAMP_WOO_SHIPPING,
    BULK_UPDATE_ORDERS_STATUS,
    WOO_POS_SETTINGS,
    WOO_POS_LOCAL_CATALOG_M1,
    HIDE_SITES_FROM_SITE_PICKER,
    AI_PRODUCT_IMAGE_BACKGROUND_REMOVAL;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }

            WOO_POS_SETTINGS,
            WC_SHIPPING_BANNER,
            BETTER_CUSTOMER_SEARCH_M2,
            ORDER_CREATION_AUTO_TAX_RATE,
            AI_PRODUCT_IMAGE_BACKGROUND_REMOVAL,
            WOO_POS_LOCAL_CATALOG_M1 -> PackageUtils.isDebugBuild()

            NEW_SHIPPING_SUPPORT,
            BULK_UPDATE_ORDERS_STATUS,
            HIDE_SITES_FROM_SITE_PICKER,
            REVAMP_WOO_SHIPPING -> true
        }
    }
}
