package com.woocommerce.android.ui.payments.feedback.ipp

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.payments.GetActivePaymentsPlugin
import com.woocommerce.android.ui.payments.cardreader.CashOnDeliverySettingsRepository
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Use case to check if the IPP feedback banner should be shown to the user.
 */
class ShouldShowFeedbackBanner @Inject constructor(
    private val prefs: AppPrefsWrapper,
    private val getActivePaymentsPlugin: GetActivePaymentsPlugin,
    private val cashOnDeliverySettings: CashOnDeliverySettingsRepository,
) {
    /**
     * 1. Check if COD is enabled, if not return false.
     * 2. Check if WCPay plugin is used if no, return false.
     * 3. Check if survey has been already completed, if yes return false.
     * 4. Check if survey has been dismissed forever, if yes return false,
     * if not, check if it was last dismissed >= 7 days ago, if yes return true.
     * 5. Return false.
     */
    suspend fun invoke(): Boolean {
        if (!cashOnDeliverySettings.isCashOnDeliveryEnabled()) return false
        if (getActivePaymentsPlugin.invoke() != WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS)
            return false
        if (prefs.isIPPFeedbackSurveyCompleted()) return false
        if (prefs.isIPPFeedbackBannerDismissedForever()) return false

        val lastDismissed = prefs.getIPPFeedbackBannerLastDismissed()
        val now = Calendar.getInstance().time.time

        val differenceDays = TimeUnit.DAYS.convert(now - lastDismissed, TimeUnit.MILLISECONDS)
        if (differenceDays >= DAYS_REMIND_LATER) return true

        return false
    }

    private companion object {
        private const val DAYS_REMIND_LATER = 7
    }
}
