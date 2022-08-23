package com.woocommerce.android.ui.sitepicker

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.getSiteName
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigateToWPComWebView
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitesListItem.Header
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitesListItem.NonWooSiteUiModel
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitesListItem.WooSiteUiModel
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Logout
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.text.RegexOption.IGNORE_CASE

@HiltViewModel
class SitePickerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val repository: SitePickerRepository,
    private val resourceProvider: ResourceProvider,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val unifiedLoginTracker: UnifiedLoginTracker,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val userEligibilityFetcher: UserEligibilityFetcher
) : ScopedViewModel(savedState) {
    companion object {
        private const val WOOCOMMERCE_INSTALLATION_URL = "https://wordpress.com/plugins/woocommerce/"
        private const val WOOCOMMERCE_INSTALLATION_DONE_URL = "marketplace/thank-you/woocommerce"
    }

    private val navArgs: SitePickerFragmentArgs by savedState.navArgs()

    val sitePickerViewStateData = LiveDataDelegate(savedState, SitePickerViewState())
    private var sitePickerViewState by sitePickerViewStateData

    private val _sites = MutableLiveData<List<SitesListItem>>()
    val sites: LiveData<List<SitesListItem>> = _sites

    private var loginSiteAddress: String?
        get() = savedState.get("key") ?: appPrefsWrapper.getLoginSiteAddress()
        set(value) = savedState.set("key", value)

    val shouldShowToolbar: Boolean
        get() = !navArgs.openedFromLogin

    init {
        when (navArgs.openedFromLogin) {
            true -> loadLoginView()
            false -> loadStorePickerView()
        }
        updateSiteViewDetails()
        loadAndDisplaySites()
    }

    private fun updateSiteViewDetails() {
        sitePickerViewState = sitePickerViewState.copy(
            userInfo = getUserInfo(),
            primaryBtnText = resourceProvider.getString(string.continue_button),
            secondaryBtnText = resourceProvider.getString(string.login_try_another_account),
            currentSitePickerState = SitePickerState.StoreListState
        )
    }

    private fun loadAndDisplaySites() {
        launch {
            val sitesInDb = getSitesFromDb()
            if (sitesInDb.isNotEmpty()) {
                displaySites(sitesInDb)
            }
            fetchSitesFromApi(sitesInDb.isEmpty())
        }
    }

    private suspend fun fetchSitesFromApi(showSkeleton: Boolean) {
        sitePickerViewState = sitePickerViewState.copy(
            isSkeletonViewVisible = showSkeleton
        )

        val startTime = System.currentTimeMillis()
        val result = repository.fetchWooCommerceSites()
        val duration = System.currentTimeMillis() - startTime

        sitePickerViewState = sitePickerViewState.copy(
            isSkeletonViewVisible = false, isProgressDiaLogVisible = false
        )
        when {
            result.isError -> triggerEvent(ShowSnackbar(string.site_picker_error))
            result.model != null -> {
                if (result.model!!.any { it.isJetpackCPConnected }) {
                    analyticsTrackerWrapper.track(
                        stat = AnalyticsEvent.JETPACK_CP_SITES_FETCHED,
                        properties = mapOf(AnalyticsTracker.KEY_FETCH_SITES_DURATION to duration)
                    )
                }
                displaySites(repository.getSites())
            }
        }
    }

    private suspend fun getSitesFromDb(): List<SiteModel> {
        val sitesInDb = repository.getSites()
        return if (!FeatureFlag.JETPACK_CP.isEnabled() ||
            sitesInDb.none { it.isJetpackCPConnected }
        ) {
            sitesInDb
        } else {
            emptyList()
        }
    }

    private fun loadLoginView() {
        appPrefsWrapper.getUnifiedLoginLastSource()?.let { unifiedLoginTracker.setSource(it) }
        trackLoginEvent(UnifiedLoginTracker.Flow.EPILOGUE, UnifiedLoginTracker.Step.START)
        sitePickerViewState = sitePickerViewState.copy(
            isHelpBtnVisible = true,
            isSecondaryBtnVisible = true,
            primaryBtnText = resourceProvider.getString(string.continue_button)
        )
    }

    private fun loadStorePickerView() {
        sitePickerViewState = sitePickerViewState.copy(
            isHelpBtnVisible = false,
            isSecondaryBtnVisible = false,
            primaryBtnText = resourceProvider.getString(string.continue_button),
            toolbarTitle = resourceProvider.getString(string.site_picker_title)
        )
    }

    private fun displaySites(sites: List<SiteModel>) {
        val filteredSites = sites.filter {
            FeatureFlag.JETPACK_CP.isEnabled() || !it.isJetpackCPConnected
        }

        if (filteredSites.isEmpty()) {
            loginSiteAddress?.let { loadAccountMismatchView(it) } ?: loadNoStoreView()
            return
        }

        val wooSites = filteredSites.filter { it.hasWooCommerce }
        val nonWooSites = filteredSites.filter { !it.hasWooCommerce }
        val selectedSite = selectedSite.getIfExists() ?: wooSites.getOrNull(0)

        if (_sites.value == null) {
            // Track events only on the first call
            trackLoginEvent(currentStep = UnifiedLoginTracker.Step.SITE_LIST)
            analyticsTrackerWrapper.track(
                AnalyticsEvent.SITE_PICKER_STORES_SHOWN,
                mapOf(
                    AnalyticsTracker.KEY_NUMBER_OF_STORES to wooSites.size,
                    AnalyticsTracker.KEY_NUMBER_OF_NON_WOO_SITES to nonWooSites.size
                )
            )
        }

        _sites.value = buildList {
            if (wooSites.isNotEmpty()) {
                add(Header(R.string.login_pick_store))
                addAll(
                    wooSites.map {
                        WooSiteUiModel(
                            site = it,
                            isSelected = selectedSite?.id == it.id
                        )
                    }
                )
            }
            if (navArgs.openedFromLogin && nonWooSites.isNotEmpty()) {
                add(Header(R.string.login_non_woo_stores_label))
                addAll(nonWooSites.map { NonWooSiteUiModel(it) })
            }
        }
        sitePickerViewState = sitePickerViewState.copy(
            hasConnectedStores = filteredSites.isNotEmpty(),
            isPrimaryBtnVisible = wooSites.isNotEmpty()
        )
        loginSiteAddress?.let { processLoginSiteAddress(it) }
    }

    /**
     * Signin M1: User logged in with a URL. Here we check that login url to see
     * if the site is (in this order):
     * - Connected to the same account the user logged in with
     * - Has WooCommerce installed
     */
    private fun processLoginSiteAddress(url: String) {
        val site = repository.getSiteBySiteUrl(url)?.takeIf {
            FeatureFlag.JETPACK_CP.isEnabled() || !it.isJetpackCPConnected
        }
        when {
            site == null -> {
                // The url doesn't match any sites for this account.
                loadAccountMismatchView(url)
            }
            !site.hasWooCommerce -> {
                // Show not woo store message view.
                loadWooNotFoundView(site)
            }
            else -> {
                // We have a pre-validation woo store. Attempt to just
                // login with this store directly.
                onSiteSelected(site)
                onContinueButtonClick(true)
            }
        }
    }

    private fun loadNoStoreView() {
        trackLoginEvent(currentStep = UnifiedLoginTracker.Step.NO_WOO_STORES)
        sitePickerViewState = sitePickerViewState.copy(
            isNoStoresViewVisible = true,
            isPrimaryBtnVisible = true,
            primaryBtnText = resourceProvider.getString(string.login_site_picker_enter_site_address),
            noStoresLabelText = resourceProvider.getString(string.login_no_stores),
            noStoresBtnText = resourceProvider.getString(string.login_site_picker_new_to_woo),
            currentSitePickerState = SitePickerState.NoStoreState
        )
    }

    /**
     * SignIn M1: The url the user submitted during login belongs
     * to a site that is not connected to the account the user logged
     * in with.
     */
    private fun loadAccountMismatchView(url: String) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_PICKER_AUTO_LOGIN_ERROR_NOT_CONNECTED_TO_USER,
            mapOf(
                AnalyticsTracker.KEY_URL to url,
                AnalyticsTracker.KEY_HAS_CONNECTED_STORES to sitePickerViewState.hasConnectedStores
            )
        )
        trackLoginEvent(currentStep = UnifiedLoginTracker.Step.WRONG_WP_ACCOUNT)
        sitePickerViewState = sitePickerViewState.copy(
            isNoStoresViewVisible = true,
            isPrimaryBtnVisible = sitePickerViewState.hasConnectedStores == true || !loginSiteAddress.isNullOrEmpty(),
            primaryBtnText = resourceProvider.getString(
                if (sitePickerViewState.hasConnectedStores == true) string.login_view_connected_stores
                else string.login_site_picker_try_another_address
            ),
            noStoresLabelText = resourceProvider.getString(string.login_not_connected_to_account, url),
            noStoresBtnText = resourceProvider.getString(string.login_need_help_finding_email),
            currentSitePickerState = SitePickerState.AccountMismatchState
        )
    }

    private fun loadWooNotFoundView(site: SiteModel) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_PICKER_AUTO_LOGIN_ERROR_NOT_WOO_STORE,
            mapOf(
                AnalyticsTracker.KEY_URL to site.url,
                AnalyticsTracker.KEY_HAS_CONNECTED_STORES to sitePickerViewState.hasConnectedStores
            )
        )
        trackLoginEvent(currentStep = UnifiedLoginTracker.Step.NOT_WOO_STORE)
        // Make sure installation is enabled only for selfhosted and atomic sites
        // TODO remove this when we handle non-atomic sites
        val isWooInstallationEnabled = site.isJetpackConnected
        sitePickerViewState = sitePickerViewState.copy(
            isNoStoresViewVisible = true,
            isPrimaryBtnVisible = isWooInstallationEnabled,
            primaryBtnText = resourceProvider.getString(string.login_install_woo),
            noStoresLabelText = resourceProvider.getString(string.login_not_woo_store, site.url),
            noStoresBtnText = resourceProvider.getString(string.login_view_connected_stores),
            currentSitePickerState = SitePickerState.WooNotFoundState
        )
    }

    private fun getUserInfo() = repository.getUserAccount().let {
        UserInfo(displayName = it.displayName, username = it.userName ?: "", userAvatarUrl = it.avatarUrl)
    }

    fun onSiteSelected(siteModel: SiteModel) {
        val updatedSites = _sites.value?.map {
            when (it) {
                is WooSiteUiModel -> it.copy(isSelected = it.site.id == siteModel.id)
                else -> it
            }
        }
        updatedSites?.let { _sites.value = it }
    }

    fun onNonWooSiteSelected(siteModel: SiteModel) {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.SITE_PICKER_NON_WOO_SITE_TAPPED,
            properties = mapOf(
                AnalyticsTracker.KEY_IS_NON_ATOMIC to (!siteModel.isJetpackConnected && !siteModel.isJetpackCPConnected)
            )
        )
        // Strip protocol from site's URL
        val protocolRegex = Regex("^(http[s]?://)", IGNORE_CASE)
        val cleanedUrl = siteModel.url.replaceFirst(protocolRegex, "")

        loginSiteAddress = cleanedUrl
        loadWooNotFoundView(siteModel)
    }

    fun onViewConnectedStoresButtonClick() {
        analyticsTrackerWrapper.track(AnalyticsEvent.SITE_PICKER_VIEW_CONNECTED_STORES_BUTTON_TAPPED)
        trackLoginEvent(clickEvent = UnifiedLoginTracker.Click.VIEW_CONNECTED_STORES)
        sitePickerViewState = sitePickerViewState.copy(
            isNoStoresViewVisible = false,
            isPrimaryBtnVisible = sites.value!!.any { it is WooSiteUiModel },
            primaryBtnText = resourceProvider.getString(string.continue_button),
            currentSitePickerState = SitePickerState.StoreListState
        )
    }

    fun onNeedHelpFindingEmailButtonClick() {
        analyticsTrackerWrapper.track(AnalyticsEvent.SITE_PICKER_HELP_FINDING_CONNECTED_EMAIL_LINK_TAPPED)
        trackLoginEvent(clickEvent = UnifiedLoginTracker.Click.HELP_FINDING_CONNECTED_EMAIL)
        triggerEvent(SitePickerEvent.NavigateToEmailHelpDialogEvent)
    }

    fun onRefreshButtonClick() {
        analyticsTrackerWrapper.track(AnalyticsEvent.SITE_PICKER_NOT_CONNECTED_JETPACK_REFRESH_APP_LINK_TAPPED)
        sitePickerViewState = sitePickerViewState.copy(isProgressDiaLogVisible = true)
        launch { fetchSitesFromApi(showSkeleton = false) }
    }

    fun onNewToWooClick() {
        analyticsTrackerWrapper.track(AnalyticsEvent.SITE_PICKER_NEW_TO_WOO_TAPPED)
        triggerEvent(SitePickerEvent.NavigateToNewToWooEvent)
    }

    fun onEnterSiteAddressClick() {
        analyticsTrackerWrapper.track(AnalyticsEvent.SITE_PICKER_ENTER_SITE_ADDRESS_TAPPED)
        triggerEvent(SitePickerEvent.NavigateToSiteAddressEvent)
    }

    fun onTryAnotherAccountButtonClick() {
        trackLoginEvent(clickEvent = UnifiedLoginTracker.Click.TRY_ANOTHER_ACCOUNT)
        launch {
            repository.logout().let {
                if (!repository.isUserLoggedIn()) {
                    appPrefsWrapper.removeLoginSiteAddress()
                    triggerEvent(Logout)
                }
            }
        }
    }

    fun onHelpButtonClick() {
        analyticsTrackerWrapper.track(AnalyticsEvent.SITE_PICKER_HELP_BUTTON_TAPPED)
        trackLoginEvent(clickEvent = UnifiedLoginTracker.Click.SHOW_HELP)
        triggerEvent(SitePickerEvent.NavigationToHelpFragmentEvent)
    }

    fun onContinueButtonClick(isAutoLogin: Boolean = false) {
        _sites.value?.first { (it is WooSiteUiModel) && it.isSelected }
            ?.let { it as WooSiteUiModel }
            ?.let {
                // the current site is selected again so do nothing
                if (it.site.id == selectedSite.getIfExists()?.id) {
                    triggerEvent(Exit)
                    return
                }
                val eventProperties = mapOf(
                    AnalyticsTracker.KEY_SELECTED_STORE_ID to it.site.id,
                    AnalyticsTracker.KEY_IS_JETPACK_CP_CONNECTED to it.site.isJetpackCPConnected,
                    AnalyticsTracker.KEY_ACTIVE_JETPACK_CONNECTION_PLUGINS
                        to it.site.activeJetpackConnectionPlugins.orEmpty()
                )
                if (isAutoLogin) {
                    analyticsTrackerWrapper.track(
                        AnalyticsEvent.SITE_PICKER_AUTO_LOGIN_SUBMITTED,
                        eventProperties
                    )
                } else {
                    analyticsTrackerWrapper.track(
                        AnalyticsEvent.SITE_PICKER_CONTINUE_TAPPED,
                        eventProperties
                    )
                }

                sitePickerViewState = sitePickerViewState.copy(isProgressDiaLogVisible = true)
                launch {
                    val siteVerificationResult = repository.verifySiteWooAPIVersion(it.site)
                    when {
                        siteVerificationResult.isError -> {
                            sitePickerViewState = sitePickerViewState.copy(isProgressDiaLogVisible = false)
                            val event = when (siteVerificationResult.error.type) {
                                WooErrorType.TIMEOUT -> SitePickerEvent.JetpackTimeoutError
                                else -> ShowSnackbar(
                                    message = string.login_verifying_site_error,
                                    args = arrayOf(it.site.getSiteName())
                                )
                            }
                            triggerEvent(event)
                        }
                        siteVerificationResult.model?.apiVersion == WooCommerceStore.WOO_API_NAMESPACE_V3 -> {
                            selectedSite.set(it.site)
                            userEligibilityFetcher.fetchUserInfo()?.let { userModel ->
                                sitePickerViewState = sitePickerViewState.copy(isProgressDiaLogVisible = false)
                                userEligibilityFetcher.updateUserInfo(userModel)

                                trackLoginEvent(currentStep = UnifiedLoginTracker.Step.SUCCESS)
                                appPrefsWrapper.removeLoginSiteAddress()
                                triggerEvent(SitePickerEvent.NavigateToMainActivityEvent)
                            }
                        }
                        else -> {
                            sitePickerViewState = sitePickerViewState.copy(isProgressDiaLogVisible = false)
                            triggerEvent(SitePickerEvent.ShowWooUpgradeDialogEvent)
                        }
                    }
                }
            }
    }

    fun onInstallWooClicked() {
        loginSiteAddress?.let {
            triggerEvent(
                NavigateToWPComWebView(
                    url = "$WOOCOMMERCE_INSTALLATION_URL$it",
                    validationUrl = WOOCOMMERCE_INSTALLATION_DONE_URL
                )
            )
        }
    }

    fun onWooInstalled() {
        suspend fun fetchSite(site: SiteModel, retries: Int = 0): Result<SiteModel> {
            delay(retries * TimeUnit.SECONDS.toMillis(2))
            val result = repository.fetchWooCommerceSite(site)

            val maxNumberOfRetries = 2
            if (retries == maxNumberOfRetries) return result

            val updatedSite = result.getOrNull()
            return when {
                updatedSite == null -> {
                    WooLog.w(
                        WooLog.T.SITE_PICKER,
                        "Fetching site failed after Woo installation, error: ${result.exceptionOrNull()}"
                    )
                    fetchSite(site, retries = retries + 1)
                }
                !updatedSite.hasWooCommerce -> {
                    // Force a retry if the woocommerce_is_active is not updated yet
                    WooLog.d(WooLog.T.SITE_PICKER, "Fetched site has woocommerce_is_active false, retry")
                    fetchSite(site, retries = retries + 1)
                }
                else -> {
                    WooLog.d(WooLog.T.SITE_PICKER, "Site fetched successfully")
                    result
                }
            }
        }
        launch {
            val site = loginSiteAddress?.let { repository.getSiteBySiteUrl(it) } ?: return@launch

            sitePickerViewState = sitePickerViewState.copy(
                isSkeletonViewVisible = true,
                isPrimaryBtnVisible = false,
                isSecondaryBtnVisible = true,
                isNoStoresViewVisible = false
            )

            WooLog.d(WooLog.T.SITE_PICKER, "Woo is installed, fetch the site ${site.siteId}")
            // Fetch site
            val result = fetchSite(site)
            sitePickerViewState = sitePickerViewState.copy(isSkeletonViewVisible = false)

            result.fold(
                onSuccess = {
                    // Continue login
                    displaySites(repository.getSites())
                },
                onFailure = {
                    triggerEvent(ShowSnackbar(string.site_picker_error))
                    // This would lead to the [WooNotFoundState] again
                    // The chance of getting this state is small, because of the retry mechanism above
                    displaySites(repository.getSites())
                }
            )
        }
    }

    fun onSiteAddressReceived(siteAddress: String) {
        loginSiteAddress = siteAddress
        launch { fetchSitesFromApi(showSkeleton = true) }
    }

    private fun trackLoginEvent(
        currentFlow: UnifiedLoginTracker.Flow? = null,
        currentStep: UnifiedLoginTracker.Step? = null,
        clickEvent: UnifiedLoginTracker.Click? = null
    ) {
        if (navArgs.openedFromLogin) {
            if (currentFlow != null && currentStep != null) {
                unifiedLoginTracker.track(currentFlow, currentStep)
            } else if (currentStep != null) {
                unifiedLoginTracker.track(step = currentStep)
            }
            if (clickEvent != null) {
                unifiedLoginTracker.trackClick(clickEvent)
            }
        }
    }

    @Parcelize
    data class SitePickerViewState(
        val userInfo: UserInfo? = null,
        val toolbarTitle: String? = null,
        val hasConnectedStores: Boolean? = null,
        val primaryBtnText: String? = null,
        val secondaryBtnText: String? = null,
        val isNoStoresViewVisible: Boolean = false,
        val noStoresLabelText: String? = null,
        val noStoresBtnText: String? = null,
        val isHelpBtnVisible: Boolean = false,
        val isSkeletonViewVisible: Boolean = false,
        val isProgressDiaLogVisible: Boolean = false,
        val isPrimaryBtnVisible: Boolean = false,
        val isSecondaryBtnVisible: Boolean = false,
        val currentSitePickerState: SitePickerState = SitePickerState.StoreListState
    ) : Parcelable

    @Parcelize
    data class UserInfo(val displayName: String, val username: String, val userAvatarUrl: String) : Parcelable

    sealed interface SitesListItem : Parcelable {
        @Parcelize
        data class Header(@StringRes val label: Int) : SitesListItem

        @Parcelize
        data class WooSiteUiModel(
            val site: SiteModel,
            val isSelected: Boolean
        ) : SitesListItem

        @Parcelize
        data class NonWooSiteUiModel(
            val site: SiteModel
        ) : SitesListItem
    }

    sealed class SitePickerEvent : MultiLiveEvent.Event() {
        object ShowWooUpgradeDialogEvent : SitePickerEvent()
        object NavigateToMainActivityEvent : SitePickerEvent()
        object NavigateToEmailHelpDialogEvent : SitePickerEvent()
        object NavigationToHelpFragmentEvent : SitePickerEvent()
        object NavigateToNewToWooEvent : SitePickerEvent()
        object NavigateToSiteAddressEvent : SitePickerEvent()
        object JetpackTimeoutError : SitePickerEvent()
        data class NavigateToWPComWebView(val url: String, val validationUrl: String) : SitePickerEvent()
    }

    enum class SitePickerState {
        StoreListState, NoStoreState, AccountMismatchState, WooNotFoundState
    }
}
