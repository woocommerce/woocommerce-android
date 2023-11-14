package com.woocommerce.android.ui.payments.feedback.ipp

import com.woocommerce.android.AppPrefsWrapper
import javax.inject.Inject

class MarkIPPFeedbackSurveyAsCompleted @Inject constructor(
    private val prefs: AppPrefsWrapper,
) {
    operator fun invoke() {
        prefs.setIPPFeedbackSurveyCompleted(true)
    }
}
