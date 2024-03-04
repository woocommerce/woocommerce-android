package com.woocommerce.android.util

import android.content.Context

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    DB_DOWNGRADE,
    MORE_MENU_INBOX,
    BETTER_TABLETS_SUPPORT_PRODUCTS,
    WC_SHIPPING_BANNER,
    IAP_FOR_STORE_CREATION,
    BETTER_CUSTOMER_SEARCH_M2,
    ORDER_CREATION_AUTO_TAX_RATE,
    BLAZE_I3,
    EXPANDED_ANALYTIC_HUB_M2,
    CONNECTIVITY_TOOL;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }

            BETTER_TABLETS_SUPPORT_PRODUCTS,
            MORE_MENU_INBOX,
            WC_SHIPPING_BANNER,
            BETTER_CUSTOMER_SEARCH_M2,
            ORDER_CREATION_AUTO_TAX_RATE,
            BLAZE_I3,
            EXPANDED_ANALYTIC_HUB_M2,
            CONNECTIVITY_TOOL-> PackageUtils.isDebugBuild()

            IAP_FOR_STORE_CREATION -> false
        }
    }
}
