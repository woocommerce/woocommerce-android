package com.woocommerce.android.ui.payments.feedback.ipp

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.payments.GetActivePaymentsPlugin
import com.woocommerce.android.ui.payments.cardreader.CashOnDeliverySettingsRepository
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import org.wordpress.android.fluxc.store.WooCommerceStore
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
    private val wooCommerceStore: WooCommerceStore,
    private val siteModel: SiteModel,
) {
    /**
     * 1. Check if store's country code is US or CA, if not, return false
     * 2. Check if COD is enabled, if not return false.
     * 3. Check if WCPay plugin is used if no, return false.
     * 4. Check if survey has been already completed, if yes return false.
     * 5. Check if survey has been dismissed forever, if yes return false,
     * if not, check if it was last dismissed >= 7 days ago, if yes return true.
     * 6. Return false.
     */
    suspend operator fun invoke(): Boolean {
        return isStoreInSupportedCountry() &&
            isIPPEnabled() &&
            !isSurveyCompletedOrDismissedForever() &&
            wasDismissedLongAgoEnoughToShowAgain()
    }

    private fun isStoreInSupportedCountry(): Boolean =
        wooCommerceStore.getSiteSettings(siteModel)?.countryCode in SUPPORTED_COUNTRIES

    private suspend fun isIPPEnabled(): Boolean =
        cashOnDeliverySettings.isCashOnDeliveryEnabled() && isWooCommercePaymentsPluginEnabled()

    private fun isSurveyCompletedOrDismissedForever(): Boolean =
        prefs.isIPPFeedbackSurveyCompleted() || prefs.isIPPFeedbackBannerDismissedForever()

    private suspend fun isWooCommercePaymentsPluginEnabled() =
        getActivePaymentsPlugin() == WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS

    private fun wasDismissedLongAgoEnoughToShowAgain(): Boolean {
        val lastDismissed = prefs.getIPPFeedbackBannerLastDismissed()
        val now = Calendar.getInstance().time.time
        val differenceDays = TimeUnit.DAYS.convert(now - lastDismissed, TimeUnit.MILLISECONDS)
        return differenceDays >= REMIND_LATER_INTERVAL_DAYS
    }

    private companion object {
        private const val REMIND_LATER_INTERVAL_DAYS = 7
        val SUPPORTED_COUNTRIES = listOf("US", "CA")
    }
}
