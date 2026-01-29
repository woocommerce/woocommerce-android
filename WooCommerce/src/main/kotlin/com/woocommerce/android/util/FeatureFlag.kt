package com.woocommerce.android.util

import android.content.Context
import com.woocommerce.android.AppPrefs

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    DB_DOWNGRADE,
    WC_SHIPPING_BANNER,
    BETTER_CUSTOMER_SEARCH_M2,
    ORDER_CREATION_AUTO_TAX_RATE,
    BOOKINGS_MVP,
    POS_REFUNDS,
    POS_PRODUCTS_FTS,
    WOO_POS_LOCAL_CATALOG_FILE_APPROACH,
    WOO_PUSH_NOTIFICATIONS_SYSTEM,
    WOO_PUSH_NOTIFICATIONS_SYSTEM_M2,
    WOO_POS_CLIENT_SIDE_BANNER,
    AGE_ELIGIBILITY_CHECKS;

    fun isEnabled(context: Context? = null): Boolean {
        if (PackageUtils.isDebugBuild()) {
            return try {
                AppPrefs.isFeatureFlagOverrideEnabled(this, getDefaultValue(context))
            } catch (_: UninitializedPropertyAccessException) {
                getDefaultValue(context)
            }
        }
        return getDefaultValue(context)
    }

    fun getDefaultValue(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }

            WC_SHIPPING_BANNER,
            BETTER_CUSTOMER_SEARCH_M2,
            ORDER_CREATION_AUTO_TAX_RATE,
            BOOKINGS_MVP,
            POS_PRODUCTS_FTS,
            POS_REFUNDS,
            WOO_POS_LOCAL_CATALOG_FILE_APPROACH,
            WOO_POS_CLIENT_SIDE_BANNER,
            AGE_ELIGIBILITY_CHECKS -> PackageUtils.isDebugBuild()

            WOO_PUSH_NOTIFICATIONS_SYSTEM,
            WOO_PUSH_NOTIFICATIONS_SYSTEM_M2 -> false
        }
    }
}
