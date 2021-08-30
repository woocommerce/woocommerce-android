package com.woocommerce.android.util

import android.content.Context
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.util.payment.CardPresentEligibleFeatureChecker

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    DB_DOWNGRADE,
    ORDER_CREATION,
    CARD_READER,
    PRODUCT_ADD_ONS;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }
            ORDER_CREATION -> PackageUtils.isDebugBuild() || PackageUtils.isTesting()
            CARD_READER -> CardPresentEligibleFeatureChecker.isCardPresentEligible
            PRODUCT_ADD_ONS ->
                (PackageUtils.isDebugBuild() || PackageUtils.isTesting()) &&
                    AppPrefs.isProductAddonsEnabled
        }
    }
}
