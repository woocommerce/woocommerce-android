package com.woocommerce.android.ui.feedback

import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.model.FeatureFeedbackSettings.Feature
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState
import javax.inject.Inject

class FeedbackRepository @Inject constructor() {
    fun getFeatureFeedbackState(feature: Feature): FeedbackState {
        return FeedbackPrefs.getFeatureFeedbackSettings(feature)?.feedbackState ?: FeedbackState.UNANSWERED
    }

    fun saveFeatureFeedback(feature: Feature, feedbackState: FeedbackState) {
        FeedbackPrefs.setFeatureFeedbackSettings(FeatureFeedbackSettings(feature, feedbackState))
    }

    fun getFeatureFeedbackSetting(feature: Feature): FeatureFeedbackSettings {
        return FeedbackPrefs.getFeatureFeedbackSettings(feature) ?: FeatureFeedbackSettings(feature)
    }
}
