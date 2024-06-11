package com.woocommerce.android.ui.sitepicker

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.analytics.ExperimentTracker
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorViewModel.AccountMismatchPrimaryButton.CONNECT_JETPACK
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigateToAccountMismatchScreen
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigateToAddStoreEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigateToEmailHelpDialogEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigateToHelpFragmentEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigateToMainActivityEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigateToNewToWooEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.ShowWooUpgradeDialogEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerState.NoStoreState
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerState.StoreListState
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerState.WooNotFoundState
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitesListItem
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitesListItem.NonWooSiteUiModel
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitesListItem.WooSiteUiModel
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Logout
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assert
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload

@ExperimentalCoroutinesApi
class SitePickerViewModelTest : BaseUnitTest() {
    private val defaultExpectedSiteList = SitePickerTestUtils.generateStores()

    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { invocationOnMock -> invocationOnMock.arguments[0].toString() }
        on { getString(any(), any()) } doAnswer { invocationOnMock -> invocationOnMock.arguments[0].toString() }
    }
    private val selectedSite: SelectedSite = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val userEligibilityFetcher: UserEligibilityFetcher = mock()
    private val repository: SitePickerRepository = mock {
        onBlocking { getSites() } doReturn defaultExpectedSiteList.toMutableList()
    }
    private val accountRepository: AccountRepository = mock()
    private val unifiedLoginTracker: UnifiedLoginTracker = mock()
    private val experimentTracker: ExperimentTracker = mock()

    private lateinit var viewModel: SitePickerViewModel
    private lateinit var savedState: SavedStateHandle

    private fun whenViewModelIsCreated() {
        viewModel = SitePickerViewModel(
            savedState = savedState,
            selectedSite = selectedSite,
            repository = repository,
            accountRepository = accountRepository,
            resourceProvider = resourceProvider,
            appPrefsWrapper = appPrefsWrapper,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            userEligibilityFetcher = userEligibilityFetcher,
            unifiedLoginTracker = unifiedLoginTracker,
            experimentTracker = experimentTracker,
        )
    }

    private fun givenTheScreenIsFromLogin(calledFromLogin: Boolean) {
        savedState = SitePickerFragmentArgs(openedFromLogin = calledFromLogin).toSavedStateHandle()
    }

    private fun givenThatUserLoggedInFromEnteringSiteAddress(expectedSite: SiteModel? = null) {
        whenever(appPrefsWrapper.getLoginSiteAddress()).thenReturn(SitePickerTestUtils.loginSiteAddress)
        whenever(repository.getSiteBySiteUrl(any())).thenReturn(expectedSite)
    }

    private suspend fun givenThatSiteVerificationIsCompleted() {
        whenever(repository.verifySiteWooAPIVersion(any())).thenReturn(
            WooResult(SitePickerTestUtils.apiVerificationResponse)
        )
        whenever(userEligibilityFetcher.fetchUserInfo()).thenReturn(Result.success(SitePickerTestUtils.userModel))
    }

    private suspend fun whenSitesAreFetched(
        returnsError: Boolean = false,
        returnsEmpty: Boolean = false,
        sitesFromDb: List<SiteModel> = defaultExpectedSiteList,
        sitesFromApi: List<SiteModel> = defaultExpectedSiteList
    ) {
        when {
            returnsEmpty -> {
                whenever(repository.fetchWooCommerceSites()).thenReturn(WooResult(mutableListOf()))
                whenever(repository.getSites()).thenReturn(mutableListOf())
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
                whenever(repository.getSites()).thenReturn(mutableListOf())
            }

            else -> {
                whenever(repository.getSites()).thenReturn(sitesFromDb)
                whenever(repository.fetchWooCommerceSites()).thenReturn(WooResult(sitesFromApi))
            }
        }
    }

    private val defaultSitePickerViewState = SitePickerViewModel.SitePickerViewState(
        userInfo = SitePickerTestUtils.userInfo,
        primaryBtnText = resourceProvider.getString(R.string.continue_button),
        secondaryBtnText = resourceProvider.getString(R.string.login_try_another_account),
        hasConnectedStores = defaultExpectedSiteList.isNotEmpty()
    )

    @Before
    fun setup() {
        whenever(accountRepository.getUserAccount()).thenReturn(SitePickerTestUtils.account)
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
            SitePickerTestUtils.getDefaultSwitchStoreViewState(defaultSitePickerViewState, resourceProvider)
        )
    }

    @Test
    fun `given that the view model is created, when stores fetch succeeds, then stores are displayed correctly`() =
        testBlocking {
            whenSitesAreFetched()
            whenViewModelIsCreated()

            var items: List<SitesListItem>? = null
            viewModel.sites.observeForever { items = it }

            val sites = items?.filterIsInstance<WooSiteUiModel>()

            verify(repository, times(1)).fetchWooCommerceSites()
            verify(analyticsTrackerWrapper, times(1)).track(
                AnalyticsEvent.SITE_PICKER_STORES_SHOWN,
                mapOf(
                    AnalyticsTracker.KEY_NUMBER_OF_STORES to sites?.size,
                    AnalyticsTracker.KEY_NUMBER_OF_NON_WOO_SITES to 0
                )
            )

            assertThat(items).isNotEmpty
            assertThat(items?.first()).isEqualTo(SitesListItem.Header(R.string.login_pick_store))
            assertThat(sites?.first()?.isSelected).isTrue()
        }

    @Test
    fun `given some sites don't have woo, when stores fetch succeeds, then show sites in separate sections`() =
        testBlocking {
            fun MutableList<SitesListItem>.assertThenRemoveFirstItem(
                assertion: Assert<*, *>.() -> Unit
            ): MutableList<SitesListItem> {
                assertThat(first()).assertion()
                removeFirst()
                return this
            }

            val expectedSites = defaultExpectedSiteList.mapIndexed { index, siteModel ->
                if (index < 2) siteModel.apply { hasWooCommerce = false } else siteModel
            }
            whenever(repository.fetchWooCommerceSites()).thenReturn(WooResult(expectedSites))
            whenViewModelIsCreated()

            val items = viewModel.sites.captureValues().last().toMutableList()

            verify(analyticsTrackerWrapper, times(1)).track(
                AnalyticsEvent.SITE_PICKER_STORES_SHOWN,
                mapOf(
                    AnalyticsTracker.KEY_NUMBER_OF_STORES to items.count { it is WooSiteUiModel },
                    AnalyticsTracker.KEY_NUMBER_OF_NON_WOO_SITES to items.count { it is NonWooSiteUiModel }
                )
            )

            items.assertThenRemoveFirstItem { isEqualTo(SitesListItem.Header(R.string.login_pick_store)) }
            repeat(expectedSites.count { it.hasWooCommerce }) {
                items.assertThenRemoveFirstItem { isInstanceOf(WooSiteUiModel::class.java) }
            }
            items.assertThenRemoveFirstItem { isEqualTo(SitesListItem.Header(R.string.login_non_woo_stores_label)) }
            repeat(expectedSites.count { !it.hasWooCommerce }) {
                items.assertThenRemoveFirstItem { isInstanceOf(NonWooSiteUiModel::class.java) }
            }
        }

    @Test
    fun `given that the view model is created, when store fetch is in error, then error is displayed`() = testBlocking {
        whenSitesAreFetched(returnsError = true)
        whenViewModelIsCreated()

        var sites: List<SitesListItem>? = null
        viewModel.sites.observeForever { sites = it }

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        verify(repository, times(1)).fetchWooCommerceSites()
        verify(repository, times(1)).getSites()

        assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.site_picker_error))
        assertThat(sites).isNull()
    }

    @Test
    fun `given that the view model is created, when store fetch is empty, then empty view is displayed`() =
        testBlocking {
            val expectedSitePickerViewState = SitePickerTestUtils.getEmptyViewState(
                defaultSitePickerViewState,
                resourceProvider
            )
            whenSitesAreFetched(returnsEmpty = true)
            whenViewModelIsCreated()
            advanceUntilIdle()

            var sitePickerData: SitePickerViewModel.SitePickerViewState? = null
            viewModel.sitePickerViewStateData.observeForever { _, new -> sitePickerData = new }

            var items: List<SitesListItem>? = null
            viewModel.sites.observeForever { items = it }

            assertThat(sitePickerData?.isNoStoresViewVisible).isEqualTo(
                expectedSitePickerViewState.isNoStoresViewVisible
            )
            assertThat(sitePickerData?.primaryBtnText).isEqualTo(expectedSitePickerViewState.primaryBtnText)
            assertThat(sitePickerData?.noStoresLabelText).isEqualTo(expectedSitePickerViewState.noStoresLabelText)
            assertThat(sitePickerData?.noStoresBtnText).isEqualTo(expectedSitePickerViewState.noStoresBtnText)
            assertThat(sitePickerData?.currentSitePickerState).isEqualTo(NoStoreState)
            assertThat(items).isNull()
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
        givenThatUserLoggedInFromEnteringSiteAddress(defaultExpectedSiteList[1])
        givenThatSiteVerificationIsCompleted()
        whenSitesAreFetched()
        whenViewModelIsCreated()

        var items: List<SitesListItem>? = null
        viewModel.sites.observeForever { items = it }

        verify(appPrefsWrapper, atLeastOnce()).getLoginSiteAddress()
        verify(repository, atLeastOnce()).getSiteBySiteUrl(any())
        val sites = items?.filterIsInstance<WooSiteUiModel>()
        assertThat(sites?.get(1)?.isSelected).isTrue
        assertThat(sites?.get(1)?.site?.url).isEqualTo(SitePickerTestUtils.loginSiteAddress)
    }

    @Test
    fun `given login with wp email, when only a single woo store is available, then site should be auto selected`() =
        testBlocking {
            givenTheScreenIsFromLogin(calledFromLogin = true)
            givenThatSiteVerificationIsCompleted()
            val siteList = listOf(defaultExpectedSiteList.first())
            whenSitesAreFetched(sitesFromDb = siteList, sitesFromApi = siteList)

            whenViewModelIsCreated()

            var items: List<SitesListItem>? = null
            viewModel.sites.observeForever { items = it }
            val event = viewModel.event.captureValues().last()

            val sites = items?.filterIsInstance<WooSiteUiModel>()
            assertThat(sites?.first()?.isSelected).isTrue
            assertThat(event).isEqualTo(NavigateToMainActivityEvent)
            verify(repository, times(2)).verifySiteWooAPIVersion(sites?.first()?.site!!)
        }

    @Test
    fun `given the site address entered during login does not match the user account, account error is displayed`() =
        testBlocking {
            givenThatUserLoggedInFromEnteringSiteAddress(null)
            whenever(repository.fetchSiteInfo(any())).thenReturn(
                Result.success(
                    ConnectSiteInfoPayload(
                        url = SitePickerTestUtils.loginSiteAddress,
                        isWordPress = true,
                        isWPCom = false
                    )
                )
            )
            whenSitesAreFetched()
            whenViewModelIsCreated()

            val url = SitePickerTestUtils.loginSiteAddress

            verify(repository, atLeastOnce()).getSiteBySiteUrl(any())
            verify(analyticsTrackerWrapper, atLeastOnce()).track(
                AnalyticsEvent.SITE_PICKER_AUTO_LOGIN_ERROR_NOT_CONNECTED_TO_USER,
                mapOf(
                    AnalyticsTracker.KEY_URL to url,
                    AnalyticsTracker.KEY_HAS_CONNECTED_STORES to true
                )
            )

            assertThat(viewModel.event.value).isEqualTo(NavigateToAccountMismatchScreen(CONNECT_JETPACK, url))
        }

    @Test
    fun `given that the site address entered during login does not have Woo, no woo error screen is displayed`() =
        testBlocking {
            givenThatUserLoggedInFromEnteringSiteAddress(
                defaultExpectedSiteList[1].apply {
                    hasWooCommerce = false
                    setIsJetpackConnected(true)
                }
            )
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
                resourceProvider.getString(R.string.login_install_woo)
            )
            assertThat(sitePickerData?.noStoresLabelText).isEqualTo(
                resourceProvider.getString(R.string.login_not_woo_store, url)
            )
            assertThat(sitePickerData?.noStoresBtnText).isEqualTo(
                resourceProvider.getString(R.string.login_view_connected_stores)
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

        val selectedSiteModel = defaultExpectedSiteList[1]

        viewModel.onSiteSelected(selectedSiteModel)
        viewModel.onContinueButtonClick()

        verify(repository, times(1)).verifySiteWooAPIVersion(any())
        verify(selectedSite, times(1)).set(any())
        verify(userEligibilityFetcher, times(1)).fetchUserInfo()
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

            val selectedSiteModel = defaultExpectedSiteList[1]

            viewModel.onSiteSelected(selectedSiteModel)
            viewModel.onContinueButtonClick()

            verify(repository, times(1)).verifySiteWooAPIVersion(any())
            verify(selectedSite, times(0)).set(any())
            verify(userEligibilityFetcher, times(0)).fetchUserInfo()
            verify(appPrefsWrapper, times(0)).removeLoginSiteAddress()

            assertThat(view).isEqualTo(ShowWooUpgradeDialogEvent)
            assertThat(isProgressShown).containsExactly(false, true, false)
        }

    @Test
    fun `given list of sites is shown, when a non-woo site is tapped, then show the Woo not found error`() =
        testBlocking {
            val expectedSites = defaultExpectedSiteList.mapIndexed { index, siteModel ->
                if (index == 0) {
                    siteModel.apply {
                        hasWooCommerce = false
                        url = SitePickerTestUtils.loginSiteAddress
                        setIsJetpackConnected(true)
                    }
                } else {
                    siteModel
                }
            }
            whenever(repository.fetchWooCommerceSites()).thenReturn(WooResult(expectedSites))
            whenViewModelIsCreated()

            val sitePickerState = viewModel.sitePickerViewStateData.liveData.runAndCaptureValues {
                viewModel.onNonWooSiteSelected(expectedSites[0])
            }.last()

            assertThat(sitePickerState.currentSitePickerState).isEqualTo(WooNotFoundState)
            verify(analyticsTrackerWrapper).track(
                stat = AnalyticsEvent.SITE_PICKER_NON_WOO_SITE_TAPPED,
                properties = mapOf(AnalyticsTracker.KEY_IS_NON_ATOMIC to false)
            )
        }

    @Test
    fun `given user is logging in, then when help button is clicked, help screen is displayed`() = testBlocking {
        givenTheScreenIsFromLogin(true)
        whenViewModelIsCreated()

        var view: NavigateToHelpFragmentEvent? = null
        viewModel.event.observeForever {
            if (it is NavigateToHelpFragmentEvent) view = it
        }

        viewModel.onHelpButtonClick()

        verify(analyticsTrackerWrapper, times(1)).track(AnalyticsEvent.SITE_PICKER_HELP_BUTTON_TAPPED)
        assertThat(view).isEqualTo(NavigateToHelpFragmentEvent(HelpOrigin.LOGIN_EPILOGUE))
    }

    @Test
    fun `given user is logging in, then when try another account is clicked, logout is initiated`() = testBlocking {
        whenever(accountRepository.logout()).thenReturn(true)
        givenTheScreenIsFromLogin(true)
        whenViewModelIsCreated()

        var view: Logout? = null
        viewModel.event.observeForever {
            if (it is Logout) view = it
        }

        viewModel.onTryAnotherAccountButtonClick()

        verify(accountRepository, times(1)).logout()
        assertThat(view).isEqualTo(Logout)
    }

    @Test
    fun `given there are no sites, when add a store is tapped, then navigate to site discovery screen`() =
        testBlocking {
            givenTheScreenIsFromLogin(true)
            whenViewModelIsCreated()

            var view: NavigateToAddStoreEvent? = null
            viewModel.event.observeForever {
                if (it is NavigateToAddStoreEvent) view = it
            }

            viewModel.onAddStoreClick()

            verify(analyticsTrackerWrapper, times(1)).track(
                AnalyticsEvent.SITE_PICKER_ADD_A_STORE_TAPPED
            )
            assertThat(view).isEqualTo(NavigateToAddStoreEvent)
        }

    @Test
    fun `given user is logging in, when new to woo clicked, then open browser with the docs`() = testBlocking {
        givenTheScreenIsFromLogin(true)
        whenViewModelIsCreated()

        var view: NavigateToNewToWooEvent? = null
        viewModel.event.observeForever {
            if (it is NavigateToNewToWooEvent) view = it
        }

        viewModel.onNewToWooClick()

        verify(analyticsTrackerWrapper, times(1)).track(
            AnalyticsEvent.SITE_PICKER_NEW_TO_WOO_TAPPED
        )
        assertThat(view).isEqualTo(NavigateToNewToWooEvent)
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
        val expectedSite = defaultExpectedSiteList[1].apply { hasWooCommerce = false }
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

    @Test
    fun `when install woo is tapped, then open a webview to trigger installation`() = testBlocking {
        val expectedSite = defaultExpectedSiteList[1].apply { hasWooCommerce = false }
        givenThatUserLoggedInFromEnteringSiteAddress(expectedSite)
        givenTheScreenIsFromLogin(true)

        whenSitesAreFetched()
        whenViewModelIsCreated()
        viewModel.onInstallWooClicked()

        val event = viewModel.event.captureValues().last()
        assertThat(event).isInstanceOf(SitePickerEvent.NavigateToWPComWebView::class.java)
    }

    @Test
    fun `when woo installation completes, then continue login`() = testBlocking {
        val expectedSite = defaultExpectedSiteList[1].apply { hasWooCommerce = false }
        givenThatUserLoggedInFromEnteringSiteAddress(expectedSite)
        givenTheScreenIsFromLogin(true)
        whenSitesAreFetched()
        whenever(repository.fetchWooCommerceSite(expectedSite))
            .thenReturn(Result.success(expectedSite.apply { hasWooCommerce = true }))

        whenViewModelIsCreated()
        viewModel.onWooInstalled()

        val sites = viewModel.sites.captureValues().last().filterIsInstance<WooSiteUiModel>()
        assertThat(sites[1].isSelected).isTrue
        assertThat(sites[1].site.url).isEqualTo(SitePickerTestUtils.loginSiteAddress)
    }

    @Test
    fun `given woo installation finishes, when fetching site fails, then retry`() = testBlocking {
        val expectedSite = defaultExpectedSiteList[1].apply { hasWooCommerce = false }
        val expectedSiteClone = SitePickerTestUtils.generateStores()[1].apply { hasWooCommerce = true }
        givenThatUserLoggedInFromEnteringSiteAddress(expectedSite)
        givenTheScreenIsFromLogin(true)
        whenSitesAreFetched()
        whenever(repository.fetchWooCommerceSite(expectedSite))
            .thenReturn(Result.failure(Exception()))
            .thenReturn(Result.success(expectedSiteClone))

        whenViewModelIsCreated()
        viewModel.onWooInstalled()
        advanceUntilIdle()

        verify(repository, times(2)).fetchWooCommerceSite(expectedSite)
    }

    @Test
    fun `given woo installation finishes, when fetched site doesn't have woo, then retry`() = testBlocking {
        val expectedSite = defaultExpectedSiteList[1].apply { hasWooCommerce = false }
        val expectedSiteCloneOne = SitePickerTestUtils.generateStores()[1].apply { hasWooCommerce = false }
        val expectedSiteCloneTwo = SitePickerTestUtils.generateStores()[1].apply { hasWooCommerce = true }
        givenThatUserLoggedInFromEnteringSiteAddress(expectedSite)
        givenTheScreenIsFromLogin(true)
        whenSitesAreFetched()
        whenever(repository.fetchWooCommerceSite(expectedSite))
            .thenReturn(Result.success(expectedSiteCloneOne))
            .thenReturn(Result.success(expectedSiteCloneTwo))

        whenViewModelIsCreated()
        viewModel.onWooInstalled()
        advanceUntilIdle()

        verify(repository, times(2)).fetchWooCommerceSite(expectedSite)
    }

    @Test
    fun `given site verification returns timeout error, when verifying site, timeout dialog is displayed`() =
        testBlocking {
            whenever(repository.verifySiteWooAPIVersion(any())).thenReturn(
                WooResult(SitePickerTestUtils.timeoutErrorApiVerificationResponse)
            )
            whenSitesAreFetched()
            whenViewModelIsCreated()

            val isProgressShown = ArrayList<Boolean>()
            viewModel.sitePickerViewStateData.observeForever { old, new ->
                new.isProgressDiaLogVisible.takeIfNotEqualTo(old?.isProgressDiaLogVisible) { isProgressShown.add(it) }
            }

            var view: ShowDialog? = null
            viewModel.event.observeForever {
                if (it is ShowDialog) view = it
            }

            val selectedSiteModel = defaultExpectedSiteList[1]

            viewModel.onSiteSelected(selectedSiteModel)
            viewModel.onContinueButtonClick()

            verify(repository, times(1)).verifySiteWooAPIVersion(any())
            assertThat(view).isInstanceOf(ShowDialog::class.java)
            assertThat(isProgressShown).containsExactly(false, true, false)
        }

    @Test
    fun `given entered site is a simple WPCom site, when loading site picker, then display simple site state`() =
        testBlocking {
            givenTheScreenIsFromLogin(true)
            givenThatUserLoggedInFromEnteringSiteAddress(
                defaultExpectedSiteList[1].apply {
                    setIsWPCom(true)
                }
            )
            whenSitesAreFetched()
            whenViewModelIsCreated()

            val state = viewModel.sitePickerViewStateData.liveData.captureValues().last()

            assertThat(state.currentSitePickerState).isEqualTo(SitePickerViewModel.SitePickerState.SimpleWPComState)
            assertThat(state.isNoStoresViewVisible).isTrue
            assertThat(state.noStoresLabelText).isEqualTo(resourceProvider.getString(R.string.login_simple_wpcom_site))
            assertThat(state.isNoStoresBtnVisible).isFalse
        }
}
