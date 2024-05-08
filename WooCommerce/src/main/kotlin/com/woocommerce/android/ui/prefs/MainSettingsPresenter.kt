package com.woocommerce.android.ui.prefs

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.notifications.NotificationChannelsHandler
import com.woocommerce.android.notifications.NotificationChannelsHandler.NewOrderNotificationSoundStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.whatsnew.FeatureAnnouncementRepository
import com.woocommerce.android.util.BuildConfigWrapper
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.util.StringUtils
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class MainSettingsPresenter @Inject constructor(
    private val selectedSite: SelectedSite,
    private val accountStore: AccountStore,
    private val wooCommerceStore: WooCommerceStore,
    private val featureAnnouncementRepository: FeatureAnnouncementRepository,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val accountRepository: AccountRepository,
    private val notificationChannelsHandler: NotificationChannelsHandler,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val getWooVersion: GetWooCorePluginCachedVersion,
) : MainSettingsContract.Presenter {
    private var appSettingsFragmentView: MainSettingsContract.View? = null

    override val isChaChingSoundEnabled: Boolean
        get() = notificationChannelsHandler.checkNewOrderNotificationSound() == NewOrderNotificationSoundStatus.DEFAULT

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
        val supportsJetpackInstallation = selectedSite.connectionType.let {
            it == SiteConnectionType.JetpackConnectionPackage ||
                it == SiteConnectionType.ApplicationPasswords
        }
        appSettingsFragmentView?.handleJetpackInstallOption(supportsJetpackInstallation = supportsJetpackInstallation)
    }

    override fun setupApplicationPasswordsSettings() {
        if (selectedSite.connectionType == SiteConnectionType.ApplicationPasswords) {
            appSettingsFragmentView?.handleApplicationPasswordsSettings()
        }
    }

    override fun onNotificationsClicked() {
        if (isChaChingSoundEnabled) {
            analyticsTracker.track(AnalyticsEvent.SETTINGS_NOTIFICATIONS_OPEN_CHANNEL_SETTINGS_BUTTON_TAPPED)
            appSettingsFragmentView?.showDeviceAppNotificationSettings()
        } else {
            appSettingsFragmentView?.showNotificationsSettingsScreen()
        }
    }

    override val isDomainOptionVisible: Boolean
        get() = selectedSite.get().isWPComAtomic

    override val isCloseAccountOptionVisible: Boolean
        get() = selectedSite.connectionType != SiteConnectionType.ApplicationPasswords &&
            accountRepository.getUserAccount()?.userName != null

    override val isThemePickerOptionVisible: Boolean
        get() = selectedSite.get().isWPComAtomic

    override val wooPluginVersion: String
        get() = getWooVersion() ?: ""
}
