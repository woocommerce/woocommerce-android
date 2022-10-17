package com.woocommerce.android.ui.payments.cardreader.onboarding

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.UiString
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.CardReaderTracker
import com.woocommerce.android.ui.payments.cardreader.LearnMoreUrlProvider
import com.woocommerce.android.ui.payments.cardreader.LearnMoreUrlProvider.LearnMoreUrlType.IN_PERSON_PAYMENTS
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CashOnDeliverySource.ONBOARDING
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.ORDER
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.CashOnDeliveryDisabled
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.ChoosePaymentGatewayProvider
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.GenericError
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.NoConnectionError
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.PluginIsNotSupportedInTheCountry
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.PluginUnsupportedVersion
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.SetupNotCompleted
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.StoreCountryNotSupported
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.StripeAccountCountryNotSupported
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.StripeAccountOverdueRequirement
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.StripeAccountPendingRequirement
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.StripeAccountRejected
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.StripeAccountUnderReview
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.WcpayNotActivated
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.WcpayNotInstalled
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingEvent
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.CashOnDeliveryDisabledState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.GenericErrorState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.LoadingState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.NoConnectionErrorState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.StripeAcountError
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.StripeExtensionError
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.UnsupportedErrorState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.WCPayError
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.WCPayError.WCPayNotInstalledState
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.STRIPE_EXTENSION_GATEWAY
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.gateways.WCGatewayModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient
import org.wordpress.android.fluxc.store.Settings
import org.wordpress.android.fluxc.store.WCGatewayStore
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class CardReaderOnboardingViewModelTest : BaseUnitTest() {
    private val onboardingChecker: CardReaderOnboardingChecker = mock()
    private val tracker: CardReaderTracker = mock()
    private val selectedSite: SelectedSite = mock {
        on(it.get()).thenReturn(SiteModel())
    }
    private val learnMoreUrlProvider: LearnMoreUrlProvider = mock {
        on { provideLearnMoreUrlFor(IN_PERSON_PAYMENTS) }.thenReturn(AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS)
    }
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val cardReaderManager: CardReaderManager = mock()
    private val gatewayStore: WCGatewayStore = mock()
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
                        CardReaderFlowParam.PaymentOrRefund.Payment(1L, ORDER)
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
                .thenReturn(StoreCountryNotSupported(""))

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
                        onboardingState = StoreCountryNotSupported(""),
                    )
                ).initSavedStateHandle()
            )

            verify(onboardingChecker, never()).getOnboardingState()
        }

    @Test
    fun `given country not supported incoming, when view model init, then track onboarding state is called`() =
        testBlocking {
            createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = StoreCountryNotSupported(""),
                    )
                ).initSavedStateHandle()
            )

            verify(tracker).trackOnboardingState(StoreCountryNotSupported(""))
        }

    @Test
    fun `given stripe is not supported in country, when init, then track onboarding state is called`() =
        testBlocking {
            createVM(
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

            verify(tracker).trackOnboardingState(
                PluginIsNotSupportedInTheCountry(
                    STRIPE_EXTENSION_GATEWAY,
                    ""
                )
            )
        }

    @Test
    fun `given wcpay is not supported in country, when init, then track onboarding state is called`() =
        testBlocking {
            createVM(
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

            verify(tracker).trackOnboardingState(
                PluginIsNotSupportedInTheCountry(
                    WOOCOMMERCE_PAYMENTS,
                    ""
                )
            )
        }

    @Test
    fun `given wcpay is installed, when init, then track onboarding state is called`() =
        testBlocking {
            createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = WcpayNotInstalled,
                    )
                ).initSavedStateHandle()
            )

            verify(tracker).trackOnboardingState(
                WcpayNotInstalled
            )
        }

    @Test
    fun `given wcpay unsupported version, when init, then track onboarding state is called`() =
        testBlocking {
            createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = PluginUnsupportedVersion(WOOCOMMERCE_PAYMENTS),
                    )
                ).initSavedStateHandle()
            )

            verify(tracker).trackOnboardingState(
                PluginUnsupportedVersion(WOOCOMMERCE_PAYMENTS)
            )
        }

    @Test
    fun `given stripe unsupported version, when init, then track onboarding state is called`() =
        testBlocking {
            createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = PluginUnsupportedVersion(STRIPE_EXTENSION_GATEWAY),
                    )
                ).initSavedStateHandle()
            )

            verify(tracker).trackOnboardingState(
                PluginUnsupportedVersion(STRIPE_EXTENSION_GATEWAY)
            )
        }

    @Test
    fun `given wcpay not activated, when init, then track onboarding state is called`() =
        testBlocking {
            createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = WcpayNotActivated,
                    )
                ).initSavedStateHandle()
            )

            verify(tracker).trackOnboardingState(
                WcpayNotActivated
            )
        }

    @Test
    fun `given wcpay setup not completed, when init, then track onboarding state is called`() =
        testBlocking {
            createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = SetupNotCompleted(WOOCOMMERCE_PAYMENTS),
                    )
                ).initSavedStateHandle()
            )

            verify(tracker).trackOnboardingState(
                SetupNotCompleted(WOOCOMMERCE_PAYMENTS)
            )
        }

    @Test
    fun `given stripe setup not completed, when init, then track onboarding state is called`() =
        testBlocking {
            createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = SetupNotCompleted(STRIPE_EXTENSION_GATEWAY),
                    )
                ).initSavedStateHandle()
            )

            verify(tracker).trackOnboardingState(
                SetupNotCompleted(STRIPE_EXTENSION_GATEWAY)
            )
        }

    @Test
    fun `given wcpay in test mode with live account, when init, then track onboarding state is called`() =
        testBlocking {
            createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = PluginInTestModeWithLiveStripeAccount(
                            WOOCOMMERCE_PAYMENTS
                        ),
                    )
                ).initSavedStateHandle()
            )

            verify(tracker).trackOnboardingState(
                PluginInTestModeWithLiveStripeAccount(
                    WOOCOMMERCE_PAYMENTS
                )
            )
        }

    @Test
    fun `given stripe in test mode with live account, when init, then track onboarding state is called`() =
        testBlocking {
            createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = PluginInTestModeWithLiveStripeAccount(
                            STRIPE_EXTENSION_GATEWAY
                        ),
                    )
                ).initSavedStateHandle()
            )

            verify(tracker).trackOnboardingState(
                PluginInTestModeWithLiveStripeAccount(
                    STRIPE_EXTENSION_GATEWAY
                )
            )
        }

    @Test
    fun `given stripe account under review, when init, then track onboarding state is called`() =
        testBlocking {
            createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = StripeAccountUnderReview(STRIPE_EXTENSION_GATEWAY),
                    )
                ).initSavedStateHandle()
            )

            verify(tracker).trackOnboardingState(
                StripeAccountUnderReview(STRIPE_EXTENSION_GATEWAY)
            )
        }

    @Test
    fun `given stripe account pending requirement, when init, then track onboarding state is called`() =
        testBlocking {
            createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = StripeAccountPendingRequirement(
                            null,
                            STRIPE_EXTENSION_GATEWAY,
                            null,
                            "US"
                        ),
                    )
                ).initSavedStateHandle()
            )

            verify(tracker).trackOnboardingState(
                StripeAccountPendingRequirement(
                    null,
                    STRIPE_EXTENSION_GATEWAY,
                    null,
                    "US"
                )
            )
        }

    @Test
    fun `given stripe account overdue requirement, when init, then track onboarding state is called`() =
        testBlocking {
            createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = StripeAccountOverdueRequirement(
                            STRIPE_EXTENSION_GATEWAY
                        ),
                    )
                ).initSavedStateHandle()
            )

            verify(tracker).trackOnboardingState(
                StripeAccountOverdueRequirement(
                    STRIPE_EXTENSION_GATEWAY
                ),
            )
        }

    @Test
    fun `given stripe account rejected, when init, then track onboarding state is called`() =
        testBlocking {
            createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = StripeAccountRejected(
                            STRIPE_EXTENSION_GATEWAY
                        ),
                    )
                ).initSavedStateHandle()
            )

            verify(tracker).trackOnboardingState(
                StripeAccountRejected(
                    STRIPE_EXTENSION_GATEWAY
                ),
            )
        }

    @Test
    fun `given generic error, when init, then track onboarding state is called`() =
        testBlocking {
            createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = GenericError,
                    )
                ).initSavedStateHandle()
            )

            verify(tracker).trackOnboardingState(GenericError)
        }

    @Test
    fun `given no connection error, when init, then track onboarding state is called`() =
        testBlocking {
            createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = NoConnectionError,
                    )
                ).initSavedStateHandle()
            )

            verify(tracker).trackOnboardingState(NoConnectionError)
        }

    @Test
    fun `given stripe account country not supported, when init, then track onboarding state is called`() =
        testBlocking {
            createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = StripeAccountCountryNotSupported(WOOCOMMERCE_PAYMENTS, "US"),
                    )
                ).initSavedStateHandle()
            )

            verify(tracker).trackOnboardingState(
                StripeAccountCountryNotSupported(WOOCOMMERCE_PAYMENTS, "US")
            )
        }

    @Test
    fun `given country not supported incoming, when view model init, then country not supported state shown`() =
        testBlocking {
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = StoreCountryNotSupported(""),
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
                        onboardingState = WcpayNotInstalled,
                    )
                ).initSavedStateHandle()
            )

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayNotInstalledState::class.java)
        }

    @Test
    fun `when stripe account country not supported, then country not supported state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(StripeAccountCountryNotSupported(mock(), ""))

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
                        onboardingState = StripeAccountCountryNotSupported(mock(), ""),
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
                .thenReturn(StoreCountryNotSupported("US"))
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
                        onboardingState = StoreCountryNotSupported("US"),
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
                .thenReturn(StoreCountryNotSupported(""))
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
                .thenReturn(StoreCountryNotSupported(""))
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
    fun `given learn more provider returns Stripe, when learn more clicked, then app navigates to Stripe docs`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(StoreCountryNotSupported(""))
            whenever(
                learnMoreUrlProvider.provideLearnMoreUrlFor(
                    IN_PERSON_PAYMENTS
                )
            ).thenReturn(
                AppUrls.STRIPE_LEARN_MORE_ABOUT_PAYMENTS
            )

            val viewModel = createVM()

            (viewModel.viewStateData.value as UnsupportedErrorState.Country).onLearnMoreActionClicked.invoke()

            val event = viewModel.event.value as OnboardingEvent.NavigateToUrlInGenericWebView
            assertThat(event.url).isEqualTo(AppUrls.STRIPE_LEARN_MORE_ABOUT_PAYMENTS)
        }

    @Test
    fun `given learn more provider returns WCPay, when learn more clicked, then app navigates to wcpay docs`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(StoreCountryNotSupported(""))
            whenever(learnMoreUrlProvider.provideLearnMoreUrlFor(IN_PERSON_PAYMENTS))
                .thenReturn(AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS)

            val viewModel = createVM()

            (viewModel.viewStateData.value as UnsupportedErrorState.Country).onLearnMoreActionClicked.invoke()

            val event = viewModel.event.value as OnboardingEvent.NavigateToUrlInGenericWebView
            assertThat(event.url).isEqualTo(AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS)
        }

    @Test
    fun `given stripe account country not supported, when learn more clicked, then app shows learn more section`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(StripeAccountCountryNotSupported(mock(), ""))
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
                .thenReturn(StripeAccountCountryNotSupported(mock(), ""))
            val viewModel = createVM()

            (viewModel.viewStateData.value as UnsupportedErrorState.StripeAccountInUnsupportedCountry)
                .onContactSupportActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(OnboardingEvent.NavigateToSupport::class.java)
        }

    @Test
    fun `when wcpay not installed, then wcpay not installed state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(WcpayNotInstalled)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayNotInstalledState::class.java)
        }

    @Test
    fun `when wcpay not activated, then wcpay not activated state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(WcpayNotActivated)

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
                        onboardingState = WcpayNotActivated
                    )
                ).initSavedStateHandle()
            )

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayError.WCPayNotActivatedState::class.java)
        }

    @Test
    fun `when wcpay not setup, then wcpay not setup state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(SetupNotCompleted(WOOCOMMERCE_PAYMENTS))

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
                        onboardingState = SetupNotCompleted(WOOCOMMERCE_PAYMENTS),
                    )
                ).initSavedStateHandle()
            )

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayError.WCPayNotSetupState::class.java)
        }

    @Test
    fun `when stripe extension not setup, then stripe extension not setup state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(SetupNotCompleted(STRIPE_EXTENSION_GATEWAY))

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                StripeExtensionError.StripeExtensionNotSetupState::class.java
            )
        }

    @Test
    fun `when stripe extension not setup, then correct labels and illustrations shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(SetupNotCompleted(STRIPE_EXTENSION_GATEWAY))

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
                .thenReturn(PluginUnsupportedVersion(WOOCOMMERCE_PAYMENTS))

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayError.WCPayUnsupportedVersionState::class.java)
        }

    @Test
    fun `when wcpay in test mode with live stripe account, then wcpay in test mode state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(PluginInTestModeWithLiveStripeAccount(mock()))

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
                        onboardingState = PluginInTestModeWithLiveStripeAccount(mock()),
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
                PluginUnsupportedVersion(STRIPE_EXTENSION_GATEWAY)
            )

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                StripeExtensionError.StripeExtensionUnsupportedVersionState::class.java
            )
        }

    @Test
    fun `given cash on delivery disabled, when init, then cash on delivery disabled onboarding state is shown`() =
        testBlocking {
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = CashOnDeliveryDisabled(
                            countryCode = countryCode,
                            preferredPlugin = WOOCOMMERCE_PAYMENTS,
                            version = pluginVersion
                        )
                    )
                ).initSavedStateHandle()
            )

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                CashOnDeliveryDisabledState::class.java
            )
        }

    @Test
    fun `given cash on delivery disabled, when init, then cash on delivery disabled tracking state is called`() =
        testBlocking {
            createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = CashOnDeliveryDisabled(
                            countryCode = countryCode,
                            preferredPlugin = WOOCOMMERCE_PAYMENTS,
                            version = pluginVersion
                        ),
                    )
                ).initSavedStateHandle()
            )

            verify(tracker).trackOnboardingState(
                CashOnDeliveryDisabled(
                    countryCode = countryCode,
                    preferredPlugin = WOOCOMMERCE_PAYMENTS,
                    version = pluginVersion
                )
            )
        }

    @Test
    fun `given cash on delivery disabled screen, when skip button clicked, then continues to connection`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    CashOnDeliveryDisabled(
                        countryCode = countryCode,
                        preferredPlugin = WOOCOMMERCE_PAYMENTS,
                        version = pluginVersion
                    )
                )
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    cardReaderOnboardingParam = CardReaderOnboardingParams.Check(
                        CardReaderFlowParam.PaymentOrRefund.Payment(1L, ORDER)
                    )
                ).initSavedStateHandle()
            )

            (viewModel.viewStateData.value as CashOnDeliveryDisabledState).onSkipCashOnDeliveryClicked.invoke()

            assertThat(viewModel.event.value)
                .isInstanceOf(OnboardingEvent.ContinueToConnection::class.java)
        }

    @Test
    fun `given cash on delivery disabled screen, when skip button clicked, then continues to hub`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    CashOnDeliveryDisabled(
                        countryCode = countryCode,
                        preferredPlugin = WOOCOMMERCE_PAYMENTS,
                        version = pluginVersion
                    )
                )
            val viewModel = createVM()

            (viewModel.viewStateData.value as CashOnDeliveryDisabledState).onSkipCashOnDeliveryClicked.invoke()

            assertThat(viewModel.event.value)
                .isInstanceOf(OnboardingEvent.ContinueToHub::class.java)
        }

    @Test
    fun `given cash on delivery disabled screen, when skip button clicked, then store decision in app prefs`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    CashOnDeliveryDisabled(
                        countryCode = countryCode,
                        preferredPlugin = WOOCOMMERCE_PAYMENTS,
                        version = pluginVersion
                    )
                )
            val viewModel = createVM()

            (viewModel.viewStateData.value as CashOnDeliveryDisabledState).onSkipCashOnDeliveryClicked.invoke()

            verify(appPrefsWrapper).setCashOnDeliveryDisabledStateSkipped(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong(),
                eq(true)
            )
        }

    @Test
    fun `given enable cash on delivery clicked, when success, then continue to connection`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    CashOnDeliveryDisabled(
                        countryCode = countryCode,
                        preferredPlugin = WOOCOMMERCE_PAYMENTS,
                        version = pluginVersion
                    )
                )
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    cardReaderOnboardingParam = CardReaderOnboardingParams.Check(
                        CardReaderFlowParam.PaymentOrRefund.Payment(1L, ORDER)
                    )
                ).initSavedStateHandle()
            )

            (viewModel.viewStateData.value as CashOnDeliveryDisabledState).onCashOnDeliveryEnabledSuccessfully.invoke()

            assertThat(viewModel.event.value)
                .isInstanceOf(OnboardingEvent.ContinueToConnection::class.java)
        }

    @Test
    fun `given enable cash on delivery clicked, when success, then continue to hub`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    CashOnDeliveryDisabled(
                        countryCode = countryCode,
                        preferredPlugin = WOOCOMMERCE_PAYMENTS,
                        version = pluginVersion
                    )
                )
            val viewModel = createVM()

            (viewModel.viewStateData.value as CashOnDeliveryDisabledState).onCashOnDeliveryEnabledSuccessfully.invoke()

            assertThat(viewModel.event.value)
                .isInstanceOf(OnboardingEvent.ContinueToHub::class.java)
        }

    @Test
    fun `given enable cash on delivery clicked, when success, then don't store anything in prefs`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    CashOnDeliveryDisabled(
                        countryCode = countryCode,
                        preferredPlugin = WOOCOMMERCE_PAYMENTS,
                        version = pluginVersion
                    )
                )
            val viewModel = createVM()

            (viewModel.viewStateData.value as CashOnDeliveryDisabledState).onCashOnDeliveryEnabledSuccessfully.invoke()

            verify(appPrefsWrapper, never()).setCashOnDeliveryDisabledStateSkipped(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong(),
                eq(true)
            )
        }

    @Test
    fun `given cash on delivery enabled clicked, then show progress`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    CashOnDeliveryDisabled(
                        countryCode = countryCode,
                        preferredPlugin = WOOCOMMERCE_PAYMENTS,
                        version = pluginVersion
                    )
                )
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    cardReaderOnboardingParam = CardReaderOnboardingParams.Check(
                        CardReaderFlowParam.PaymentOrRefund.Payment(1L, ORDER)
                    )
                ).initSavedStateHandle()
            )

            (viewModel.viewStateData.value as CashOnDeliveryDisabledState).onEnableCashOnDeliveryClicked.invoke()

            assertTrue((viewModel.viewStateData.value as CashOnDeliveryDisabledState).shouldShowProgress)
        }

    @Test
    fun `given cash on delivery disabled state, then don't show progress`() =
        testBlocking {
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    CardReaderOnboardingParams.Failed(
                        cardReaderFlowParam = mock(),
                        onboardingState = CashOnDeliveryDisabled(
                            countryCode = countryCode,
                            preferredPlugin = WOOCOMMERCE_PAYMENTS,
                            version = pluginVersion
                        ),
                    )
                ).initSavedStateHandle()
            )

            assertFalse((viewModel.viewStateData.value as CashOnDeliveryDisabledState).shouldShowProgress)
        }

    @Test
    fun `given cash on delivery, when learn more clicked, then app shows learn more section`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    CashOnDeliveryDisabled(
                        countryCode = countryCode,
                        preferredPlugin = WOOCOMMERCE_PAYMENTS,
                        version = pluginVersion
                    )
                )
            val viewModel = createVM()

            (viewModel.viewStateData.value as CashOnDeliveryDisabledState)
                .onLearnMoreActionClicked.invoke()

            val event = viewModel.event.value as OnboardingEvent.NavigateToUrlInGenericWebView
            assertThat(event.url).isEqualTo(AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS)
        }

    @Test
    fun `given cash on delivery enable success, when enable cod clicked, then success state is displayed`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    CashOnDeliveryDisabled(
                        countryCode = countryCode,
                        preferredPlugin = WOOCOMMERCE_PAYMENTS,
                        version = pluginVersion
                    )
                )
            whenever(
                gatewayStore.updatePaymentGateway(
                    site = selectedSite.get(),
                    gatewayId = GatewayRestClient.GatewayId.CASH_ON_DELIVERY,
                    enabled = true,
                    title = "Pay in Person",
                    description = "Pay by card or another accepted payment method",
                    settings = Settings(
                        instructions = "Pay by card or another accepted payment method"
                    )
                )
            ).thenReturn(
                WooResult(
                    model = WCGatewayModel(
                        id = "",
                        title = "",
                        description = "",
                        order = 0,
                        isEnabled = true,
                        methodTitle = "",
                        methodDescription = "",
                        features = emptyList()
                    )
                )
            )
            val viewModel = createVM()

            (viewModel.viewStateData.value as CashOnDeliveryDisabledState)
                .onEnableCashOnDeliveryClicked.invoke()

            assertTrue(
                (viewModel.viewStateData.value as CashOnDeliveryDisabledState).cashOnDeliveryEnabledSuccessfully!!
            )
            assertFalse(
                (viewModel.viewStateData.value as CashOnDeliveryDisabledState).shouldShowProgress
            )
        }

    @Test
    fun `given cash on delivery enable failure, when enable cod clicked, then failure state is displayed`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    CashOnDeliveryDisabled(
                        countryCode = countryCode,
                        preferredPlugin = WOOCOMMERCE_PAYMENTS,
                        version = pluginVersion
                    )
                )
            whenever(
                gatewayStore.updatePaymentGateway(
                    site = selectedSite.get(),
                    gatewayId = GatewayRestClient.GatewayId.CASH_ON_DELIVERY,
                    enabled = true,
                    title = "Pay in Person",
                    description = "Pay by card or another accepted payment method",
                    settings = Settings(
                        instructions = "Pay by card or another accepted payment method"
                    )
                )
            ).thenReturn(
                WooResult(
                    error = WooError(
                        type = WooErrorType.GENERIC_ERROR,
                        original = BaseRequest.GenericErrorType.NETWORK_ERROR
                    )
                )
            )
            val viewModel = createVM()

            (viewModel.viewStateData.value as CashOnDeliveryDisabledState)
                .onEnableCashOnDeliveryClicked.invoke()

            assertFalse(
                (viewModel.viewStateData.value as CashOnDeliveryDisabledState).cashOnDeliveryEnabledSuccessfully!!
            )
            assertFalse(
                (viewModel.viewStateData.value as CashOnDeliveryDisabledState).shouldShowProgress
            )
        }

    @Test
    fun `when cash on delivery disabled state, then confirm header shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CashOnDeliveryDisabled(
                    countryCode = countryCode,
                    preferredPlugin = WOOCOMMERCE_PAYMENTS,
                    version = pluginVersion
                )
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as CashOnDeliveryDisabledState
            assertThat(viewStateData.headerLabel).isNotNull()
        }

    @Test
    fun `when cash on delivery disabled state, then confirm header label correct`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CashOnDeliveryDisabled(
                    countryCode = countryCode,
                    preferredPlugin = WOOCOMMERCE_PAYMENTS,
                    version = pluginVersion
                )
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as CashOnDeliveryDisabledState
            assertThat(viewStateData.headerLabel).isEqualTo(
                UiString.UiStringRes(
                    R.string.card_reader_onboarding_cash_on_delivery_disabled_error_header
                )
            )
        }

    @Test
    fun `when cash on delivery disabled state, then confirm hint shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CashOnDeliveryDisabled(
                    countryCode = countryCode,
                    preferredPlugin = WOOCOMMERCE_PAYMENTS,
                    version = pluginVersion
                )
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as CashOnDeliveryDisabledState
            assertThat(viewStateData.cashOnDeliveryHintLabel).isNotNull()
        }

    @Test
    fun `when cash on delivery disabled state, then confirm hint label correct`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CashOnDeliveryDisabled(
                    countryCode = countryCode,
                    preferredPlugin = WOOCOMMERCE_PAYMENTS,
                    version = pluginVersion
                )
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as CashOnDeliveryDisabledState
            assertThat(viewStateData.cashOnDeliveryHintLabel).isEqualTo(
                UiString.UiStringRes(
                    R.string.card_reader_onboarding_cash_on_delivery_disabled_error_hint
                )
            )
        }

    @Test
    fun `when cash on delivery disabled state, then illustration shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CashOnDeliveryDisabled(
                    countryCode = countryCode,
                    preferredPlugin = WOOCOMMERCE_PAYMENTS,
                    version = pluginVersion
                )
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as CashOnDeliveryDisabledState
            assertThat(viewStateData.cardIllustration).isNotNull()
        }

    @Test
    fun `when cash on delivery disabled state, then correct illustration shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CashOnDeliveryDisabled(
                    countryCode = countryCode,
                    preferredPlugin = WOOCOMMERCE_PAYMENTS,
                    version = pluginVersion
                )
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as CashOnDeliveryDisabledState
            assertThat(viewStateData.cardIllustration).isEqualTo(
                R.drawable.img_products_error
            )
        }

    @Test
    fun `when cash on delivery disabled state, then skip button shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CashOnDeliveryDisabled(
                    countryCode = countryCode,
                    preferredPlugin = WOOCOMMERCE_PAYMENTS,
                    version = pluginVersion
                )
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as CashOnDeliveryDisabledState
            assertThat(viewStateData.skipCashOnDeliveryButtonLabel).isNotNull()
        }

    @Test
    fun `when cash on delivery disabled state, then skip button label is correct`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CashOnDeliveryDisabled(
                    countryCode = countryCode,
                    preferredPlugin = WOOCOMMERCE_PAYMENTS,
                    version = pluginVersion
                )
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as CashOnDeliveryDisabledState
            assertThat(viewStateData.skipCashOnDeliveryButtonLabel).isEqualTo(
                UiString.UiStringRes(
                    R.string.skip
                )
            )
        }

    @Test
    fun `when cash on delivery disabled state, then enable cod button shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CashOnDeliveryDisabled(
                    countryCode = countryCode,
                    preferredPlugin = WOOCOMMERCE_PAYMENTS,
                    version = pluginVersion
                )
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as CashOnDeliveryDisabledState
            assertThat(viewStateData.enableCashOnDeliveryButtonLabel).isNotNull()
        }

    @Test
    fun `when cash on delivery disabled state, then enable cod button label correct`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                CashOnDeliveryDisabled(
                    countryCode = countryCode,
                    preferredPlugin = WOOCOMMERCE_PAYMENTS,
                    version = pluginVersion
                )
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as CashOnDeliveryDisabledState
            assertThat(viewStateData.enableCashOnDeliveryButtonLabel).isEqualTo(
                UiString.UiStringRes(
                    R.string.card_reader_onboarding_cash_on_delivery_disabled_button
                )
            )
        }

    @Test
    fun `when stripe extension outdated, then correct labels and illustrations shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                PluginUnsupportedVersion(STRIPE_EXTENSION_GATEWAY)
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
    fun `when wcpay and stripe extension installed-activated, then payment gateway screen is shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                ChoosePaymentGatewayProvider
            )

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                OnboardingViewState.SelectPaymentPluginState::class.java
            )
        }

    @Test
    fun `when wcpay and stripe extension active, then confirm illustration shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                ChoosePaymentGatewayProvider
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as OnboardingViewState.SelectPaymentPluginState
            assertThat(viewStateData.cardIllustration).isNotNull()
        }

    @Test
    fun `when wcpay and stripe extension active, then confirm header shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                ChoosePaymentGatewayProvider
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as OnboardingViewState.SelectPaymentPluginState
            assertThat(viewStateData.headerLabel).isNotNull()
        }

    @Test
    fun `when wcpay and stripe extension active, then confirm hint shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                ChoosePaymentGatewayProvider
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as OnboardingViewState.SelectPaymentPluginState
            assertThat(viewStateData.choosePluginHintLabel).isNotNull()
        }

    @Test
    fun `when wcpay and stripe active, then wcpay button shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                ChoosePaymentGatewayProvider
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as OnboardingViewState.SelectPaymentPluginState
            assertThat(viewStateData.selectWcPayButtonLabel).isNotNull
        }

    @Test
    fun `when wcpay and stripe active, then stripe button shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                ChoosePaymentGatewayProvider
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as OnboardingViewState.SelectPaymentPluginState
            assertThat(viewStateData.selectStripeButtonLabel).isNotNull
        }

    @Test
    fun `when wcpay and stripe extension active, then confirm payment method button shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                ChoosePaymentGatewayProvider
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as OnboardingViewState.SelectPaymentPluginState
            assertThat(viewStateData.confirmPaymentMethodButtonLabel).isNotNull()
        }

    @Test
    fun `when wcpay and stripe extension active, then correct labels and illustrations shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                ChoosePaymentGatewayProvider
            )

            val viewModel = createVM()

            val state = (
                viewModel.viewStateData.value as OnboardingViewState.SelectPaymentPluginState
                )
            assertThat(state.cardIllustration)
                .describedAs("Check illustration")
                .isEqualTo(
                    R.drawable.ic_credit_card_give
                )
            assertThat(state.headerLabel)
                .describedAs("Check header")
                .isEqualTo(
                    UiString.UiStringRes(
                        R.string.card_reader_onboarding_choose_payment_provider
                    )
                )
            assertThat(state.choosePluginHintLabel)
                .describedAs("Check hint")
                .isEqualTo(
                    UiString.UiStringRes(
                        R.string.card_reader_onboarding_choose_plugin_hint
                    )
                )
            assertThat(state.selectWcPayButtonLabel)
                .describedAs("Check wcpayButtonLabel")
                .isEqualTo(
                    UiString.UiStringRes(
                        R.string.card_reader_onboarding_choose_wcpayment_button
                    )
                )
            assertThat(state.icWcPayLogo)
                .describedAs("Check illustration")
                .isEqualTo(
                    R.drawable.ic_wcpay
                )
            assertThat(state.icCheckmarkWcPay)
                .describedAs("Check illustration")
                .isEqualTo(
                    R.drawable.ic_menu_action_mode_check
                )
            assertThat(state.selectStripeButtonLabel)
                .describedAs("Check wcpayButtonLabel")
                .isEqualTo(
                    UiString.UiStringRes(
                        R.string.card_reader_onboarding_choose_stripe_button
                    )
                )
            assertThat(state.confirmPaymentMethodButtonLabel)
                .describedAs("Check wcpayButtonLabel")
                .isEqualTo(
                    UiString.UiStringRes(
                        R.string.card_reader_onboarding_confirm_payment_method_button
                    )
                )
        }

    @Test
    fun `given wcpay and stripe extension active, when user taps wcpay and confirm button, then load screen shown `() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                ChoosePaymentGatewayProvider
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as OnboardingViewState.SelectPaymentPluginState
            viewStateData.onConfirmPaymentMethodClicked.invoke(WOOCOMMERCE_PAYMENTS)

            assertThat(viewModel.viewStateData.value).isEqualTo(LoadingState)
        }

    @Test
    fun `given plugin type not null, when user selects wcpay and taps confirm button, then clear last known reader`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                ChoosePaymentGatewayProvider
            )
            whenever(cardReaderManager.initialized).thenReturn(true)

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as OnboardingViewState.SelectPaymentPluginState
            viewStateData.onConfirmPaymentMethodClicked.invoke(WOOCOMMERCE_PAYMENTS)

            verify(appPrefsWrapper).removeLastConnectedCardReaderId()
        }

    @Test
    fun `given card reader has inititialized, when user selects wcpay and confirm button, then disconnect reader`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                ChoosePaymentGatewayProvider
            )
            whenever(cardReaderManager.initialized).thenReturn(true)

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as OnboardingViewState.SelectPaymentPluginState
            viewStateData.onConfirmPaymentMethodClicked.invoke(WOOCOMMERCE_PAYMENTS)

            verify(cardReaderManager).disconnectReader()
        }

    @Test
    fun `given plugin type null, when view model init, then do not clear last known reader`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                ChoosePaymentGatewayProvider
            )

            createVM()

            verify(appPrefsWrapper, never()).removeLastConnectedCardReaderId()
        }

    @Test
    fun `given plugin type null, when view model init, then do not disconnect reader`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                ChoosePaymentGatewayProvider
            )

            createVM()

            verify(cardReaderManager, never()).disconnectReader()
        }

    @Test
    fun `given wcpay and stripe extension active, when user taps stripe and confirm button, then load screen shown `() =

        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                ChoosePaymentGatewayProvider
            )

            val viewModel = createVM()

            val viewStateData = viewModel.viewStateData.value as OnboardingViewState.SelectPaymentPluginState
            viewStateData.onConfirmPaymentMethodClicked.invoke(STRIPE_EXTENSION_GATEWAY)

            assertThat(viewModel.viewStateData.value).isEqualTo(LoadingState)
        }

    @Test
    fun `when refresh clicked, then loading screen shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(
                WcpayNotActivated
            )
            val viewModel = createVM()
            val receivedViewStates = mutableListOf<OnboardingViewState>()
            viewModel.viewStateData.observeForever {
                receivedViewStates.add(it)
            }

            (viewModel.viewStateData.value as WCPayError.WCPayNotActivatedState)
                .refreshButtonAction.invoke()

            assertThat(receivedViewStates[1]).isEqualTo(LoadingState)
        }

    @Test
    fun `when account rejected, then account rejected state shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(StripeAccountRejected(mock()))

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
                        onboardingState = StripeAccountRejected(mock()),
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
                    StripeAccountPendingRequirement(
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
                    StripeAccountPendingRequirement(
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
                    StripeAccountPendingRequirement(
                        0L,
                        WOOCOMMERCE_PAYMENTS,
                        pluginVersion,
                        countryCode
                    )
                )
            val viewModel = createVM(
                CardReaderOnboardingFragmentArgs(
                    cardReaderOnboardingParam = CardReaderOnboardingParams.Check(
                        CardReaderFlowParam.PaymentOrRefund.Payment(1L, ORDER)
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
                        CardReaderFlowParam.PaymentOrRefund.Payment(1L, ORDER),
                        onboardingState = StripeAccountPendingRequirement(
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
                    StripeAccountPendingRequirement(
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
                .thenReturn(StripeAccountOverdueRequirement(mock()))

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
                        onboardingState = StripeAccountOverdueRequirement(mock()),
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
                .thenReturn(StripeAccountUnderReview(mock()))

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
                        onboardingState = StripeAccountUnderReview(mock()),
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
                .thenReturn(GenericError)

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
                        onboardingState = GenericError,
                    )
                ).initSavedStateHandle()
            )

            assertThat(viewModel.viewStateData.value).isInstanceOf(GenericErrorState::class.java)
        }

    @Test
    fun `when network not available, then no connection error shown`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(NoConnectionError)

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
                        onboardingState = NoConnectionError,
                    )
                ).initSavedStateHandle()
            )

            assertThat(viewModel.viewStateData.value).isInstanceOf(NoConnectionErrorState::class.java)
        }

    // Tracking Begin
    @Test
    fun `given cod disabled screen, when skip button clicked, then track event`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    CashOnDeliveryDisabled(
                        countryCode = countryCode,
                        preferredPlugin = WOOCOMMERCE_PAYMENTS,
                        version = pluginVersion
                    )
                )
            val viewModel = createVM()

            (viewModel.viewStateData.value as CashOnDeliveryDisabledState).onSkipCashOnDeliveryClicked.invoke()

            verify(tracker).trackOnboardingSkippedState(
                CashOnDeliveryDisabled(
                    countryCode = countryCode,
                    preferredPlugin = WOOCOMMERCE_PAYMENTS,
                    version = pluginVersion
                )
            )
        }

    @Test
    fun `given cod disabled screen, when enable cod button clicked, then track event`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    CashOnDeliveryDisabled(
                        countryCode = countryCode,
                        preferredPlugin = WOOCOMMERCE_PAYMENTS,
                        version = pluginVersion
                    )
                )
            val viewModel = createVM()

            (viewModel.viewStateData.value as CashOnDeliveryDisabledState).onEnableCashOnDeliveryClicked.invoke()

            verify(tracker).trackOnboardingCtaTappedState(
                CashOnDeliveryDisabled(
                    countryCode = countryCode,
                    preferredPlugin = WOOCOMMERCE_PAYMENTS,
                    version = pluginVersion
                )
            )
        }

    @Test
    fun `given cod enable tapped, when success, then track success event`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    CashOnDeliveryDisabled(
                        countryCode = countryCode,
                        preferredPlugin = WOOCOMMERCE_PAYMENTS,
                        version = pluginVersion
                    )
                )
            whenever(
                gatewayStore.updatePaymentGateway(
                    site = selectedSite.get(),
                    gatewayId = GatewayRestClient.GatewayId.CASH_ON_DELIVERY,
                    enabled = true,
                    title = "Pay in Person",
                    description = "Pay by card or another accepted payment method",
                    settings = Settings(
                        instructions = "Pay by card or another accepted payment method"
                    )
                )
            ).thenReturn(
                WooResult(
                    model = WCGatewayModel(
                        id = "",
                        title = "",
                        description = "",
                        order = 0,
                        isEnabled = true,
                        methodTitle = "",
                        methodDescription = "",
                        features = emptyList()
                    )
                )
            )
            val viewModel = createVM()

            (viewModel.viewStateData.value as CashOnDeliveryDisabledState).onEnableCashOnDeliveryClicked.invoke()

            verify(tracker).trackCashOnDeliveryEnabledSuccess(ONBOARDING)
        }

    @Test
    fun `given cod enable tapped, when failure, then track failure event`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(
                    CashOnDeliveryDisabled(
                        countryCode = countryCode,
                        preferredPlugin = WOOCOMMERCE_PAYMENTS,
                        version = pluginVersion
                    )
                )
            whenever(
                gatewayStore.updatePaymentGateway(
                    site = selectedSite.get(),
                    gatewayId = GatewayRestClient.GatewayId.CASH_ON_DELIVERY,
                    enabled = true,
                    title = "Pay in Person",
                    description = "Pay by card or another accepted payment method",
                    settings = Settings(
                        instructions = "Pay by card or another accepted payment method"
                    )
                )
            ).thenReturn(
                WooResult(
                    error = WooError(
                        type = WooErrorType.GENERIC_ERROR,
                        original = BaseRequest.GenericErrorType.NETWORK_ERROR,
                        message = "Enabling COD failed. Please try again later"
                    )
                )
            )
            val viewModel = createVM()

            (viewModel.viewStateData.value as CashOnDeliveryDisabledState).onEnableCashOnDeliveryClicked.invoke()

            verify(tracker).trackCashOnDeliveryEnabledFailure(
                ONBOARDING,
                "Enabling COD failed. Please try again later"
            )
        }

    @Test
    fun `given multiple gateway, when user selects wcpay, then track selected gateway`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(ChoosePaymentGatewayProvider)

            val viewModel = createVM()
            (viewModel.viewStateData.value as OnboardingViewState.SelectPaymentPluginState)
                .onConfirmPaymentMethodClicked.invoke(WOOCOMMERCE_PAYMENTS)

            verify(tracker).trackPaymentGatewaySelected(WOOCOMMERCE_PAYMENTS)
        }

    @Test
    fun `given multiple gateway, when user selects stripe extension, then track selected gateway`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(ChoosePaymentGatewayProvider)

            val viewModel = createVM()
            (viewModel.viewStateData.value as OnboardingViewState.SelectPaymentPluginState)
                .onConfirmPaymentMethodClicked.invoke(STRIPE_EXTENSION_GATEWAY)

            verify(tracker).trackPaymentGatewaySelected(STRIPE_EXTENSION_GATEWAY)
        }

    @Test
    fun `when learn more tapped, then event tracked`() =
        testBlocking {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(GenericError)
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
            learnMoreUrlProvider,
            selectedSite,
            appPrefsWrapper,
            cardReaderManager,
            gatewayStore
        )
}
