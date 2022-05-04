package com.woocommerce.android.ui.cardreader.onboarding

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.UiString
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.cardreader.CardReaderTracker
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingState.PluginIsNotSupportedInTheCountry
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingEvent
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.GenericErrorState
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.LoadingState
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.NoConnectionErrorState
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.StripeAcountError
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.StripeExtensionError
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.UnsupportedErrorState
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.WCPayError
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.WCPayError.WCPayNotInstalledState
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.WcPayAndStripeInstalledState
import com.woocommerce.android.ui.cardreader.onboarding.PluginType.STRIPE_EXTENSION_GATEWAY
import com.woocommerce.android.ui.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.user.WCUserModel
import org.wordpress.android.fluxc.model.user.WCUserRole

private const val DUMMY_SITE_URL = "dummy-site.url"

@ExperimentalCoroutinesApi
class CardReaderOnboardingViewModelTest : BaseUnitTest() {
    private val onboardingChecker: CardReaderOnboardingChecker = mock()
    private val tracker: CardReaderTracker = mock()
    private val userEligibilityFetcher: UserEligibilityFetcher = mock {
        val model = mock<WCUserModel>()
        whenever(model.getUserRoles()).thenReturn(arrayListOf(WCUserRole.ADMINISTRATOR))
        onBlocking { it.fetchUserInfo() } doReturn model
    }
    private val selectedSite: SelectedSite = mock {
        on(it.get()).thenReturn(SiteModel())
    }
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val countryCode = "US"
    private val pluginVersion = "4.0.0"

    @Test
    fun `when screen initialized, then loading state shown`() {
        val viewModel = createVM()

        assertThat(viewModel.viewStateData.value).isInstanceOf(LoadingState::class.java)
    }

    @Test
    fun `given hub flow, when onboarding completed, then navigates to card reader hub screen`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.OnboardingCompleted(WOOCOMMERCE_PAYMENTS, pluginVersion, countryCode)
            )

            val viewModel = createVM()

            assertThat(viewModel.event.value)
                .isInstanceOf(OnboardingEvent.ContinueToHub::class.java)
        }

    @Test
    fun `given payment flow, when onboarding completed, then navigates to card reader connection screen`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.OnboardingCompleted(WOOCOMMERCE_PAYMENTS, pluginVersion, countryCode)
            )

            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    cardReaderOnboardingParam = CardReaderOnboardingParams.Check(
                        CardReaderFlowParam.PaymentOrRefund.Payment(1L)
                    )
                ).initSavedStateHandle()
            )

            assertThat(viewModel.event.value)
                .isInstanceOf(OnboardingEvent.ContinueToConnection::class.java)
        }

    @Test
    fun `when store country not supported, then country not supported state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StoreCountryNotSupported(""))

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(UnsupportedErrorState.Country::class.java)
        }

    @Test
    fun `given incoming failed onboarding, when view model init, then get onboarding state never called`() =
        testBlocking {
            createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = CardReaderOnboardingState.StoreCountryNotSupported(""),
                    )
                ).initSavedStateHandle()
            )

            verify(onboardingChecker, never()).getOnboardingState()
        }

    @Test
    fun `given country not supported incoming, when view model init, then country not supported state shown`() =
        testBlocking {
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = CardReaderOnboardingState.StoreCountryNotSupported(""),
                    )
                ).initSavedStateHandle()
            )

            assertThat(viewModel.viewStateData.value).isInstanceOf(UnsupportedErrorState.Country::class.java)
        }

    @Test
    fun `given wcpay is not supported in country, when init, then wcpay in country not supported state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    PluginIsNotSupportedInTheCountry(
                        WOOCOMMERCE_PAYMENTS,
                        ""
                    )
                )

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(UnsupportedErrorState.WcPayInCountry::class.java)
        }

    @Test
    fun `given incoming wcpay is not supported in country, when view model init, then country not supported shown`() =
        testBlocking {
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = PluginIsNotSupportedInTheCountry(
                            WOOCOMMERCE_PAYMENTS,
                            ""
                        ),
                    )
                ).initSavedStateHandle()
            )

            assertThat(viewModel.viewStateData.value).isInstanceOf(UnsupportedErrorState.WcPayInCountry::class.java)
        }

    @Test
    fun `given stripe is not supported in country, when init, then stripe in country not supported state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    PluginIsNotSupportedInTheCountry(
                        STRIPE_EXTENSION_GATEWAY,
                        ""
                    )
                )

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(UnsupportedErrorState.StripeInCountry::class.java)
        }

    @Test
    fun `given incoming stripe is not supported in country, when view model init, then country not supported shown`() =
        testBlocking {
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = PluginIsNotSupportedInTheCountry(
                            STRIPE_EXTENSION_GATEWAY,
                            ""
                        ),
                    )
                ).initSavedStateHandle()
            )

            assertThat(viewModel.viewStateData.value).isInstanceOf(UnsupportedErrorState.StripeInCountry::class.java)
        }

    @Test
    fun `given incoming wcpay not installed in country, when view model init, then wc pay not installed shown`() =
        testBlocking {
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = CardReaderOnboardingState.WcpayNotInstalled,
                    )
                ).initSavedStateHandle()
            )

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayNotInstalledState::class.java)
        }

    @Test
    fun `when stripe account country not supported, then country not supported state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StripeAccountCountryNotSupported(mock(), ""))

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                UnsupportedErrorState.StripeAccountInUnsupportedCountry::class.java
            )
        }

    @Test
    fun `when incoming stripe account country not supported, then country not supported state shown`() =
        testBlocking {
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = CardReaderOnboardingState.StripeAccountCountryNotSupported(mock(), ""),
                    )
                ).initSavedStateHandle()
            )

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                UnsupportedErrorState.StripeAccountInUnsupportedCountry::class.java
            )
        }

    @Test
    fun `given wcpay not supported in country, when init, then current store country name shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    PluginIsNotSupportedInTheCountry(
                        WOOCOMMERCE_PAYMENTS,
                        "US"
                    )
                )
            val viewModel = createVM()

            val countryName = (
                (viewModel.viewStateData.value as UnsupportedErrorState.WcPayInCountry)
                    .headerLabel as UiString.UiStringRes
                ).params[0]

            assertThat(countryName).isEqualTo(UiString.UiStringText("United States"))
        }

    @Test
    fun `given incoming plugin not supported, when view model init, then plugin not supported shown`() =
        testBlocking {
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = PluginIsNotSupportedInTheCountry(
                            WOOCOMMERCE_PAYMENTS,
                            "US"
                        ),
                    )
                ).initSavedStateHandle()
            )

            assertThat(viewModel.viewStateData.value).isInstanceOf(UnsupportedErrorState.WcPayInCountry::class.java)
        }

    @Test
    fun `when country not supported, then current store country name shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StoreCountryNotSupported("US"))
            val viewModel = createVM()

            val countryName = (
                (viewModel.viewStateData.value as UnsupportedErrorState.Country)
                    .headerLabel as UiString.UiStringRes
                ).params[0]

            assertThat(countryName).isEqualTo(UiString.UiStringText("United States"))
        }

    @Test
    fun `when incoming country not supported, then current store country name shown`() =
        testBlocking {
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = CardReaderOnboardingState.StoreCountryNotSupported("US"),
                    )
                ).initSavedStateHandle()
            )

            val countryName = (
                (viewModel.viewStateData.value as UnsupportedErrorState.Country)
                    .headerLabel as UiString.UiStringRes
                ).params[0]
            assertThat(countryName).isEqualTo(UiString.UiStringText("United States"))
        }

    @Test
    fun `given wcpay not supported in country, when learn more clicked, then app shows learn more section`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    PluginIsNotSupportedInTheCountry(
                        WOOCOMMERCE_PAYMENTS,
                        "US"
                    )
                )
            val viewModel = createVM()

            (viewModel.viewStateData.value as UnsupportedErrorState.WcPayInCountry)
                .onLearnMoreActionClicked.invoke()

            val event = viewModel.event.value as OnboardingEvent.NavigateToUrlInGenericWebView
            assertThat(event.url).isEqualTo(AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS)
        }

    @Test
    fun `given store country not supported, when learn more clicked, then app shows learn more section`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StoreCountryNotSupported(""))
            val viewModel = createVM()

            (viewModel.viewStateData.value as UnsupportedErrorState.Country)
                .onLearnMoreActionClicked.invoke()

            val event = viewModel.event.value as OnboardingEvent.NavigateToUrlInGenericWebView
            assertThat(event.url).isEqualTo(AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS)
        }

    @Test
    fun `given store country not supported, when contact support clicked, then app navigates to support screen`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StoreCountryNotSupported(""))
            val viewModel = createVM()

            (viewModel.viewStateData.value as UnsupportedErrorState.Country).onContactSupportActionClicked.invoke()

            assertThat(viewModel.event.value)
                .isInstanceOf(OnboardingEvent.NavigateToSupport::class.java)
        }

    @Test
    fun `given wcpay in not supported in country, when contact support clicked, then app nav to support screen`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    PluginIsNotSupportedInTheCountry(
                        WOOCOMMERCE_PAYMENTS,
                        "US"
                    )
                )
            val viewModel = createVM()

            (viewModel.viewStateData.value as UnsupportedErrorState.WcPayInCountry)
                .onContactSupportActionClicked.invoke()

            assertThat(viewModel.event.value)
                .isInstanceOf(OnboardingEvent.NavigateToSupport::class.java)
        }

    @Test
    fun `given preferred plugin Stripe, when learn more clicked, then app navigates to Stripe docs`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StoreCountryNotSupported(""))
            whenever(appPrefsWrapper.getCardReaderPreferredPlugin(any(), any(), any()))
                .thenReturn(STRIPE_EXTENSION_GATEWAY)

            val viewModel = createVM()

            (viewModel.viewStateData.value as UnsupportedErrorState.Country).onLearnMoreActionClicked.invoke()

            val event = viewModel.event.value as OnboardingEvent.NavigateToUrlInGenericWebView
            assertThat(event.url).isEqualTo(AppUrls.STRIPE_LEARN_MORE_ABOUT_PAYMENTS)
        }

    @Test
    fun `given preferred plugin WCPay, when learn more clicked, then app navigates to wcpay docs`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StoreCountryNotSupported(""))
            whenever(appPrefsWrapper.getCardReaderPreferredPlugin(any(), any(), any()))
                .thenReturn(WOOCOMMERCE_PAYMENTS)

            val viewModel = createVM()

            (viewModel.viewStateData.value as UnsupportedErrorState.Country).onLearnMoreActionClicked.invoke()

            val event = viewModel.event.value as OnboardingEvent.NavigateToUrlInGenericWebView
            assertThat(event.url).isEqualTo(AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS)
        }

    @Test
    fun `given preferred plugin null, when learn more clicked, then app navigates to wcpay docs`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StoreCountryNotSupported(""))
            whenever(appPrefsWrapper.getCardReaderPreferredPlugin(any(), any(), any()))
                .thenReturn(null)

            val viewModel = createVM()

            (viewModel.viewStateData.value as UnsupportedErrorState.Country).onLearnMoreActionClicked.invoke()

            val event = viewModel.event.value as OnboardingEvent.NavigateToUrlInGenericWebView
            assertThat(event.url).isEqualTo(AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS)
        }

    @Test
    fun `given stripe account country not supported, when learn more clicked, then app shows learn more section`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StripeAccountCountryNotSupported(mock(), ""))
            val viewModel = createVM()

            (viewModel.viewStateData.value as UnsupportedErrorState.StripeAccountInUnsupportedCountry)
                .onLearnMoreActionClicked.invoke()

            val event = viewModel.event.value as OnboardingEvent.NavigateToUrlInGenericWebView
            assertThat(event.url).isEqualTo(AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS)
        }

    @Test
    fun `given stripe account country not supported, when contact support clicked, then app navs to support screen`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StripeAccountCountryNotSupported(mock(), ""))
            val viewModel = createVM()

            (viewModel.viewStateData.value as UnsupportedErrorState.StripeAccountInUnsupportedCountry)
                .onContactSupportActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(OnboardingEvent.NavigateToSupport::class.java)
        }

    @Test
    fun `when wcpay not installed, then wcpay not installed state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(CardReaderOnboardingState.WcpayNotInstalled)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayNotInstalledState::class.java)
        }

    @Test
    fun `when wcpay not activated, then wcpay not activated state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(CardReaderOnboardingState.WcpayNotActivated)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayError.WCPayNotActivatedState::class.java)
        }

    @Test
    fun `when incoming wcpay not activated, then wcpay not activated state shown`() =
        testBlocking {
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = CardReaderOnboardingState.WcpayNotActivated
                    )
                ).initSavedStateHandle()
            )

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayError.WCPayNotActivatedState::class.java)
        }

    @Test
    fun `when wcpay not setup, then wcpay not setup state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.SetupNotCompleted(WOOCOMMERCE_PAYMENTS))

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayError.WCPayNotSetupState::class.java)
        }

    @Test
    fun `when incoming wcpay not setup, then wcpay not setup state shown`() =
        testBlocking {
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = CardReaderOnboardingState.SetupNotCompleted(WOOCOMMERCE_PAYMENTS),
                    )
                ).initSavedStateHandle()
            )

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayError.WCPayNotSetupState::class.java)
        }

    @Test
    fun `when stripe extension not setup, then stripe extension not setup state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.SetupNotCompleted(STRIPE_EXTENSION_GATEWAY))

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                StripeExtensionError.StripeExtensionNotSetupState::class.java
            )
        }

    @Test
    fun `when stripe extension not setup, then correct labels and illustrations shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.SetupNotCompleted(STRIPE_EXTENSION_GATEWAY))

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
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.PluginUnsupportedVersion(WOOCOMMERCE_PAYMENTS))

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayError.WCPayUnsupportedVersionState::class.java)
        }

    @Test
    fun `when wcpay in test mode with live stripe account, then wcpay in test mode state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount(mock()))

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                StripeAcountError.PluginInTestModeWithLiveAccountState::class.java
            )
        }

    @Test
    fun `when incoming wcpay in test mode with live stripe account, then wcpay in test mode state shown`() =
        testBlocking {
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount(mock()),
                    )
                ).initSavedStateHandle()
            )

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                StripeAcountError.PluginInTestModeWithLiveAccountState::class.java
            )
        }

    @Test
    fun `when unsupported stripe extension installed, then unsupported stripe extension state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.PluginUnsupportedVersion(STRIPE_EXTENSION_GATEWAY)
            )

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                StripeExtensionError.StripeExtensionUnsupportedVersionState::class.java
            )
        }

    @Test
    fun `when stripe extension outdated, then correct labels and illustrations shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.PluginUnsupportedVersion(STRIPE_EXTENSION_GATEWAY)
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
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.WcpayAndStripeActivated
            )

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                WcPayAndStripeInstalledState::class.java
            )
        }

    @Test
    fun `given user is admin, when wcpay and stripe extension active, then open wpadmin button shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.WcpayAndStripeActivated
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as WcPayAndStripeInstalledState
            assertThat(viewStateData.openWPAdminActionClicked != null).isTrue
            assertThat(viewStateData.openWPAdminLabel).isNotNull
        }

    @Test
    fun `when wcpay and stripe extension active, then refresh screen button shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.WcpayAndStripeActivated
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as WcPayAndStripeInstalledState
            assertThat(viewStateData.refreshButtonLabel).isNotNull
        }

    @Test
    fun `when refresh clicked, then loading screen shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.WcpayAndStripeActivated
            )
            val viewModel = createVM()
            val receivedViewStates = mutableListOf<OnboardingViewState>()
            viewModel.viewStateData.observeForever {
                receivedViewStates.add(it)
            }

            (viewModel.viewStateData.value as WcPayAndStripeInstalledState)
                .onRefreshAfterUpdatingClicked.invoke()

            assertThat(receivedViewStates[1]).isEqualTo(LoadingState)
        }

    @Test
    fun `given site is self-hosted, when user taps on Go To Plugin Admin, then generic webview shown`() =
        testBlocking {
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

            (viewModel.viewStateData.value as WcPayAndStripeInstalledState)
                .openWPAdminActionClicked!!.invoke()

            assertThat(viewModel.event.value).isInstanceOf(
                OnboardingEvent.NavigateToUrlInGenericWebView::class.java
            )
        }

    @Test
    fun `given site is wpcom, when user taps on Go To Plugin Admin, then wpcom webview shown`() =
        testBlocking {
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

            (viewModel.viewStateData.value as WcPayAndStripeInstalledState)
                .openWPAdminActionClicked!!.invoke()

            assertThat(viewModel.event.value).isInstanceOf(
                OnboardingEvent.NavigateToUrlInWPComWebView::class.java
            )
        }

    @Test
    fun `given site is atomic, when user taps on Go To Plugin Admin, then wpcom webview shown`() =
        testBlocking {
            whenever(selectedSite.get()).thenReturn(
                SiteModel().apply {
                    setIsWPComAtomic(true)
                }
            )
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.WcpayAndStripeActivated
            )
            val viewModel = createVM()

            (viewModel.viewStateData.value as WcPayAndStripeInstalledState)
                .openWPAdminActionClicked!!.invoke()

            assertThat(viewModel.event.value).isInstanceOf(
                OnboardingEvent.NavigateToUrlInWPComWebView::class.java
            )
        }

    @Test
    fun `when user taps on Go To Plugin Admin, then app navigates to Plugin Admin url`() =
        testBlocking {
            whenever(selectedSite.get()).thenReturn(
                SiteModel().apply {
                    url = DUMMY_SITE_URL
                }
            )

            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.WcpayAndStripeActivated
            )
            val viewModel = createVM()

            (viewModel.viewStateData.value as WcPayAndStripeInstalledState)
                .openWPAdminActionClicked!!.invoke()

            val event = viewModel.event.value as OnboardingEvent.NavigateToUrlInGenericWebView
            assertThat(event.url).isEqualTo(DUMMY_SITE_URL + AppUrls.PLUGIN_MANAGEMENT_SUFFIX)
        }

    @Test
    fun `given user is NOT admin, when wcpay and stripe extension active, then open wpadmin button NOT shown`() =
        testBlocking {
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

            val viewStateData = viewModel.viewStateData.value as WcPayAndStripeInstalledState
            assertThat(viewStateData.openWPAdminLabel).isNull()
            assertThat(viewStateData.openWPAdminActionClicked == null).isTrue
        }

    @Test
    fun `when account rejected, then account rejected state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StripeAccountRejected(mock()))

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value)
                .isInstanceOf(StripeAcountError.StripeAccountRejectedState::class.java)
        }

    @Test
    fun `when incoming account rejected, then account rejected state shown`() =
        testBlocking {
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = CardReaderOnboardingState.StripeAccountRejected(mock()),
                    )
                ).initSavedStateHandle()
            )

            assertThat(viewModel.viewStateData.value)
                .isInstanceOf(StripeAcountError.StripeAccountRejectedState::class.java)
        }

    @Test
    fun `when account pending requirements, then account pending requirements state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    CardReaderOnboardingState.StripeAccountPendingRequirement(
                        0L,
                        WOOCOMMERCE_PAYMENTS,
                        pluginVersion,
                        countryCode
                    )
                )

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                StripeAcountError.StripeAccountPendingRequirementsState::class.java
            )
        }

    @Test
    fun `given account pending requirements and hub, when button clicked, then continues to hub`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    CardReaderOnboardingState.StripeAccountPendingRequirement(
                        0L,
                        WOOCOMMERCE_PAYMENTS,
                        pluginVersion,
                        countryCode
                    )
                )

            val viewModel = createVM()
            (viewModel.viewStateData.value as StripeAcountError.StripeAccountPendingRequirementsState)
                .onButtonActionClicked.invoke()

            assertThat(viewModel.event.value)
                .isInstanceOf(OnboardingEvent.ContinueToHub::class.java)
        }

    @Test
    fun `given account pending requirements and payment, when button clicked, then continues to connection`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    CardReaderOnboardingState.StripeAccountPendingRequirement(
                        0L,
                        WOOCOMMERCE_PAYMENTS,
                        pluginVersion,
                        countryCode
                    )
                )
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    cardReaderOnboardingParam = CardReaderOnboardingParams.Check(
                        CardReaderFlowParam.PaymentOrRefund.Payment(1L)
                    )
                ).initSavedStateHandle()
            )

            (viewModel.viewStateData.value as StripeAcountError.StripeAccountPendingRequirementsState)
                .onButtonActionClicked.invoke()

            assertThat(viewModel.event.value)
                .isInstanceOf(OnboardingEvent.ContinueToConnection::class.java)
        }

    @Test
    fun `given incoming failed status, when button clicked, then continues to connection`() =
        testBlocking {
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    cardReaderOnboardingParam = CardReaderOnboardingParams.Failed(
                        CardReaderFlowParam.PaymentOrRefund.Payment(1L),
                        onboardingState = CardReaderOnboardingState.StripeAccountPendingRequirement(
                            0L,
                            WOOCOMMERCE_PAYMENTS,
                            pluginVersion,
                            countryCode
                        )
                    )
                ).initSavedStateHandle()
            )

            (viewModel.viewStateData.value as StripeAcountError.StripeAccountPendingRequirementsState)
                .onButtonActionClicked.invoke()
            assertThat(viewModel.event.value)
                .isInstanceOf(OnboardingEvent.ContinueToConnection::class.java)
        }

    @Test
    fun `when account pending requirements, then due date not empty`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    CardReaderOnboardingState.StripeAccountPendingRequirement(
                        0L,
                        WOOCOMMERCE_PAYMENTS,
                        pluginVersion,
                        countryCode
                    )
                )

            val viewModel = createVM()

            assertThat(
                (viewModel.viewStateData.value as StripeAcountError.StripeAccountPendingRequirementsState).dueDate
            ).isNotEmpty()
        }

    @Test
    fun `when account overdue requirements, then account overdue requirements state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StripeAccountOverdueRequirement(mock()))

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                StripeAcountError.StripeAccountOverdueRequirementsState::class.java
            )
        }

    @Test
    fun `when incoming account overdue requirements, then account overdue requirements state shown`() =
        testBlocking {
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = CardReaderOnboardingState.StripeAccountOverdueRequirement(mock()),
                    )
                ).initSavedStateHandle()
            )

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                StripeAcountError.StripeAccountOverdueRequirementsState::class.java
            )
        }

    @Test
    fun `when account under review, then account under review state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StripeAccountUnderReview(mock()))

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                StripeAcountError.StripeAccountUnderReviewState::class.java
            )
        }

    @Test
    fun `when incoming account under review, then account under review state shown`() =
        testBlocking {
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = CardReaderOnboardingState.StripeAccountUnderReview(mock()),
                    )
                ).initSavedStateHandle()
            )

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                StripeAcountError.StripeAccountUnderReviewState::class.java
            )
        }

    @Test
    fun `when onboarding check fails, then generic state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.GenericError)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(GenericErrorState::class.java)
        }

    @Test
    fun `when incoming onboarding check fails, then generic state shown`() =
        testBlocking {
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = CardReaderOnboardingState.GenericError,
                    )
                ).initSavedStateHandle()
            )

            assertThat(viewModel.viewStateData.value).isInstanceOf(GenericErrorState::class.java)
        }

    @Test
    fun `when network not available, then no connection error shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.NoConnectionError)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(NoConnectionErrorState::class.java)
        }

    @Test
    fun `when incoming network not available, then no connection error shown`() =
        testBlocking {
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = CardReaderOnboardingState.NoConnectionError,
                    )
                ).initSavedStateHandle()
            )

            assertThat(viewModel.viewStateData.value).isInstanceOf(NoConnectionErrorState::class.java)
        }

    // Tracking Begin
    @Test
    fun `when learn more tapped, then event tracked`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.GenericError)
            val viewModel = createVM()

            (viewModel.viewStateData.value as GenericErrorState).onLearnMoreActionClicked.invoke()

            verify(tracker).trackOnboardingLearnMoreTapped()
        }

    @Test
    fun `when onboarding state checked, then event propagated to tracker`() =
        testBlocking {
            val onboardingState: CardReaderOnboardingState = mock()
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(onboardingState)

            createVM()

            verify(tracker).trackOnboardingState(onboardingState)
        }
    // Tracking End

    private fun createVM(
        savedState: SavedStateHandle = CardReaderOnboardingFragmentArgs(
            cardReaderOnboardingParam = CardReaderOnboardingParams.Check(CardReaderFlowParam.CardReadersHub)
        ).initSavedStateHandle()
    ) =
        CardReaderOnboardingViewModel(
            savedState,
            onboardingChecker,
            tracker,
            userEligibilityFetcher,
            selectedSite,
            appPrefsWrapper,
        )
}
