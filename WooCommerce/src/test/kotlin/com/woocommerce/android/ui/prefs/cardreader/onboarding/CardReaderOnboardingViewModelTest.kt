package com.woocommerce.android.ui.prefs.cardreader.onboarding

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.UiString
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingEvent
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.GenericErrorState
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.LoadingState
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.NoConnectionErrorState
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.StripeAcountError
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.StripeExtensionError
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.UnsupportedCountryState
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.WCPayError
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.user.WCUserModel
import org.wordpress.android.fluxc.model.user.WCUserRole

private const val DUMMY_SITE_URL = "dummy-site.url"

@ExperimentalCoroutinesApi
class CardReaderOnboardingViewModelTest : BaseUnitTest() {
    private val onboardingChecker: CardReaderOnboardingChecker = mock()
    private val tracker: AnalyticsTrackerWrapper = mock()
    private val userEligibilityFetcher: UserEligibilityFetcher = mock {
        val model = mock<WCUserModel>()
        whenever(model.getUserRoles()).thenReturn(arrayListOf(WCUserRole.ADMINISTRATOR))
        onBlocking { it.fetchUserInfo() } doReturn model
    }
    private val selectedSite: SelectedSite = mock()

    @Test
    fun `when screen initialized, then loading state shown`() {
        val viewModel = createVM()

        assertThat(viewModel.viewStateData.value).isInstanceOf(LoadingState::class.java)
    }

    @Test
    fun `when onboarding completed, then navigates to card reader hub screen`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.OnboardingCompleted(PluginType.WOOCOMMERCE_PAYMENTS)
            )

            val viewModel = createVM()

            assertThat(viewModel.event.value)
                .isInstanceOf(CardReaderOnboardingViewModel.OnboardingEvent.Continue::class.java)
        }

    @Test
    fun `when store country not supported, then country not supported state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StoreCountryNotSupported(""))

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(UnsupportedCountryState::class.java)
        }

    @Test
    fun `when account country not supported, then country not supported state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StripeAccountCountryNotSupported(""))

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(UnsupportedCountryState::class.java)
        }

    @Test
    fun `when country not supported, then current store country name shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StoreCountryNotSupported("US"))
            val viewModel = createVM()

            val countryName = (viewModel.viewStateData.value as UnsupportedCountryState).headerLabel.params[0]

            assertThat(countryName).isEqualTo(UiString.UiStringText("United States"))
        }

    @Test
    fun `given store country not supported, when learn more clicked, then app shows learn more section`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StoreCountryNotSupported(""))
            val viewModel = createVM()

            (viewModel.viewStateData.value as UnsupportedCountryState).onLearnMoreActionClicked.invoke()

            val event = viewModel.event.value as OnboardingEvent.NavigateToUrlInGenericWebView
            assertThat(event.url).isEqualTo(AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS)
        }

    @Test
    fun `given store country not supported, when contact support clicked, then app navigates to support screen`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StoreCountryNotSupported(""))
            val viewModel = createVM()

            (viewModel.viewStateData.value as UnsupportedCountryState).onContactSupportActionClicked.invoke()

            assertThat(viewModel.event.value)
                .isInstanceOf(OnboardingEvent.NavigateToSupport::class.java)
        }

    @Test
    fun `given account country not supported, when learn more clicked, then app shows learn more section`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StripeAccountCountryNotSupported(""))
            val viewModel = createVM()

            (viewModel.viewStateData.value as UnsupportedCountryState).onLearnMoreActionClicked.invoke()

            val event = viewModel.event.value as OnboardingEvent.NavigateToUrlInGenericWebView
            assertThat(event.url).isEqualTo(AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS)
        }

    @Test
    fun `given account country not supported, when contact support clicked, then app navigates to support screen`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StripeAccountCountryNotSupported(""))
            val viewModel = createVM()

            (viewModel.viewStateData.value as UnsupportedCountryState).onContactSupportActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(OnboardingEvent.NavigateToSupport::class.java)
        }

    @Test
    fun `when wcpay not installed, then wcpay not installed state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(CardReaderOnboardingState.WcpayNotInstalled)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayError.WCPayNotInstalledState::class.java)
        }

    @Test
    fun `when wcpay not activated, then wcpay not activated state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(CardReaderOnboardingState.WcpayNotActivated)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayError.WCPayNotActivatedState::class.java)
        }

    @Test
    fun `when wcpay not setup, then wcpay not setup state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.SetupNotCompleted(PluginType.WOOCOMMERCE_PAYMENTS))

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayError.WCPayNotSetupState::class.java)
        }

    @Test
    fun `when stripe extension not setup, then stripe extension not setup state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.SetupNotCompleted(PluginType.STRIPE_EXTENSION_GATEWAY))

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                StripeExtensionError.StripeExtensionNotSetupState::class.java
            )
        }

    @Test
    fun `when stripe extension not setup, then correct labels and illustrations shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.SetupNotCompleted(PluginType.STRIPE_EXTENSION_GATEWAY))

            val viewModel = createVM()

            val state = (
                viewModel.viewStateData.value as StripeExtensionError.StripeExtensionNotSetupState
                )
            assertThat(state.headerLabel)
                .describedAs("Check header")
                .isEqualTo(UiString.UiStringRes(R.string.card_reader_onboarding_stripe_extension_not_setup_header))
            assertThat(state.hintLabel)
                .describedAs("Check hint")
                .isEqualTo(UiString.UiStringRes(R.string.card_reader_onboarding_stripe_extension_not_setup_hint))
            assertThat(state.refreshButtonLabel)
                .describedAs("Check refreshButtonLabel")
                .isEqualTo(UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_setup_refresh_button))
            assertThat(state.illustration)
                .describedAs("Check illustration")
                .isEqualTo(R.drawable.img_stripe_extension)
        }

    @Test
    fun `when unsupported wcpay version installed, then unsupported wcpay version state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.PluginUnsupportedVersion(PluginType.WOOCOMMERCE_PAYMENTS))

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayError.WCPayUnsupportedVersionState::class.java)
        }

    @Test
    fun `when wcpay in test mode with live stripe account, then wcpay in test mode state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                StripeAcountError.WCPayInTestModeWithLiveAccountState::class.java
            )
        }

    @Test
    fun `when unsupported stripe extension installed, then unsupported stripe extension state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.PluginUnsupportedVersion(PluginType.STRIPE_EXTENSION_GATEWAY)
            )

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                StripeExtensionError.StripeExtensionUnsupportedVersionState::class.java
            )
        }

    @Test
    fun `when stripe extension outdated, then correct labels and illustrations shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.PluginUnsupportedVersion(PluginType.STRIPE_EXTENSION_GATEWAY)
            )

            val viewModel = createVM()

            val state = (
                viewModel.viewStateData.value as StripeExtensionError.StripeExtensionUnsupportedVersionState
                )
            assertThat(state.headerLabel)
                .describedAs("Check header")
                .isEqualTo(
                    UiString.UiStringRes(
                        R.string.card_reader_onboarding_stripe_extension_unsupported_version_header
                    )
                )
            assertThat(state.hintLabel)
                .describedAs("Check hint")
                .isEqualTo(
                    UiString.UiStringRes(
                        R.string.card_reader_onboarding_stripe_extension_unsupported_version_hint
                    )
                )
            assertThat(state.refreshButtonLabel)
                .describedAs("Check refreshButtonLabel")
                .isEqualTo(
                    UiString.UiStringRes(
                        R.string.card_reader_onboarding_wcpay_unsupported_version_refresh_button
                    )
                )
            assertThat(state.illustration)
                .describedAs("Check illustration")
                .isEqualTo(R.drawable.img_stripe_extension)
        }

    @Test
    fun `when wcpay and stripe extension installed-activated, then wcpay and stripe extension activated state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.WcpayAndStripeActivated
            )

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                OnboardingViewState.WcPayAndStripeInstalledState::class.java
            )
        }

    @Test
    fun `given user is admin, when wcpay and stripe extension active, then open wpadmin button shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.WcpayAndStripeActivated
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as OnboardingViewState.WcPayAndStripeInstalledState
            assertThat(viewStateData.openWPAdminActionClicked != null).isTrue
            assertThat(viewStateData.openWPAdminLabel).isNotNull
        }

    @Test
    fun `when wcpay and stripe extension active, then refresh screen button shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.WcpayAndStripeActivated
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as OnboardingViewState.WcPayAndStripeInstalledState
            assertThat(viewStateData.onRefreshAfterUpdatingClicked != null).isTrue
            assertThat(viewStateData.refreshButtonLabel).isNotNull
        }

    @Test
    fun `when refresh clicked, then loading screen shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.WcpayAndStripeActivated
            )
            val viewModel = createVM()
            val receivedViewStates = mutableListOf<OnboardingViewState>()
            viewModel.viewStateData.observeForever {
                receivedViewStates.add(it)
            }

            (viewModel.viewStateData.value as OnboardingViewState.WcPayAndStripeInstalledState)
                .onRefreshAfterUpdatingClicked.invoke()

            assertThat(receivedViewStates[1]).isEqualTo(LoadingState)
        }

    @Test
    fun `given site is self-hosted, when user taps on Go To Plugin Admin, then generic webview shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(selectedSite.get())
                .thenReturn(
                    SiteModel()
                        .apply {
                            setIsWPComAtomic(false)
                            setIsWPCom(false)
                        }
                )
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.WcpayAndStripeActivated
            )
            val viewModel = createVM()

            (viewModel.viewStateData.value as OnboardingViewState.WcPayAndStripeInstalledState)
                .openWPAdminActionClicked!!.invoke()

            assertThat(viewModel.event.value).isInstanceOf(
                OnboardingEvent.NavigateToUrlInGenericWebView::class.java
            )
        }

    @Test
    fun `given site is wpcom, when user taps on Go To Plugin Admin, then wpcom webview shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(selectedSite.get()).thenReturn(
                SiteModel()
                    .apply {
                        setIsWPCom(true)
                    }
            )
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.WcpayAndStripeActivated
            )
            val viewModel = createVM()

            (viewModel.viewStateData.value as OnboardingViewState.WcPayAndStripeInstalledState)
                .openWPAdminActionClicked!!.invoke()

            assertThat(viewModel.event.value).isInstanceOf(
                OnboardingEvent.NavigateToUrlInWPComWebView::class.java
            )
        }

    @Test
    fun `given site is atomic, when user taps on Go To Plugin Admin, then wpcom webview shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(selectedSite.get()).thenReturn(
                SiteModel().apply {
                    setIsWPComAtomic(true)
                }
            )
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.WcpayAndStripeActivated
            )
            val viewModel = createVM()

            (viewModel.viewStateData.value as OnboardingViewState.WcPayAndStripeInstalledState)
                .openWPAdminActionClicked!!.invoke()

            assertThat(viewModel.event.value).isInstanceOf(
                OnboardingEvent.NavigateToUrlInWPComWebView::class.java
            )
        }

    @Test
    fun `when user taps on Go To Plugin Admin, then app navigates to Plugin Admin url`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(selectedSite.get()).thenReturn(
                SiteModel().apply {
                    url = DUMMY_SITE_URL
                }
            )

            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.WcpayAndStripeActivated
            )
            val viewModel = createVM()

            (viewModel.viewStateData.value as OnboardingViewState.WcPayAndStripeInstalledState)
                .openWPAdminActionClicked!!.invoke()

            val event = viewModel.event.value as OnboardingEvent.NavigateToUrlInGenericWebView
            assertThat(event.url).isEqualTo(DUMMY_SITE_URL + AppUrls.PLUGIN_MANAGEMENT_SUFFIX)
        }

    @Test
    fun `given user is NOT admin, when wcpay and stripe extension active, then open wpadmin button NOT shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(userEligibilityFetcher.fetchUserInfo()).thenAnswer {
                val model = mock<WCUserModel>()
                whenever(model.getUserRoles())
                    .thenReturn(arrayListOf(WCUserRole.SHOP_MANAGER))
                model
            }
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.WcpayAndStripeActivated
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as OnboardingViewState.WcPayAndStripeInstalledState
            assertThat(viewStateData.openWPAdminLabel).isNull()
            assertThat(viewStateData.openWPAdminActionClicked == null).isTrue
        }

    @Test
    fun `when account rejected, then account rejected state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StripeAccountRejected)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value)
                .isInstanceOf(StripeAcountError.StripeAccountRejectedState::class.java)
        }

    @Test
    fun `when account pending requirements, then account pending requirements state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    CardReaderOnboardingState.StripeAccountPendingRequirement(
                        0L,
                        PluginType.WOOCOMMERCE_PAYMENTS
                    )
                )

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                StripeAcountError.StripeAccountPendingRequirementsState::class.java
            )
        }

    @Test
    fun `when account pending requirements, then due date not empty`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    CardReaderOnboardingState.StripeAccountPendingRequirement(
                        0L,
                        PluginType.WOOCOMMERCE_PAYMENTS
                    )
                )

            val viewModel = createVM()

            assertThat(
                (viewModel.viewStateData.value as StripeAcountError.StripeAccountPendingRequirementsState).dueDate
            ).isNotEmpty()
        }

    @Test
    fun `when account overdue requirements, then account overdue requirements state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StripeAccountOverdueRequirement)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                StripeAcountError.StripeAccountOverdueRequirementsState::class.java
            )
        }

    @Test
    fun `when account under review, then account under review state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StripeAccountUnderReview)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                StripeAcountError.StripeAccountUnderReviewState::class.java
            )
        }

    @Test
    fun `when onboarding check fails, then generic state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.GenericError)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(GenericErrorState::class.java)
        }

    @Test
    fun `when network not available, then no connection error shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.NoConnectionError)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(NoConnectionErrorState::class.java)
        }

    // Tracking Begin
    @Test
    fun `when learn more tapped, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.GenericError)
            val viewModel = createVM()

            (viewModel.viewStateData.value as GenericErrorState).onLearnMoreActionClicked.invoke()

            verify(tracker).track(AnalyticsTracker.Stat.CARD_PRESENT_ONBOARDING_LEARN_MORE_TAPPED)
        }

    @Test
    fun `when generic error occurs, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.GenericError)

            createVM()

            verify(tracker).track(
                AnalyticsTracker.Stat.CARD_PRESENT_ONBOARDING_NOT_COMPLETED, mapOf("reason" to "generic_error")
            )
        }

    @Test
    fun `when store country not supported, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StoreCountryNotSupported(""))

            createVM()

            verify(tracker).track(
                AnalyticsTracker.Stat.CARD_PRESENT_ONBOARDING_NOT_COMPLETED, mapOf("reason" to "country_not_supported")
            )
        }

    @Test
    fun `when account country not supported, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StripeAccountCountryNotSupported(""))

            createVM()

            verify(tracker).track(
                AnalyticsTracker.Stat.CARD_PRESENT_ONBOARDING_NOT_COMPLETED,
                mapOf("reason" to "account_country_not_supported")
            )
        }

    @Test
    fun `when wcpay not installed, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.WcpayNotInstalled)

            createVM()

            verify(tracker).track(
                AnalyticsTracker.Stat.CARD_PRESENT_ONBOARDING_NOT_COMPLETED, mapOf("reason" to "wcpay_not_installed")
            )
        }

    @Test
    fun `when wcpay not activated, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.WcpayNotActivated)

            createVM()

            verify(tracker).track(
                AnalyticsTracker.Stat.CARD_PRESENT_ONBOARDING_NOT_COMPLETED, mapOf("reason" to "wcpay_not_activated")
            )
        }

    @Test
    fun `when wcpay unsupported version, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.PluginUnsupportedVersion(PluginType.WOOCOMMERCE_PAYMENTS))

            createVM()

            verify(tracker).track(
                AnalyticsTracker.Stat.CARD_PRESENT_ONBOARDING_NOT_COMPLETED,
                mapOf("reason" to "wcpay_unsupported_version")
            )
        }

    @Test
    fun `when wcpay setup not complete, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.SetupNotCompleted(PluginType.WOOCOMMERCE_PAYMENTS))

            createVM()

            verify(tracker).track(
                AnalyticsTracker.Stat.CARD_PRESENT_ONBOARDING_NOT_COMPLETED,
                mapOf("reason" to "wcpay_not_setup")
            )
        }

    @Test
    fun `when account pending requirements, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    CardReaderOnboardingState.StripeAccountPendingRequirement(
                        0L,
                        PluginType.WOOCOMMERCE_PAYMENTS
                    )
                )

            createVM()

            verify(tracker).track(
                AnalyticsTracker.Stat.CARD_PRESENT_ONBOARDING_NOT_COMPLETED,
                mapOf("reason" to "account_pending_requirements")
            )
        }

    @Test
    fun `when account overdue requirements, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StripeAccountOverdueRequirement)

            createVM()

            verify(tracker).track(
                AnalyticsTracker.Stat.CARD_PRESENT_ONBOARDING_NOT_COMPLETED,
                mapOf("reason" to "account_overdue_requirements")
            )
        }

    @Test
    fun `when account under review, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StripeAccountUnderReview)

            createVM()

            verify(tracker).track(
                AnalyticsTracker.Stat.CARD_PRESENT_ONBOARDING_NOT_COMPLETED, mapOf("reason" to "account_under_review")
            )
        }

    @Test
    fun `when account rejected, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StripeAccountRejected)

            createVM()

            verify(tracker).track(
                AnalyticsTracker.Stat.CARD_PRESENT_ONBOARDING_NOT_COMPLETED, mapOf("reason" to "account_rejected")
            )
        }

    @Test
    fun `when wcpay in test mode with live account, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount)

            createVM()

            verify(tracker).track(
                AnalyticsTracker.Stat.CARD_PRESENT_ONBOARDING_NOT_COMPLETED,
                mapOf("reason" to "wcpay_in_test_mode_with_live_account")
            )
        }

    @Test
    fun `when onboarding completed, then event NOT tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.OnboardingCompleted(PluginType.WOOCOMMERCE_PAYMENTS))

            createVM()

            verify(tracker, never()).track(eq(AnalyticsTracker.Stat.CARD_PRESENT_ONBOARDING_NOT_COMPLETED), any())
        }
    // Tracking End

    private fun createVM() =
        CardReaderOnboardingViewModel(
            SavedStateHandle(),
            onboardingChecker,
            tracker,
            userEligibilityFetcher,
            selectedSite
        )
}
