package com.woocommerce.android.ui.sitepicker

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigateToEmailHelpDialogEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigateToMainActivityEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigationToHelpFragmentEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigationToLearnMoreAboutJetpackEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigationToWhatIsJetpackFragmentEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.ShowWooUpgradeDialogEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerState.AccountMismatchState
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerState.NoStoreState
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerState.StoreListState
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerState.WooNotFoundState
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Logout
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

@ExperimentalCoroutinesApi
class SitePickerViewModelTest : BaseUnitTest() {
    private val resourceProvider: ResourceProvider = mock()
    private val selectedSite: SelectedSite = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val userEligibilityFetcher: UserEligibilityFetcher = mock()
    private val repository: SitePickerRepository = mock()
    private val unifiedLoginTracker: UnifiedLoginTracker = mock()

    private lateinit var viewModel: SitePickerViewModel
    private lateinit var savedState: SavedStateHandle

    private fun whenViewModelIsCreated() {
        viewModel = SitePickerViewModel(
            savedState = savedState,
            selectedSite = selectedSite,
            repository = repository,
            resourceProvider = resourceProvider,
            appPrefsWrapper = appPrefsWrapper,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            userEligibilityFetcher = userEligibilityFetcher,
            unifiedLoginTracker = unifiedLoginTracker
        )
    }

    private fun givenTheScreenIsFromLogin(calledFromLogin: Boolean) {
        savedState = SitePickerFragmentArgs(openedFromLogin = calledFromLogin).initSavedStateHandle()
    }

    private fun givenThatUserLoggedInFromEnteringSiteAddress(expectedSite: SiteModel? = null) {
        whenever(appPrefsWrapper.getLoginSiteAddress()).thenReturn(SitePickerTestUtils.loginSiteAddress)
        whenever(repository.getSiteBySiteUrl(any())).thenReturn(expectedSite)
    }

    private suspend fun givenThatSiteVerificationIsCompleted() {
        whenever(repository.verifySiteWooAPIVersion(any())).thenReturn(
            WooResult(SitePickerTestUtils.apiVerificationResponse)
        )
        whenever(userEligibilityFetcher.fetchUserInfo()).thenReturn(SitePickerTestUtils.userModel)
    }

    private suspend fun whenSitesAreFetched(returnsError: Boolean = false, returnsEmpty: Boolean = false) {
        when {
            returnsEmpty -> {
                whenever(repository.fetchWooCommerceSites()).thenReturn(WooResult(mutableListOf()))
                whenever(repository.getWooCommerceSites()).thenReturn(mutableListOf())
            }
            returnsError -> {
                whenever(repository.fetchWooCommerceSites()).thenReturn(
                    WooResult(
                        WooError(
                            type = WooErrorType.GENERIC_ERROR,
                            message = "",
                            original = BaseRequest.GenericErrorType.UNKNOWN
                        )
                    )
                )
                whenever(repository.getWooCommerceSites()).thenReturn(mutableListOf())
            }
            else -> {
                whenever(repository.fetchWooCommerceSites()).thenReturn(WooResult(expectedSiteList))
            }
        }
    }

    private val expectedSiteList = SitePickerTestUtils.generateStores()

    private val defaultSitePickerViewState = SitePickerViewModel.SitePickerViewState(
        userInfo = SitePickerTestUtils.userInfo,
        sitePickerLabelText = resourceProvider.getString(R.string.site_picker_label),
        primaryBtnText = resourceProvider.getString(R.string.continue_button),
        secondaryBtnText = resourceProvider.getString(R.string.login_try_another_account),
        hasConnectedStores = expectedSiteList.isNotEmpty()
    )

    @Before
    fun setup() {
        whenever(repository.getUserAccount()).thenReturn(SitePickerTestUtils.account)
        whenever(repository.getWooCommerceSites()).thenReturn(expectedSiteList.toMutableList())
        givenTheScreenIsFromLogin(true)
    }

    @Test
    fun `given that user is logging in, the toolbar, help and secondary button is displayed`() = testBlocking {
        givenTheScreenIsFromLogin(true)
        whenSitesAreFetched()
        whenViewModelIsCreated()

        var sitePickerData: SitePickerViewModel.SitePickerViewState? = null
        viewModel.sitePickerViewStateData.observeForever { _, new -> sitePickerData = new }

        assertThat(sitePickerData).isEqualTo(SitePickerTestUtils.getDefaultLoginViewState(defaultSitePickerViewState))
    }

    @Test
    fun `given that user is switching stores, the toolbar, help and secondary button is hidden`() = testBlocking {
        givenTheScreenIsFromLogin(false)
        whenSitesAreFetched()
        whenViewModelIsCreated()

        var sitePickerData: SitePickerViewModel.SitePickerViewState? = null
        viewModel.sitePickerViewStateData.observeForever { _, new -> sitePickerData = new }

        assertThat(sitePickerData).isEqualTo(
            SitePickerTestUtils.getDefaultSwitchStoreViewState(defaultSitePickerViewState)
        )
    }

    @Test
    fun `given that the view model is created, when stores fetch succeeds, then stores are displayed correctly`() =
        testBlocking {
            whenSitesAreFetched()
            whenViewModelIsCreated()

            var sites: List<SitePickerViewModel.SiteUiModel>? = null
            viewModel.sites.observeForever { sites = it }

            verify(repository, times(1)).fetchWooCommerceSites()
            verify(repository, times(1)).getWooCommerceSites()
            verify(analyticsTrackerWrapper, times(1)).track(
                AnalyticsEvent.SITE_PICKER_STORES_SHOWN,
                mapOf(AnalyticsTracker.KEY_NUMBER_OF_STORES to sites?.size)
            )

            assertThat(sites).isNotEmpty
            assertThat(sites?.get(0)?.isSelected).isTrue()
        }

    @Test
    fun `given that the view model is created, when store fetch is in error, then error is displayed`() = testBlocking {
        whenSitesAreFetched(returnsError = true)
        whenViewModelIsCreated()

        var sites: List<SitePickerViewModel.SiteUiModel>? = null
        viewModel.sites.observeForever { sites = it }

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        verify(repository, times(1)).fetchWooCommerceSites()
        verify(repository, times(1)).getWooCommerceSites()

        assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.site_picker_error))
        assertThat(sites).isNull()
    }

    @Test
    fun `given that the view model is created, when store fetch is empty, then empty view is displayed`() =
        testBlocking {
            val expectedSitePickerViewState = SitePickerTestUtils.getEmptyViewState(
                defaultSitePickerViewState, resourceProvider
            )
            whenSitesAreFetched(returnsEmpty = true)
            whenViewModelIsCreated()

            var sitePickerData: SitePickerViewModel.SitePickerViewState? = null
            viewModel.sitePickerViewStateData.observeForever { _, new -> sitePickerData = new }

            var sites: List<SitePickerViewModel.SiteUiModel>? = null
            viewModel.sites.observeForever { sites = it }

            assertThat(sitePickerData?.isNoStoresViewVisible).isEqualTo(
                expectedSitePickerViewState.isNoStoresViewVisible
            )
            assertThat(sitePickerData?.primaryBtnText).isEqualTo(expectedSitePickerViewState.primaryBtnText)
            assertThat(sitePickerData?.noStoresLabelText).isEqualTo(expectedSitePickerViewState.noStoresLabelText)
            assertThat(sitePickerData?.noStoresBtnText).isEqualTo(expectedSitePickerViewState.noStoresBtnText)
            assertThat(sitePickerData?.currentSitePickerState).isEqualTo(NoStoreState)
            assertThat(sites).isNull()
        }

    @Test
    fun `given that stores in db are empty, when stores are fetched from api, then skeleton view is displayed`() =
        testBlocking {
            whenSitesAreFetched(returnsEmpty = true)
            whenViewModelIsCreated()

            val isSkeletonShown = ArrayList<Boolean>()
            viewModel.sitePickerViewStateData.observeForever { old, new ->
                new.isSkeletonViewVisible.takeIfNotEqualTo(old?.isSkeletonViewVisible) { isSkeletonShown.add(it) }
            }

            assertThat(isSkeletonShown).containsExactly(false)
        }

    @Test
    fun `given that user is logging in from site address, auto login should be initiated`() = testBlocking {
        givenThatUserLoggedInFromEnteringSiteAddress(expectedSiteList[1])
        givenThatSiteVerificationIsCompleted()
        whenSitesAreFetched()
        whenViewModelIsCreated()

        var sites: List<SitePickerViewModel.SiteUiModel>? = null
        viewModel.sites.observeForever { sites = it }

        verify(appPrefsWrapper, atLeastOnce()).getLoginSiteAddress()
        verify(repository, atLeastOnce()).getSiteBySiteUrl(any())
        assertThat(sites?.get(1)?.isSelected).isTrue
        assertThat(sites?.get(1)?.site?.url).isEqualTo(SitePickerTestUtils.loginSiteAddress)
    }

    @Test
    fun `given the site address entered during login does not match the user account, account error is displayed`() =
        testBlocking {
            givenThatUserLoggedInFromEnteringSiteAddress(null)
            whenSitesAreFetched()
            whenViewModelIsCreated()

            val url = SitePickerTestUtils.loginSiteAddress
            var sitePickerData: SitePickerViewModel.SitePickerViewState? = null
            viewModel.sitePickerViewStateData.observeForever { _, new -> sitePickerData = new }

            verify(repository, atLeastOnce()).getSiteBySiteUrl(any())
            verify(analyticsTrackerWrapper, atLeastOnce()).track(
                AnalyticsEvent.SITE_PICKER_AUTO_LOGIN_ERROR_NOT_CONNECTED_TO_USER,
                mapOf(
                    AnalyticsTracker.KEY_URL to url,
                    AnalyticsTracker.KEY_HAS_CONNECTED_STORES to true
                )
            )

            assertThat(sitePickerData?.isNoStoresViewVisible).isEqualTo(true)
            assertThat(sitePickerData?.isPrimaryBtnVisible).isEqualTo(true)
            assertThat(sitePickerData?.primaryBtnText).isEqualTo(
                resourceProvider.getString(R.string.login_view_connected_stores)
            )
            assertThat(sitePickerData?.noStoresLabelText).isEqualTo(
                resourceProvider.getString(R.string.login_not_connected_to_account, url)
            )
            assertThat(sitePickerData?.noStoresBtnText).isEqualTo(
                resourceProvider.getString(R.string.login_need_help_finding_email)
            )
            assertThat(sitePickerData?.currentSitePickerState).isEqualTo(AccountMismatchState)
        }

    @Test
    fun `given that the site address entered during login does not have Woo, no woo error screen is displayed`() =
        testBlocking {
            givenThatUserLoggedInFromEnteringSiteAddress(expectedSiteList[1].apply { hasWooCommerce = false })
            whenSitesAreFetched()
            whenViewModelIsCreated()

            val url = SitePickerTestUtils.loginSiteAddress
            var sitePickerData: SitePickerViewModel.SitePickerViewState? = null
            viewModel.sitePickerViewStateData.observeForever { _, new -> sitePickerData = new }

            verify(repository, atLeastOnce()).getSiteBySiteUrl(any())
            verify(analyticsTrackerWrapper, atLeastOnce()).track(
                AnalyticsEvent.SITE_PICKER_AUTO_LOGIN_ERROR_NOT_WOO_STORE,
                mapOf(
                    AnalyticsTracker.KEY_URL to url,
                    AnalyticsTracker.KEY_HAS_CONNECTED_STORES to true
                )
            )

            assertThat(sitePickerData?.isNoStoresViewVisible).isEqualTo(true)
            assertThat(sitePickerData?.isPrimaryBtnVisible).isEqualTo(true)
            assertThat(sitePickerData?.primaryBtnText).isEqualTo(
                resourceProvider.getString(R.string.login_view_connected_stores)
            )
            assertThat(sitePickerData?.noStoresLabelText).isEqualTo(
                resourceProvider.getString(R.string.login_not_woo_store, url)
            )
            assertThat(sitePickerData?.noStoresBtnText).isEqualTo(
                resourceProvider.getString(R.string.login_need_help_finding_email)
            )
            assertThat(sitePickerData?.currentSitePickerState).isEqualTo(WooNotFoundState)
        }

    @Test
    fun `given that a site is selected, when verification is initiated, then is successful`() = testBlocking {
        givenThatSiteVerificationIsCompleted()
        whenSitesAreFetched()
        whenViewModelIsCreated()

        val isProgressShown = ArrayList<Boolean>()
        viewModel.sitePickerViewStateData.observeForever { old, new ->
            new.isProgressDiaLogVisible.takeIfNotEqualTo(old?.isProgressDiaLogVisible) { isProgressShown.add(it) }
        }

        var view: NavigateToMainActivityEvent? = null
        viewModel.event.observeForever {
            if (it is NavigateToMainActivityEvent) view = it
        }

        val selectedSiteModel = expectedSiteList[1]

        viewModel.onSiteSelected(selectedSiteModel)
        viewModel.onContinueButtonClick()

        verify(repository, times(1)).verifySiteWooAPIVersion(any())
        verify(selectedSite, times(1)).set(any())
        verify(userEligibilityFetcher, times(1)).fetchUserInfo()
        verify(userEligibilityFetcher, times(1)).updateUserInfo(any())
        verify(appPrefsWrapper, times(1)).removeLoginSiteAddress()

        assertThat(view).isEqualTo(NavigateToMainActivityEvent)
        assertThat(isProgressShown).containsExactly(false, true, false)
    }

    @Test
    fun `given that a site is selected, when verification is initiated, but then returns upgrade error`() =
        testBlocking {
            whenever(repository.verifySiteWooAPIVersion(any())).thenReturn(
                WooResult(SitePickerTestUtils.errorApiVerificationResponse)
            )
            whenSitesAreFetched()
            whenViewModelIsCreated()

            val isProgressShown = ArrayList<Boolean>()
            viewModel.sitePickerViewStateData.observeForever { old, new ->
                new.isProgressDiaLogVisible.takeIfNotEqualTo(old?.isProgressDiaLogVisible) { isProgressShown.add(it) }
            }

            var view: ShowWooUpgradeDialogEvent? = null
            viewModel.event.observeForever {
                if (it is ShowWooUpgradeDialogEvent) view = it
            }

            val selectedSiteModel = expectedSiteList[1]

            viewModel.onSiteSelected(selectedSiteModel)
            viewModel.onContinueButtonClick()

            verify(repository, times(1)).verifySiteWooAPIVersion(any())
            verify(selectedSite, times(0)).set(any())
            verify(userEligibilityFetcher, times(0)).fetchUserInfo()
            verify(userEligibilityFetcher, times(0)).updateUserInfo(any())
            verify(appPrefsWrapper, times(0)).removeLoginSiteAddress()

            assertThat(view).isEqualTo(ShowWooUpgradeDialogEvent)
            assertThat(isProgressShown).containsExactly(false, true, false)
        }

    @Test
    fun `given user is logging in, then when help button is clicked, help screen is displayed`() = testBlocking {
        givenTheScreenIsFromLogin(true)
        whenViewModelIsCreated()

        var view: NavigationToHelpFragmentEvent? = null
        viewModel.event.observeForever {
            if (it is NavigationToHelpFragmentEvent) view = it
        }

        viewModel.onHelpButtonClick()

        verify(analyticsTrackerWrapper, times(1)).track(AnalyticsEvent.SITE_PICKER_HELP_BUTTON_TAPPED)
        assertThat(view).isEqualTo(NavigationToHelpFragmentEvent)
    }

    @Test
    fun `given user is logging in, then when try another account is clicked, logout is initiated`() = testBlocking {
        whenever(repository.logout()).thenReturn(true)
        whenever(repository.isUserLoggedIn()).thenReturn(false)
        givenTheScreenIsFromLogin(true)
        whenViewModelIsCreated()

        var view: Logout? = null
        viewModel.event.observeForever {
            if (it is Logout) view = it
        }

        viewModel.onTryAnotherAccountButtonClick()

        verify(repository, times(1)).logout()
        verify(repository, times(1)).isUserLoggedIn()
        assertThat(view).isEqualTo(Logout)
    }

    @Test
    fun `given user is logging in, when learn more about Jetpack is clicked, learn more is displayed`() = testBlocking {
        givenTheScreenIsFromLogin(true)
        whenViewModelIsCreated()

        var view: NavigationToLearnMoreAboutJetpackEvent? = null
        viewModel.event.observeForever {
            if (it is NavigationToLearnMoreAboutJetpackEvent) view = it
        }

        viewModel.onLearnMoreAboutJetpackButtonClick()

        verify(analyticsTrackerWrapper, times(1)).track(
            AnalyticsEvent.LOGIN_JETPACK_REQUIRED_VIEW_INSTRUCTIONS_BUTTON_TAPPED
        )
        assertThat(view).isEqualTo(NavigationToLearnMoreAboutJetpackEvent)
    }

    @Test
    fun `given user is logging in, when refresh button is clicked, refresh the screen`() =
        testBlocking {
            givenTheScreenIsFromLogin(true)
            whenSitesAreFetched()
            whenViewModelIsCreated()

            val isProgressShown = ArrayList<Boolean>()
            viewModel.sitePickerViewStateData.observeForever { old, new ->
                new.isProgressDiaLogVisible.takeIfNotEqualTo(old?.isProgressDiaLogVisible) { isProgressShown.add(it) }
            }

            viewModel.onRefreshButtonClick()

            verify(analyticsTrackerWrapper, times(1)).track(
                AnalyticsEvent.SITE_PICKER_NOT_CONNECTED_JETPACK_REFRESH_APP_LINK_TAPPED
            )
            verify(repository, atLeastOnce()).fetchWooCommerceSites()
            assertThat(isProgressShown).containsExactly(false, true, false)
        }

    @Test
    fun `given user is logging in, when what is Jetpack is clicked, Jetpack screen is displayed`() = testBlocking {
        givenTheScreenIsFromLogin(true)
        whenViewModelIsCreated()

        var view: NavigationToWhatIsJetpackFragmentEvent? = null
        viewModel.event.observeForever {
            if (it is NavigationToWhatIsJetpackFragmentEvent) view = it
        }

        viewModel.onWhatIsJetpackButtonClick()

        verify(analyticsTrackerWrapper, times(1)).track(
            AnalyticsEvent.LOGIN_JETPACK_REQUIRED_WHAT_IS_JETPACK_LINK_TAPPED
        )
        assertThat(view).isEqualTo(NavigationToWhatIsJetpackFragmentEvent)
    }

    @Test
    fun `given user is logging in, when need help finding email is clicked, find email dialog is displayed`() =
        testBlocking {
            givenTheScreenIsFromLogin(true)
            whenViewModelIsCreated()

            var view: NavigateToEmailHelpDialogEvent? = null
            viewModel.event.observeForever {
                if (it is NavigateToEmailHelpDialogEvent) view = it
            }

            viewModel.onNeedHelpFindingEmailButtonClick()

            verify(analyticsTrackerWrapper, times(1)).track(
                AnalyticsEvent.SITE_PICKER_HELP_FINDING_CONNECTED_EMAIL_LINK_TAPPED
            )
            assertThat(view).isEqualTo(NavigateToEmailHelpDialogEvent)
        }

    @Test
    fun `given user is logging in, when view connected sites is clicked, site list is displayed`() = testBlocking {
        val expectedSite = expectedSiteList[1].apply { hasWooCommerce = false }
        givenThatUserLoggedInFromEnteringSiteAddress(expectedSite)
        givenTheScreenIsFromLogin(true)
        whenSitesAreFetched()
        whenViewModelIsCreated()

        var sitePickerData: SitePickerViewModel.SitePickerViewState? = null
        viewModel.sitePickerViewStateData.observeForever { _, new -> sitePickerData = new }

        viewModel.onViewConnectedStoresButtonClick()

        verify(analyticsTrackerWrapper, times(1)).track(
            AnalyticsEvent.SITE_PICKER_VIEW_CONNECTED_STORES_BUTTON_TAPPED
        )
        assertThat(sitePickerData?.isNoStoresViewVisible).isFalse
        assertThat(sitePickerData?.primaryBtnText).isEqualTo(resourceProvider.getString(R.string.continue_button))
        assertThat(sitePickerData?.currentSitePickerState).isEqualTo(StoreListState)
    }
}
