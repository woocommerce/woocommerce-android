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
        GROUPED_PRODUCTS("grouped_products")
    }
}
