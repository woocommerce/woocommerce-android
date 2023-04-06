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
    STORE_CREATION_ONBOARDING,
    FREE_TRIAL_M2,
    REST_API_I2,
    ANALYTICS_HUB_FEEDBACK_BANNER,
    STORE_CREATION_PROFILER;

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
            STORE_CREATION_ONBOARDING -> true

            MORE_MENU_INBOX,
            WC_SHIPPING_BANNER,
            IPP_TAP_TO_PAY,
            REST_API_I2,
            ANALYTICS_HUB_FEEDBACK_BANNER -> PackageUtils.isDebugBuild()

            IAP_FOR_STORE_CREATION,
            STORE_CREATION_PROFILER -> false
        }
    }
}
