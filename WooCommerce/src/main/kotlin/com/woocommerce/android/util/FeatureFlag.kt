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
    IAP_FOR_STORE_CREATION,
    IPP_TAP_TO_PAY,
    IPP_FEEDBACK_BANNER,
    IPP_UK,
    STORE_CREATION_ONBOARDING,
    FREE_TRIAL_M2,
    REST_API_I2,
    ANALYTICS_HUB_FEEDBACK_BANNER,
    GIFT_CARD_READ_ONLY_SUPPORT,
    QUANTITY_RULES_READ_ONLY_SUPPORT,
    BUNDLED_PRODUCTS_READ_ONLY_SUPPORT,
    COMPOSITE_PRODUCTS_READ_ONLY_SUPPORT,
    STORE_CREATION_PROFILER,
    EU_SHIPPING_NOTIFICATION,
    IPP_ADD_PRODUCT_VIA_BARCODE_SCANNER;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }

            COUPONS_M2,
            JETPACK_CP,
            ORDER_CREATION_CUSTOMER_SEARCH,
            UNIFIED_ORDER_EDITING,
            NATIVE_STORE_CREATION_FLOW,
            IPP_FEEDBACK_BANNER,
            FREE_TRIAL_M2,
            STORE_CREATION_ONBOARDING,
            REST_API_I2,
            GIFT_CARD_READ_ONLY_SUPPORT,
            QUANTITY_RULES_READ_ONLY_SUPPORT,
            BUNDLED_PRODUCTS_READ_ONLY_SUPPORT,
            IPP_UK,
            ANALYTICS_HUB_FEEDBACK_BANNER,
            STORE_CREATION_PROFILER,
            COMPOSITE_PRODUCTS_READ_ONLY_SUPPORT -> true

            MORE_MENU_INBOX,
            WC_SHIPPING_BANNER,
            IPP_TAP_TO_PAY,
            IPP_ADD_PRODUCT_VIA_BARCODE_SCANNER,
            EU_SHIPPING_NOTIFICATION -> PackageUtils.isDebugBuild()

            IAP_FOR_STORE_CREATION -> false
        }
    }
}
