package com.woocommerce.android.ui.prefs

import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.AppUrls
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.whatsnew.FeatureAnnouncementRepository
import com.woocommerce.android.util.BuildConfigWrapper
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainSettingsPresenter @Inject constructor(
    private val selectedSite: SelectedSite,
    private val accountStore: AccountStore,
    private val wooCommerceStore: WooCommerceStore,
    private val featureAnnouncementRepository: FeatureAnnouncementRepository,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val dispatchers: CoroutineDispatchers,
) : MainSettingsContract.Presenter {
    private var appSettingsFragmentView: MainSettingsContract.View? = null

    private var jetpackMonitoringJob: Job? = null

    override val shouldShowUpsellCardReaderDismissDialog: MutableLiveData<Boolean> = MutableLiveData(false)

    override fun takeView(view: MainSettingsContract.View) {
        appSettingsFragmentView = view
    }

    override fun dropView() {
        appSettingsFragmentView = null
    }

    override fun getUserDisplayName(): String = accountStore.account.displayName

    override fun getStoreDomainName(): String {
        return selectedSite.getIfExists()?.let { site ->
            StringUtils.getSiteDomainAndPath(site)
        } ?: ""
    }

    override fun hasMultipleStores() = wooCommerceStore.getWooCommerceSites().size > 1

    override fun setupAnnouncementOption() {
        coroutineScope.launch {
            val result = featureAnnouncementRepository.getLatestFeatureAnnouncement(true)
                ?: featureAnnouncementRepository.getLatestFeatureAnnouncement(false)
            result?.let {
                if (it.canBeDisplayedOnAppUpgrade(buildConfigWrapper.versionName)) {
                    appSettingsFragmentView?.showLatestAnnouncementOption(it)
                }
            }
        }
    }

    override fun setupJetpackInstallOption() {
        appSettingsFragmentView?.handleJetpackInstallOption(selectedSite.get().isJetpackCPConnected)
        jetpackMonitoringJob?.cancel()
        if (selectedSite.get().isJetpackCPConnected) {
            jetpackMonitoringJob = coroutineScope.launch {
                selectedSite.observe()
                    .filter { it?.isJetpackConnected == true }
                    .take(1)
                    .collect { setupJetpackInstallOption() }
            }
        }
    }

    override fun onCtaClicked() {
        coroutineScope.launch {
            val countryCode = getStoreCountryCode()
            withContext(dispatchers.main) {
                appSettingsFragmentView?.openPurchaseCardReaderLink(
                    "${AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}$countryCode"
                )
            }
        }
    }

    override fun onDismissClicked() {
        shouldShowUpsellCardReaderDismissDialog.value = true
        appSettingsFragmentView?.dismissUpsellCardReaderBanner()
    }

    override fun onRemindLaterClicked(currentTimeInMillis: Long) {
        shouldShowUpsellCardReaderDismissDialog.value = false
        storeRemindLaterTimeStamp(currentTimeInMillis)
        appSettingsFragmentView?.dismissUpsellCardReaderBannerViaRemindLater()
    }

    override fun onDontShowAgainClicked() {
        shouldShowUpsellCardReaderDismissDialog.value = false
        storeDismissBannerForever()
        appSettingsFragmentView?.dismissUpsellCardReaderBannerViaDontShowAgain()
    }

    private suspend fun getStoreCountryCode(): String? {
        return withContext(dispatchers.io) {
            wooCommerceStore.getStoreCountryCode(selectedSite.get()) ?: null.also {
                WooLog.e(WooLog.T.CARD_READER, "Store's country code not found.")
            }
        }
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

    private fun hasTheMerchantDismissedBannerViaRemindMeLater(): Boolean {
        return getCardReaderUpsellBannerLastDismissed() != 0L
    }

    override fun canShowCardReaderUpsellBanner(currentTimeInMillis: Long): Boolean {
        return !isCardReaderUpsellBannerDismissedForever() &&
            (
                !hasTheMerchantDismissedBannerViaRemindMeLater() || hasTheMerchantDismissedBannerViaRemindMeLater() &&
                    isLastDialogDismissedMoreThan14DaysAgo(currentTimeInMillis)
                )
    }

    companion object {
        private const val CARD_READER_UPSELL_BANNER_REMIND_THRESHOLD_IN_DAYS = 14
    }
}
