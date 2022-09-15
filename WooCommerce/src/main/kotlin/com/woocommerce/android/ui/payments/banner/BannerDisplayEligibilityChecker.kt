package com.woocommerce.android.ui.payments.banner

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.AppUrls
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentCurrencySupportedChecker
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class BannerDisplayEligibilityChecker @Inject constructor(
    private val wooCommerceStore: WooCommerceStore,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val dispatchers: CoroutineDispatchers,
    private val selectedSite: SelectedSite,
    private val cardReaderCountryConfigProvider: CardReaderCountryConfigProvider,
    private val cardReaderPaymentCurrencySupportedChecker: CardReaderPaymentCurrencySupportedChecker,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) {
    private suspend fun getStoreCountryCode(): String? {
        return withContext(dispatchers.io) {
            wooCommerceStore.getStoreCountryCode(selectedSite.get()) ?: null.also {
                WooLog.e(WooLog.T.CARD_READER, "Store's country code not found.")
            }
        }
    }

    suspend fun getPurchaseCardReaderUrl(source: String): String {
        trackBannerCtaClicked(source)
        val countryCode = getStoreCountryCode()
        return withContext(dispatchers.main) {
            "${AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}$countryCode"
        }
    }

    fun onRemindLaterClicked(currentTimeInMillis: Long, source: String) {
        trackBannerDismissEvent(isRemindLaterSelected = true, source)
        storeRemindLaterTimeStamp(currentTimeInMillis)
    }

    fun onDontShowAgainClicked(source: String) {
        trackBannerDismissEvent(isRemindLaterSelected = false, source)
        storeDismissBannerForever()
    }

    private fun storeRemindLaterTimeStamp(currentTimeInMillis: Long) {
        val site = selectedSite.get()
        appPrefsWrapper.setCardReaderUpsellBannerRemindMeLater(
            lastDialogDismissedInMillis = currentTimeInMillis,
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId
        )
    }

    private fun storeDismissBannerForever() {
        val site = selectedSite.get()
        appPrefsWrapper.setCardReaderUpsellBannerDismissed(
            isDismissed = true,
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId
        )
    }

    private fun isCardReaderUpsellBannerDismissedForever(): Boolean {
        val site = selectedSite.get()
        return appPrefsWrapper.isCardReaderUpsellBannerDismissedForever(
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId
        )
    }

    private fun getCardReaderUpsellBannerLastDismissed(): Long {
        val site = selectedSite.get()
        return appPrefsWrapper.getCardReaderUpsellBannerLastDismissed(
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId
        )
    }

    fun isLastDialogDismissedMoreThan14DaysAgo(currentTimeInMillis: Long): Boolean {
        val timeDifference = currentTimeInMillis - getCardReaderUpsellBannerLastDismissed()
        return TimeUnit.MILLISECONDS.toDays(timeDifference) > CARD_READER_UPSELL_BANNER_REMIND_THRESHOLD_IN_DAYS
    }

    fun hasTheMerchantDismissedBannerViaRemindMeLater(): Boolean {
        return getCardReaderUpsellBannerLastDismissed() != 0L
    }

    fun canShowCardReaderUpsellBanner(currentTimeInMillis: Long): Boolean {
        return (
            !isCardReaderUpsellBannerDismissedForever() &&
                (
                    !hasTheMerchantDismissedBannerViaRemindMeLater() ||
                        isLastDialogDismissedMoreThan14DaysAgo(currentTimeInMillis)
                    )
            )
    }

    suspend fun isEligibleForInPersonPayments(): Boolean {
        val cardReaderConfig = cardReaderCountryConfigProvider.provideCountryConfigFor(
            wooCommerceStore.getStoreCountryCode(selectedSite.get())
        )
        if (cardReaderConfig is CardReaderConfigForSupportedCountry) {
            return cardReaderPaymentCurrencySupportedChecker.isCurrencySupported(
                cardReaderConfig.currency
            )
        }
        return false
    }

    private fun trackBannerDismissEvent(isRemindLaterSelected: Boolean, source: String) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.FEATURE_CARD_DISMISSED,
            mapOf(
                AnalyticsTracker.KEY_BANNER_SOURCE to source,
                AnalyticsTracker.KEY_BANNER_CAMPAIGN_NAME to AnalyticsTracker.KEY_BANNER_UPSELL_CARD_READERS,
                AnalyticsTracker.KEY_BANNER_REMIND_LATER to isRemindLaterSelected
            )
        )
    }

    private fun trackBannerCtaClicked(source: String) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.FEATURE_CARD_CTA_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_BANNER_SOURCE to source,
                AnalyticsTracker.KEY_BANNER_CAMPAIGN_NAME to AnalyticsTracker.KEY_BANNER_UPSELL_CARD_READERS,
            )
        )
    }

    companion object {
        private const val CARD_READER_UPSELL_BANNER_REMIND_THRESHOLD_IN_DAYS = 14
    }
}
