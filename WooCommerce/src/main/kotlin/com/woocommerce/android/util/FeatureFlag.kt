package com.woocommerce.android.util

import android.content.Context

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    DB_DOWNGRADE,
    MORE_MENU_INBOX,
    WC_SHIPPING_BANNER,
    IAP_FOR_STORE_CREATION,
    PRIVACY_CHOICES,
    ORDER_CREATION_PRODUCT_DISCOUNTS,
    BETTER_CUSTOMER_SEARCH_M2,
    AI_ORDER_DETAIL_THANK_YOU_NOTE,
    ORDER_CREATION_TAX_RATE_SELECTOR,
    ORDER_CREATION_AUTO_TAX_RATE,
    CUSTOM_AMOUNTS_M1,
    PRODUCT_CREATION_AI,
    PACKAGE_PHOTO_SCANNING,
    THEME_PICKER,
    ORDER_GIFT_CARD;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }

            PRIVACY_CHOICES,
            ORDER_CREATION_PRODUCT_DISCOUNTS,
            ORDER_CREATION_TAX_RATE_SELECTOR,
            PRODUCT_CREATION_AI,
            PACKAGE_PHOTO_SCANNING,
            CUSTOM_AMOUNTS_M1,
            AI_ORDER_DETAIL_THANK_YOU_NOTE -> true

            MORE_MENU_INBOX,
            WC_SHIPPING_BANNER,
            BETTER_CUSTOMER_SEARCH_M2,
            ORDER_CREATION_AUTO_TAX_RATE,
            THEME_PICKER,
            ORDER_GIFT_CARD -> PackageUtils.isDebugBuild()

            IAP_FOR_STORE_CREATION -> false
        }
    }
}
