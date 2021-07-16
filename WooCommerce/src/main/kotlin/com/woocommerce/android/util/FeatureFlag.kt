package com.woocommerce.android.util

import android.content.Context
import com.woocommerce.android.util.payment.CardPresentEligibleFeatureChecker

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    SHIPPING_LABELS_M4,
    DB_DOWNGRADE,
    ORDER_CREATION,
    CARD_READER,
    CARD_READER_ONBOARDING;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            SHIPPING_LABELS_M4 -> PackageUtils.isDebugBuild() || PackageUtils.isTesting()
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }
            ORDER_CREATION -> PackageUtils.isDebugBuild() || PackageUtils.isTesting()
            CARD_READER -> CardPresentEligibleFeatureChecker.isCardPresentEligible
            CARD_READER_ONBOARDING -> CARD_READER.isEnabled() && PackageUtils.isDebugBuild()
        }
    }
}
