package com.woocommerce.android.ui.prefs

import com.woocommerce.android.model.UserRole
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.ui.whatsnew.FeatureAnnouncementRepository
import com.woocommerce.android.util.BuildConfigWrapper
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.StringUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
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
    userEligibilityFetcher: UserEligibilityFetcher
) : MainSettingsContract.Presenter {
    private var appSettingsFragmentView: MainSettingsContract.View? = null

    private var jetpackMonitoringJob: Job? = null
    private val isUserAdmin = userEligibilityFetcher.getUser()?.roles?.contains(UserRole.Administrator) ?: false

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
            it == SiteConnectionType.JetpackConnectionPackage || it == SiteConnectionType.ApplicationPasswords
        }
        appSettingsFragmentView?.handleJetpackInstallOption(supportsJetpackInstallation = supportsJetpackInstallation)
        jetpackMonitoringJob?.cancel()
        if (supportsJetpackInstallation) {
            jetpackMonitoringJob = coroutineScope.launch {
                selectedSite.observe()
                    .filter { it?.isJetpackConnected == true }
                    .take(1)
                    .collect { setupJetpackInstallOption() }
            }
        }
    }

    override fun setupApplicationPasswordsSettings() {
        if (selectedSite.connectionType == SiteConnectionType.ApplicationPasswords) {
            appSettingsFragmentView?.handleApplicationPasswordsSettings()
        }
    }

    override val isDomainOptionVisible: Boolean
        get() = selectedSite.get().isWPComAtomic && isUserAdmin && FeatureFlag.DOMAIN_CHANGE.isEnabled()
}
