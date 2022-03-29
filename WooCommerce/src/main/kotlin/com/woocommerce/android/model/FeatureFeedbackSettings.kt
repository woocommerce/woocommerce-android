package com.woocommerce.android.model

import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.UNANSWERED

data class FeatureFeedbackSettings(
    val featureKey: FeatureKey,
    val state: FeedbackState = UNANSWERED
) {

    enum class FeedbackState {
        GIVEN,
        DISMISSED,
        UNANSWERED
    }

    enum class Feature {
        SHIPPING_LABEL_M4,
        PRODUCT_VARIATIONS,
        PRODUCT_ADDONS,
        SIMPLE_PAYMENTS,
        COUPONS
    }

    data class FeatureKey(
        private val requestingView: String,
        private val feature: Feature
    ) {
        val value
            get() = requestingView + "_" + feature::class.java.simpleName
    }

    fun registerItself() = FeedbackPrefs.setFeatureFeedbackSettings(this)
}
