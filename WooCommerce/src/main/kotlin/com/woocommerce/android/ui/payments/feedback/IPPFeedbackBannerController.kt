package com.woocommerce.android.ui.payments.feedback

import com.woocommerce.android.AppPrefsWrapper
import java.util.Date
import javax.inject.Inject

class MarkFeedbackBannerAsDismissed @Inject constructor(
    private val prefs: AppPrefsWrapper,
) {
    fun invoke() {
        prefs.setIPPFeedbackBannerDismissedRemindLater(Date().time)
    }
}

class MarkFeedbackBannerAsDismissedForever @Inject constructor(
    private val prefs: AppPrefsWrapper,
) {
    fun invoke() {
        prefs.setIPPFeedbackBannerDismissedForever(true)
    }
}

class MarkIPPFeedbackSurveyAsCompleted @Inject constructor(
    private val prefs: AppPrefsWrapper,
) {
    fun invoke() {
        prefs.setIPPFeedbackSurveyCompleted(true)
    }
}
