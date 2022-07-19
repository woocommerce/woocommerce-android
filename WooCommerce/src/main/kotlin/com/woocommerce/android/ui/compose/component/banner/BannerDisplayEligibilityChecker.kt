package com.woocommerce.android.ui.compose.component.banner

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.AppUrls
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
) {
    private suspend fun getStoreCountryCode(): String? {
        return withContext(dispatchers.io) {
            wooCommerceStore.getStoreCountryCode(selectedSite.get()) ?: null.also {
                WooLog.e(WooLog.T.CARD_READER, "Store's country code not found.")
            }
        }
    }

    suspend fun getPurchaseCardReaderUrl(): String {
        val countryCode = getStoreCountryCode()
        return withContext(dispatchers.main) {
            "${AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}$countryCode"
        }
    }

    fun onRemindLaterClicked(currentTimeInMillis: Long) {
        storeRemindLaterTimeStamp(currentTimeInMillis)
    }

    fun onDontShowAgainClicked() {
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

    fun isCardReaderUpsellBannerDismissedForever(): Boolean {
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
        return !isCardReaderUpsellBannerDismissedForever() &&
            (
                !hasTheMerchantDismissedBannerViaRemindMeLater() || hasTheMerchantDismissedBannerViaRemindMeLater() &&
                    isLastDialogDismissedMoreThan14DaysAgo(currentTimeInMillis)
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

    companion object {
        private const val CARD_READER_UPSELL_BANNER_REMIND_THRESHOLD_IN_DAYS = 14
    }
}
