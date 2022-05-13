package com.woocommerce.android.ui.sitepicker

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.getSiteName
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Logout
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

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
    private val navArgs: SitePickerFragmentArgs by savedState.navArgs()

    val sitePickerViewStateData = LiveDataDelegate(savedState, SitePickerViewState())
    private var sitePickerViewState by sitePickerViewStateData

    private val _sites = MutableLiveData<List<SiteUiModel>>()
    val sites: LiveData<List<SiteUiModel>> = _sites

    private val loginSiteAddress = appPrefsWrapper.getLoginSiteAddress()

    init {
        when (navArgs.openedFromLogin) {
            true -> loadLoginView()
            false -> loadStorePickerView()
        }
        updateSiteViewDetails()
        loadAndDisplaySites()
    }

    override fun onCleared() {
        super.onCleared()
        repository.onCleanup()
    }

    private fun updateSiteViewDetails() {
        sitePickerViewState = sitePickerViewState.copy(
            userInfo = getUserInfo(),
            sitePickerLabelText = resourceProvider.getString(string.site_picker_label),
            primaryBtnText = resourceProvider.getString(string.continue_button),
            secondaryBtnText = resourceProvider.getString(string.login_try_another_account),
            currentSitePickerState = SitePickerState.StoreListState
        )
    }

    private fun loadAndDisplaySites() {
        val sitesInDb = getSitesFromDb()
        if (sitesInDb.isNotEmpty()) {
            displaySites(sitesInDb)
        }
        launch { fetchSitesFromApi(sitesInDb.isEmpty()) }
    }

    private suspend fun fetchSitesFromApi(showSkeleton: Boolean) {
        sitePickerViewState = sitePickerViewState.copy(
            isSkeletonViewVisible = showSkeleton, isPrimaryBtnVisible = !showSkeleton
        )

        val startTime = System.currentTimeMillis()
        val result = repository.fetchWooCommerceSites()
        val duration = System.currentTimeMillis() - startTime

        sitePickerViewState = sitePickerViewState.copy(
            isSkeletonViewVisible = false, isPrimaryBtnVisible = true, isProgressDiaLogVisible = false
        )
        when {
            result.isError -> triggerEvent(ShowSnackbar(string.site_picker_error))
            result.model?.isEmpty() == true -> {
                loginSiteAddress?.let { loadAccountMismatchView(it) } ?: loadNoStoreView()
            }
            result.model != null -> loadStoreListView(result.model!!, duration)
        }
    }

    private fun getSitesFromDb(): List<SiteModel> {
        val sitesInDb = repository.getWooCommerceSites()
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
            isToolbarVisible = false,
            isHelpBtnVisible = true,
            isSecondaryBtnVisible = true,
            primaryBtnText = resourceProvider.getString(string.continue_button)
        )
    }

    private fun loadStorePickerView() {
        sitePickerViewState = sitePickerViewState.copy(
            isToolbarVisible = true,
            isHelpBtnVisible = false,
            isSecondaryBtnVisible = false,
            primaryBtnText = resourceProvider.getString(string.continue_button),
            toolbarTitle = resourceProvider.getString(string.site_picker_title)
        )
    }

    private fun loadStoreListView(sites: List<SiteModel>, duration: Long) {
        if (sites.any { it.isJetpackCPConnected }) {
            analyticsTrackerWrapper.track(
                stat = AnalyticsEvent.JETPACK_CP_SITES_FETCHED,
                properties = mapOf(AnalyticsTracker.KEY_FETCH_SITES_DURATION to duration)
            )
        }
        trackLoginEvent(currentStep = UnifiedLoginTracker.Step.SITE_LIST)
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_PICKER_STORES_SHOWN,
            mapOf(AnalyticsTracker.KEY_NUMBER_OF_STORES to sites.size)
        )

        val filteredSites = sites.filter {
            FeatureFlag.JETPACK_CP.isEnabled() || !it.isJetpackCPConnected
        }
        displaySites(filteredSites)
    }

    private fun displaySites(sites: List<SiteModel>) {
        sitePickerViewState = sitePickerViewState.copy(hasConnectedStores = sites.isNotEmpty())
        val selectedSite = selectedSite.getIfExists() ?: sites[0]
        _sites.value = sites.map {
            SiteUiModel(
                site = it,
                isSelected = selectedSite.id == it.id
            )
        }
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
                loadWooNotFoundView(url)
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
            primaryBtnText = resourceProvider.getString(string.login_jetpack_view_instructions_alt),
            noStoresLabelText = resourceProvider.getString(string.login_no_stores),
            noStoresBtnText = resourceProvider.getString(string.login_jetpack_what_is),
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
            isPrimaryBtnVisible = sitePickerViewState.hasConnectedStores == true,
            primaryBtnText = resourceProvider.getString(string.login_view_connected_stores),
            noStoresLabelText = resourceProvider.getString(string.login_not_connected_to_account, url),
            noStoresBtnText = resourceProvider.getString(string.login_need_help_finding_email),
            currentSitePickerState = SitePickerState.AccountMismatchState
        )
    }

    private fun loadWooNotFoundView(url: String) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_PICKER_AUTO_LOGIN_ERROR_NOT_WOO_STORE,
            mapOf(
                AnalyticsTracker.KEY_URL to url,
                AnalyticsTracker.KEY_HAS_CONNECTED_STORES to sitePickerViewState.hasConnectedStores
            )
        )
        trackLoginEvent(currentStep = UnifiedLoginTracker.Step.NOT_WOO_STORE)
        sitePickerViewState = sitePickerViewState.copy(
            isNoStoresViewVisible = true,
            isPrimaryBtnVisible = sitePickerViewState.hasConnectedStores == true,
            primaryBtnText = resourceProvider.getString(string.login_view_connected_stores),
            noStoresLabelText = resourceProvider.getString(string.login_not_woo_store, url),
            noStoresBtnText = resourceProvider.getString(string.login_refresh_app),
            currentSitePickerState = SitePickerState.WooNotFoundState
        )
    }

    private fun getUserInfo() = repository.getUserAccount().let {
        UserInfo(displayName = it.displayName, username = it.userName ?: "", userAvatarUrl = it.avatarUrl)
    }

    fun onSiteSelected(siteModel: SiteModel) {
        val updatedSites = _sites.value?.map {
            it.copy(isSelected = it.site.id == siteModel.id)
        }
        updatedSites?.let { _sites.value = it }
    }

    fun onViewConnectedStoresButtonClick() {
        analyticsTrackerWrapper.track(AnalyticsEvent.SITE_PICKER_VIEW_CONNECTED_STORES_BUTTON_TAPPED)
        trackLoginEvent(clickEvent = UnifiedLoginTracker.Click.VIEW_CONNECTED_STORES)
        sitePickerViewState = sitePickerViewState.copy(
            isNoStoresViewVisible = false,
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

    fun onWhatIsJetpackButtonClick() {
        analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_REQUIRED_WHAT_IS_JETPACK_LINK_TAPPED)
        triggerEvent(SitePickerEvent.NavigationToWhatIsJetpackFragmentEvent)
    }

    fun onLearnMoreAboutJetpackButtonClick() {
        analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_REQUIRED_VIEW_INSTRUCTIONS_BUTTON_TAPPED)
        triggerEvent(SitePickerEvent.NavigationToLearnMoreAboutJetpackEvent)
    }

    fun onTryAnotherAccountButtonClick() {
        trackLoginEvent(clickEvent = UnifiedLoginTracker.Click.TRY_ANOTHER_ACCOUNT)
        launch {
            repository.logout()?.let {
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
        _sites.value?.first { it.isSelected }
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
                            triggerEvent(
                                ShowSnackbar(
                                    message = string.login_verifying_site_error,
                                    args = arrayOf(it.site.getSiteName())
                                )
                            )
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
        val sitePickerLabelText: String? = null,
        val primaryBtnText: String? = null,
        val secondaryBtnText: String? = null,
        val isNoStoresViewVisible: Boolean = false,
        val noStoresLabelText: String? = null,
        val noStoresBtnText: String? = null,
        val isHelpBtnVisible: Boolean = false,
        val isSkeletonViewVisible: Boolean = false,
        val isToolbarVisible: Boolean = false,
        val isProgressDiaLogVisible: Boolean = false,
        val isPrimaryBtnVisible: Boolean = false,
        val isSecondaryBtnVisible: Boolean = false,
        val currentSitePickerState: SitePickerState = SitePickerState.StoreListState
    ) : Parcelable

    @Parcelize
    data class UserInfo(val displayName: String, val username: String, val userAvatarUrl: String) : Parcelable

    @Parcelize
    data class SiteUiModel(
        val site: SiteModel,
        val isSelected: Boolean
    ) : Parcelable

    sealed class SitePickerEvent : MultiLiveEvent.Event() {
        object ShowWooUpgradeDialogEvent : SitePickerEvent()
        object NavigateToMainActivityEvent : SitePickerEvent()
        object NavigateToEmailHelpDialogEvent : SitePickerEvent()
        object NavigationToHelpFragmentEvent : SitePickerEvent()
        object NavigationToWhatIsJetpackFragmentEvent : SitePickerEvent()
        object NavigationToLearnMoreAboutJetpackEvent : SitePickerEvent()
    }

    enum class SitePickerState {
        StoreListState, NoStoreState, AccountMismatchState, WooNotFoundState
    }
}
