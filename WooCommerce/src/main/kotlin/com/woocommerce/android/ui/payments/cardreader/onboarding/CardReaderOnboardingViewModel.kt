package com.woocommerce.android.ui.payments.cardreader.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.extensions.formatToMMMMdd
import com.woocommerce.android.model.UiString
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.LearnMoreUrlProvider
import com.woocommerce.android.ui.payments.cardreader.LearnMoreUrlProvider.LearnMoreUrlType.IN_PERSON_PAYMENTS
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingEvent.NavigateToUrlInGenericWebView
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingEvent.NavigateToUrlInWPComWebView
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingParams.Check
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingParams.Failed
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.ChoosePaymentGatewayProvider
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.GenericError
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.NoConnectionError
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.OnboardingCompleted
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
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewState.CashOnDeliveryDisabledState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewState.GenericErrorState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewState.NoConnectionErrorState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewState.StripeAccountError.PluginInTestModeWithLiveAccountState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewState.StripeAccountError.StripeAccountOverdueRequirementsState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewState.StripeAccountError.StripeAccountPendingRequirementsState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewState.StripeAccountError.StripeAccountRejectedState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewState.StripeAccountError.StripeAccountUnderReviewState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewState.StripeExtensionError.StripeExtensionNotSetupState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewState.StripeExtensionError.StripeExtensionUnsupportedVersionState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewState.UnsupportedErrorState.Country
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewState.UnsupportedErrorState.StripeAccountInUnsupportedCountry
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewState.UnsupportedErrorState.StripeInCountry
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewState.UnsupportedErrorState.WcPayInCountry
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewState.WCPayError.WCPayNotActivatedState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewState.WCPayError.WCPayNotInstalledState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewState.WCPayError.WCPayNotSetupState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewState.WCPayError.WCPayUnsupportedVersionState
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.STRIPE_EXTENSION_GATEWAY
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import com.woocommerce.android.ui.payments.hub.PaymentsHubViewModel.CashOnDeliverySource.ONBOARDING
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient
import org.wordpress.android.fluxc.store.Settings
import org.wordpress.android.fluxc.store.WCGatewayStore
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val UNIX_TO_JAVA_TIMESTAMP_OFFSET = 1000L

@HiltViewModel
class CardReaderOnboardingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val cardReaderChecker: CardReaderOnboardingChecker,
    private val paymentsFlowTracker: PaymentsFlowTracker,
    private val learnMoreUrlProvider: LearnMoreUrlProvider,
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val cardReaderManager: CardReaderManager,
    private val gatewayStore: WCGatewayStore,
    private val errorClickHandler: CardReaderOnboardingErrorCtaClickHandler,
) : ScopedViewModel(savedState) {
    private val arguments: CardReaderOnboardingFragmentArgs by savedState.navArgs()

    override val _event = SingleLiveEvent<Event>()
    override val event: LiveData<Event> = _event

    private val viewState = MutableLiveData<CardReaderOnboardingViewState>()
    val viewStateData: LiveData<CardReaderOnboardingViewState> = viewState

    init {
        when (val onboardingParam = arguments.cardReaderOnboardingParam) {
            is Check -> refreshState(onboardingParam.pluginType)
            is Failed -> showOnboardingState(onboardingParam.onboardingState)
        }
    }

    private fun refreshState(pluginType: PluginType? = null) {
        launch {
            pluginType?.let {
                disconnectCardReader()
            }
            viewState.value = CardReaderOnboardingViewState.LoadingState
            val state = cardReaderChecker.getOnboardingState(pluginType)
            showOnboardingState(state)
        }
    }

    private fun handleErrorCtaClick(errorType: CardReaderOnboardingCTAErrorType) {
        launch {
            val prevState = viewState.value
            viewState.value = CardReaderOnboardingViewState.LoadingState
            when (val reaction = errorClickHandler(errorType)) {
                CardReaderOnboardingErrorCtaClickHandler.Reaction.Refresh -> refreshState()
                is CardReaderOnboardingErrorCtaClickHandler.Reaction.ShowErrorAndRefresh -> {
                    triggerEvent(Event.ShowUiStringSnackbar(UiString.UiStringText(reaction.message)))
                    refreshState()
                }
                is CardReaderOnboardingErrorCtaClickHandler.Reaction.OpenWpComWebView -> {
                    triggerEvent(NavigateToUrlInWPComWebView(reaction.url))
                    viewState.value = prevState!!
                }

                is CardReaderOnboardingErrorCtaClickHandler.Reaction.OpenGenericWebView -> {
                    triggerEvent(NavigateToUrlInGenericWebView(reaction.url))
                    viewState.value = prevState!!
                }
            }
        }
    }

    private suspend fun disconnectCardReader() {
        if (cardReaderManager.initialized) {
            clearLastKnowReader()
            val disconnectionResult = cardReaderManager.disconnectReader()
            if (!disconnectionResult) {
                WooLog.e(WooLog.T.CARD_READER, "Onboarding: Disconnection from reader has failed")
            }
        }
    }

    private fun clearLastKnowReader() {
        appPrefsWrapper.removeLastConnectedCardReaderId()
    }

    @Suppress("LongMethod", "ComplexMethod")
    private fun showOnboardingState(state: CardReaderOnboardingState) {
        when (state) {
            is OnboardingCompleted -> {
                continueFlow()
            }
            is StoreCountryNotSupported ->
                viewState.value = Country(
                    convertCountryCodeToCountry(state.countryCode),
                    ::onContactSupportClicked,
                    ::onLearnMoreClicked
                )
            is PluginIsNotSupportedInTheCountry ->
                viewState.value = when (state.preferredPlugin) {
                    WOOCOMMERCE_PAYMENTS ->
                        WcPayInCountry(
                            convertCountryCodeToCountry(state.countryCode),
                            ::onContactSupportClicked,
                            ::onLearnMoreClicked
                        )
                    STRIPE_EXTENSION_GATEWAY ->
                        StripeInCountry(
                            convertCountryCodeToCountry(state.countryCode),
                            ::onContactSupportClicked,
                            ::onLearnMoreClicked
                        )
                }
            WcpayNotInstalled ->
                viewState.value =
                    WCPayNotInstalledState(
                        { handleErrorCtaClick(CardReaderOnboardingCTAErrorType.WC_PAY_NOT_INSTALLED) },
                        ::onLearnMoreClicked
                    )
            is PluginUnsupportedVersion ->
                when (state.preferredPlugin) {
                    WOOCOMMERCE_PAYMENTS ->
                        viewState.value =
                            WCPayUnsupportedVersionState(
                                ::refreshState,
                                ::onLearnMoreClicked
                            )
                    STRIPE_EXTENSION_GATEWAY ->
                        viewState.value =
                            StripeExtensionUnsupportedVersionState(
                                ::refreshState, ::onLearnMoreClicked
                            )
                }
            WcpayNotActivated ->
                viewState.value =
                    WCPayNotActivatedState(
                        { handleErrorCtaClick(CardReaderOnboardingCTAErrorType.WC_PAY_NOT_ACTIVATED) },
                        ::onLearnMoreClicked
                    )
            is SetupNotCompleted ->
                viewState.value = when (state.preferredPlugin) {
                    WOOCOMMERCE_PAYMENTS ->
                        WCPayNotSetupState(
                            actionButtonActionPrimary = {
                                handleErrorCtaClick(CardReaderOnboardingCTAErrorType.WC_PAY_NOT_SETUP)
                            },
                            actionButtonActionSecondary = ::refreshState,
                            onLearnMoreActionClicked = ::onLearnMoreClicked
                        )
                    STRIPE_EXTENSION_GATEWAY ->
                        StripeExtensionNotSetupState(
                            ::refreshState, ::onLearnMoreClicked
                        )
                }
            is PluginInTestModeWithLiveStripeAccount ->
                viewState.value = PluginInTestModeWithLiveAccountState(
                    onContactSupportActionClicked = ::onContactSupportClicked,
                    onLearnMoreActionClicked = ::onLearnMoreClicked
                )
            is StripeAccountUnderReview ->
                viewState.value = StripeAccountUnderReviewState(
                    onContactSupportActionClicked = ::onContactSupportClicked,
                    onLearnMoreActionClicked = ::onLearnMoreClicked
                )
            is StripeAccountPendingRequirement ->
                viewState.value = StripeAccountPendingRequirementsState(
                    onContactSupportActionClicked = ::onContactSupportClicked,
                    onLearnMoreActionClicked = ::onLearnMoreClicked,
                    onPrimaryActionClicked = { onSkipPendingRequirementsClicked() },
                    dueDate = formatDueDate(state)
                )
            is StripeAccountOverdueRequirement ->
                viewState.value = StripeAccountOverdueRequirementsState(
                    onContactSupportActionClicked = ::onContactSupportClicked,
                    onLearnMoreActionClicked = ::onLearnMoreClicked,
                    onPrimaryActionClicked = {
                        handleErrorCtaClick(CardReaderOnboardingCTAErrorType.STRIPE_ACCOUNT_OVERDUE_REQUIREMENTS)
                    },
                    onSecondaryActionClicked = ::refreshState
                )
            is StripeAccountRejected ->
                viewState.value = StripeAccountRejectedState(
                    onContactSupportActionClicked = ::onContactSupportClicked,
                    onLearnMoreActionClicked = ::onLearnMoreClicked
                )
            GenericError ->
                viewState.value = GenericErrorState(
                    onContactSupportActionClicked = ::onContactSupportClicked,
                    onLearnMoreActionClicked = ::onLearnMoreClicked
                )
            NoConnectionError ->
                viewState.value = NoConnectionErrorState(
                    onRetryButtonActionClicked = ::refreshState
                )
            is StripeAccountCountryNotSupported ->
                viewState.value = StripeAccountInUnsupportedCountry(
                    convertCountryCodeToCountry(state.countryCode),
                    ::onContactSupportClicked,
                    ::onLearnMoreClicked
                )
            ChoosePaymentGatewayProvider -> updateUiWithSelectPaymentPlugin()
            is CardReaderOnboardingState.CashOnDeliveryDisabled ->
                viewState.value = CashOnDeliveryDisabledState(
                    onSkipCashOnDeliveryClicked = {
                        (::onSkipCashOnDeliveryClicked)(
                            state.countryCode,
                            state.preferredPlugin,
                            state.version
                        )
                    },
                    onCashOnDeliveryEnabledSuccessfully =
                    { (::onCashOnDeliveryEnabledSuccessfully)() },
                    onEnableCashOnDeliveryClicked = {
                        (::onEnableCashOnDeliveryClicked)(
                            state.countryCode,
                            state.preferredPlugin,
                            state.version
                        )
                    },
                    onLearnMoreActionClicked = ::onLearnMoreClicked,
                    onContactSupportActionClicked = ::onContactSupportClicked
                )
        }
    }

    private fun onSkipCashOnDeliveryClicked(
        countryCode: String,
        preferredPlugin: PluginType,
        version: String? = null
    ) {
        val site = selectedSite.get()
        appPrefsWrapper.setCashOnDeliveryDisabledStateSkipped(
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId,
            true
        )
        paymentsFlowTracker.trackOnboardingSkippedState(
            CardReaderOnboardingState.CashOnDeliveryDisabled(
                countryCode,
                preferredPlugin,
                version
            )
        )
        continueFlow()
    }

    private fun onCashOnDeliveryEnabledSuccessfully() {
        continueFlow()
    }

    @Suppress("LongMethod")
    private fun onEnableCashOnDeliveryClicked(
        countryCode: String,
        preferredPlugin: PluginType,
        version: String? = null
    ) {
        paymentsFlowTracker.trackOnboardingCtaTapped(OnboardingCtaReasonTapped.CASH_ON_DELIVERY_TAPPED)
        viewState.value = CashOnDeliveryDisabledState(
            onSkipCashOnDeliveryClicked = {
                (::onSkipCashOnDeliveryClicked)(
                    countryCode,
                    preferredPlugin,
                    version
                )
            },
            onCashOnDeliveryEnabledSuccessfully = { (::onCashOnDeliveryEnabledSuccessfully)() },
            onEnableCashOnDeliveryClicked = {
                (::onEnableCashOnDeliveryClicked)(
                    countryCode,
                    preferredPlugin,
                    version
                )
            },
            onLearnMoreActionClicked = ::onLearnMoreClicked,
            onContactSupportActionClicked = ::onContactSupportClicked,
            shouldShowProgress = true
        )
        launch {
            val result = gatewayStore.updatePaymentGateway(
                site = selectedSite.get(),
                gatewayId = GatewayRestClient.GatewayId.CASH_ON_DELIVERY,
                enabled = true,
                title = "Pay in Person",
                description = "Pay by card or another accepted payment method",
                settings = Settings(instructions = "Pay by card or another accepted payment method"),
            )
            result.model?.let {
                paymentsFlowTracker.trackCashOnDeliveryEnabledSuccess(
                    ONBOARDING
                )
                viewState.postValue(
                    CashOnDeliveryDisabledState(
                        onSkipCashOnDeliveryClicked = {
                            (::onSkipCashOnDeliveryClicked)(
                                countryCode,
                                preferredPlugin,
                                version
                            )
                        },
                        onCashOnDeliveryEnabledSuccessfully = { (::onCashOnDeliveryEnabledSuccessfully)() },
                        onEnableCashOnDeliveryClicked = {
                            (::onEnableCashOnDeliveryClicked)(
                                countryCode,
                                preferredPlugin,
                                version
                            )
                        },
                        onLearnMoreActionClicked = ::onLearnMoreClicked,
                        onContactSupportActionClicked = ::onContactSupportClicked,
                        shouldShowProgress = false,
                        cashOnDeliveryEnabledSuccessfully = true
                    )
                )
            } ?: run {
                paymentsFlowTracker.trackCashOnDeliveryEnabledFailure(
                    ONBOARDING,
                    result.error.message
                )
                viewState.postValue(
                    CashOnDeliveryDisabledState(
                        onSkipCashOnDeliveryClicked = {
                            (::onSkipCashOnDeliveryClicked)(
                                countryCode,
                                preferredPlugin,
                                version
                            )
                        },
                        onCashOnDeliveryEnabledSuccessfully = { (::onCashOnDeliveryEnabledSuccessfully)() },
                        onEnableCashOnDeliveryClicked = {
                            (::onEnableCashOnDeliveryClicked)(
                                countryCode,
                                preferredPlugin,
                                version
                            )
                        },
                        onLearnMoreActionClicked = ::onLearnMoreClicked,
                        onContactSupportActionClicked = ::onContactSupportClicked,
                        shouldShowProgress = false,
                        cashOnDeliveryEnabledSuccessfully = false
                    )
                )
            }
        }
    }

    private fun updateUiWithSelectPaymentPlugin() {
        launch {
            viewState.value =
                CardReaderOnboardingViewState.SelectPaymentPluginState(
                    onConfirmPaymentMethodClicked = { pluginType ->
                        paymentsFlowTracker.trackPaymentGatewaySelected(pluginType)
                        (::refreshState)(pluginType)
                    }
                )
        }
    }

    private fun onContactSupportClicked() {
        triggerEvent(CardReaderOnboardingEvent.NavigateToSupport)
    }

    private fun onLearnMoreClicked() {
        paymentsFlowTracker.trackOnboardingLearnMoreTapped()
        triggerEvent(NavigateToUrlInGenericWebView(learnMoreUrlProvider.provideLearnMoreUrlFor(IN_PERSON_PAYMENTS)))
    }

    private fun onSkipPendingRequirementsClicked() {
        continueFlow()
    }

    private fun continueFlow() {
        when (val params = arguments.cardReaderOnboardingParam.cardReaderFlowParam) {
            is CardReaderFlowParam.CardReadersHub -> {
                triggerEvent(CardReaderOnboardingEvent.ContinueToHub(params))
            }
            is CardReaderFlowParam.PaymentOrRefund -> {
                triggerEvent(
                    CardReaderOnboardingEvent.ContinueToConnection(params, requireNotNull(arguments.cardReaderType))
                )
            }
            is CardReaderFlowParam.WooPosConnection -> {
                error("Unsupported flow param: $params")
            }
        }
    }

    private fun convertCountryCodeToCountry(countryCode: String?) =
        Locale("", countryCode.orEmpty()).displayName

    private fun formatDueDate(state: StripeAccountPendingRequirement) =
        state.dueDate?.let { Date(it * UNIX_TO_JAVA_TIMESTAMP_OFFSET).formatToMMMMdd() }
}
