package com.woocommerce.android.ui.payments.feedback.ipp

import com.woocommerce.android.AppPrefsWrapper
import javax.inject.Inject

/**
 * This class is used to mark the IPP feedback banner as dismissed forever.
 */
class MarkIPPFeedbackSurveyAsCompleted @Inject constructor(
    private val prefs: AppPrefsWrapper,
) {
    operator fun invoke() {
        prefs.setIPPFeedbackSurveyCompleted(true)
    }
}
