package com.woocommerce.android.util

import android.content.Context

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    DB_DOWNGRADE,
    TABLET_ORDERS_M1,
    MORE_MENU_INBOX,
    WC_SHIPPING_BANNER,
    IAP_FOR_STORE_CREATION,
    BETTER_CUSTOMER_SEARCH_M2,
    ORDER_CREATION_AUTO_TAX_RATE,
    ORDER_GIFT_CARD;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }

            ORDER_GIFT_CARD -> true

            MORE_MENU_INBOX,
            WC_SHIPPING_BANNER,
            BETTER_CUSTOMER_SEARCH_M2,
            ORDER_CREATION_AUTO_TAX_RATE,
            TABLET_ORDERS_M1 -> PackageUtils.isDebugBuild()

            IAP_FOR_STORE_CREATION -> false
        }
    }
}
