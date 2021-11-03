package com.woocommerce.android.model

import com.woocommerce.android.FeedbackPrefs
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
        SHIPPING_LABELS_M4("shipping_labels_m4"),
        PRODUCTS_VARIATIONS("products_variations"),
        PRODUCT_ADDONS("product_addons"),
        QUICK_ORDER("quick_order")
    }

    fun registerItselfWith(featureKey: String) {
        FeedbackPrefs.setFeatureFeedbackSettings(featureKey, this)
    }
}
