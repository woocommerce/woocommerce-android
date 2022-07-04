package com.woocommerce.android.model

import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.UNANSWERED

data class FeatureFeedbackSettings(
    val feature: Feature,
    val feedbackState: FeedbackState = UNANSWERED
) {
    val key
        get() = feature.toString()

    fun registerItself() = FeedbackPrefs.setFeatureFeedbackSettings(this)

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
        SIMPLE_PAYMENTS_AND_ORDER_CREATION,
        COUPONS
    }
}
