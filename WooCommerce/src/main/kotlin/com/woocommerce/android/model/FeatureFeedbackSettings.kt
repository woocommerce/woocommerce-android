package com.woocommerce.android.model

import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.UNANSWERED

data class FeatureFeedbackSettings(
    val feature: Feature,
    val state: FeedbackState = UNANSWERED
) {

    enum class FeedbackState {
        GIVEN,
        DISMISSED,
        UNANSWERED
    }

    fun registerItself() = FeedbackPrefs.setFeatureFeedbackSettings(this)

    sealed class Feature(private val requestingView: String) {
        class ShippingLabelM4(requestingView: String): Feature(requestingView)
        class ProductVariations(requestingView: String): Feature(requestingView)
        class ProductAddons(requestingView: String): Feature(requestingView)
        class SimplePayments(requestingView: String): Feature(requestingView)
        class Coupons(requestingView: String): Feature(requestingView)

        val tag
            get() = requestingView + "_" + javaClass.simpleName
    }
}
