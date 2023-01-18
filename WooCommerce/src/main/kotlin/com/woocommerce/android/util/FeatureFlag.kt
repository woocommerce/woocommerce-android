package com.woocommerce.android.util

import android.content.Context

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    DB_DOWNGRADE,
    JETPACK_CP,
    MORE_MENU_INBOX,
    COUPONS_M2,
    WC_SHIPPING_BANNER,
    UNIFIED_ORDER_EDITING,
    ORDER_CREATION_CUSTOMER_SEARCH,
    NATIVE_STORE_CREATION_FLOW,
    REST_API,
    IAP_FOR_STORE_CREATION,
    IPP_TAP_TO_PAY,
    DOMAIN_CHANGE;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }

            COUPONS_M2,
            JETPACK_CP,
            ORDER_CREATION_CUSTOMER_SEARCH,
            UNIFIED_ORDER_EDITING,
            NATIVE_STORE_CREATION_FLOW -> true

            MORE_MENU_INBOX,
            WC_SHIPPING_BANNER,
            REST_API,
            IAP_FOR_STORE_CREATION,
            IPP_TAP_TO_PAY,
            DOMAIN_CHANGE -> PackageUtils.isDebugBuild()
        }
    }
}
