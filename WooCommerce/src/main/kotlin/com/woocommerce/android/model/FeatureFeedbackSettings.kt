package com.woocommerce.android.model

import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.UNANSWERED

data class FeatureFeedbackSettings(
    val name: String,
    val state: FeedbackState = UNANSWERED
) {
    val shouldRequestFeedback
        get() = state == UNANSWERED

    enum class FeedbackState {
        GIVEN,
        DISMISSED,
        UNANSWERED
    }

    enum class Feature(val description: String) {
        SHIPPING_LABELS_M1("shipping_labels_m1"),
        PRODUCTS_M3("products_m3"),
        PRODUCTS_M2("products_m2")
    }
}
