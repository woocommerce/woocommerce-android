package com.woocommerce.android.util

import android.content.Context

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    DB_DOWNGRADE,
    ORDER_CREATION_M2,
    CARD_READER,
    JETPACK_CP,
    ORDER_FILTERS,
    ANALYTICS_HUB,
    PAYMENTS_STRIPE_EXTENSION,
    SIMPLE_PAYMENT_I2,
    IN_PERSON_PAYMENTS_CANADA;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }
            ORDER_CREATION_M2 -> false
            JETPACK_CP,
            CARD_READER -> true // Keeping the flag for a few sprints so we can quickly disable the feature if needed
            PAYMENTS_STRIPE_EXTENSION -> false
            ORDER_FILTERS,
            SIMPLE_PAYMENT_I2,
            ANALYTICS_HUB,
            IN_PERSON_PAYMENTS_CANADA -> PackageUtils.isDebugBuild()
        }
    }
}
