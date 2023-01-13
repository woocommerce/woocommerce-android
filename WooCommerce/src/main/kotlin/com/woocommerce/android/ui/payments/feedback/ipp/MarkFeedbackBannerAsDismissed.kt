package com.woocommerce.android.ui.payments.feedback.ipp

import com.woocommerce.android.AppPrefsWrapper
import java.util.Date
import javax.inject.Inject

/**
 * This class is used to mark the IPP feedback banner as dismissed for now.
 * If the user dismisses the banner, it will be shown again after a time interval defined
 * in [ShouldShowFeedbackBanner.REMIND_LATER_INTERVAL_DAYS].
 */
class MarkFeedbackBannerAsDismissed @Inject constructor(
    private val prefs: AppPrefsWrapper,
) {
    operator fun invoke() {
        prefs.setIPPFeedbackBannerDismissedRemindLater(Date().time)
    }
}
