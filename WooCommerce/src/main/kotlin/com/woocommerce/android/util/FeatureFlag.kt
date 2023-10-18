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
    IAP_FOR_STORE_CREATION,
    STORE_CREATION_ONBOARDING,
    REST_API_I2,
    ANALYTICS_HUB_FEEDBACK_BANNER,
    EU_SHIPPING_NOTIFICATION,
    PRIVACY_CHOICES,
    BLAZE,
    ORDER_CREATION_PRODUCT_DISCOUNTS,
    BETTER_CUSTOMER_SEARCH_M2,
    ORDER_CREATION_TAX_RATE_SELECTOR,
    ORDER_CREATION_AUTO_TAX_RATE,
    TTP_CA_UK,
    CUSTOM_AMOUNTS_M1,
    PRODUCT_CREATION_AI,
    BLAZE_ITERATION_2,
    PACKAGE_PHOTO_SCANNING;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }

            COUPONS_M2,
            JETPACK_CP,
            UNIFIED_ORDER_EDITING,
            STORE_CREATION_ONBOARDING,
            REST_API_I2,
            ANALYTICS_HUB_FEEDBACK_BANNER,
            EU_SHIPPING_NOTIFICATION,
            PRIVACY_CHOICES,
            BLAZE,
            ORDER_CREATION_PRODUCT_DISCOUNTS,
            ORDER_CREATION_TAX_RATE_SELECTOR,
            PRODUCT_CREATION_AI -> true

            TTP_CA_UK,
            CUSTOM_AMOUNTS_M1,
            MORE_MENU_INBOX,
            WC_SHIPPING_BANNER,
            BETTER_CUSTOMER_SEARCH_M2,
            ORDER_CREATION_AUTO_TAX_RATE,
            BLAZE_ITERATION_2,
            PACKAGE_PHOTO_SCANNING -> PackageUtils.isDebugBuild()

            IAP_FOR_STORE_CREATION -> false
        }
    }
}
