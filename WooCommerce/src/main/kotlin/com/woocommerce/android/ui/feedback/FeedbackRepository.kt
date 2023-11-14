package com.woocommerce.android.ui.feedback

import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.model.FeatureFeedbackSettings.Feature
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState
import javax.inject.Inject

class FeedbackRepository @Inject constructor(private val feedbackPrefs: FeedbackPrefs) {
    fun getFeatureFeedbackState(feature: Feature): FeedbackState {
        return feedbackPrefs.getFeatureFeedbackSettings(feature)?.feedbackState ?: FeedbackState.UNANSWERED
    }

    fun saveFeatureFeedback(feature: Feature, feedbackState: FeedbackState) {
        feedbackPrefs.setFeatureFeedbackSettings(FeatureFeedbackSettings(feature, feedbackState))
    }

    fun getFeatureFeedbackSetting(feature: Feature): FeatureFeedbackSettings {
        return feedbackPrefs.getFeatureFeedbackSettings(feature) ?: FeatureFeedbackSettings(feature)
    }
}
