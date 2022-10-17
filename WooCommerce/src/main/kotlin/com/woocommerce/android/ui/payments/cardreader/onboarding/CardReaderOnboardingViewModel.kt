package com.woocommerce.android.ui.payments.cardreader.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.extensions.formatToMMMMdd
import com.woocommerce.android.model.UiString
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.CardReaderTracker
import com.woocommerce.android.ui.payments.cardreader.LearnMoreUrlProvider
import com.woocommerce.android.ui.payments.cardreader.LearnMoreUrlProvider.LearnMoreUrlType.IN_PERSON_PAYMENTS
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CashOnDeliverySource.ONBOARDING
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
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingEvent.NavigateToUrlInGenericWebView
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.CashOnDeliveryDisabledState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.GenericErrorState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.NoConnectionErrorState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.StripeAcountError.PluginInTestModeWithLiveAccountState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.StripeAcountError.StripeAccountOverdueRequirementsState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.StripeAcountError.StripeAccountPendingRequirementsState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.StripeAcountError.StripeAccountRejectedState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.StripeAcountError.StripeAccountUnderReviewState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.StripeExtensionError.StripeExtensionNotSetupState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.StripeExtensionError.StripeExtensionUnsupportedVersionState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.UnsupportedErrorState.Country
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.UnsupportedErrorState.StripeAccountInUnsupportedCountry
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.UnsupportedErrorState.StripeInCountry
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.UnsupportedErrorState.WcPayInCountry
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.WCPayError.WCPayNotActivatedState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.WCPayError.WCPayNotInstalledState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.WCPayError.WCPayNotSetupState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.WCPayError.WCPayUnsupportedVersionState
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.STRIPE_EXTENSION_GATEWAY
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
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
    private val cardReaderTracker: CardReaderTracker,
    private val learnMoreUrlProvider: LearnMoreUrlProvider,
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val cardReaderManager: CardReaderManager,
    private val gatewayStore: WCGatewayStore,
) : ScopedViewModel(savedState) {
    private val arguments: CardReaderOnboardingFragmentArgs by savedState.navArgs()

    override val _event = SingleLiveEvent<Event>()
    override val event: LiveData<Event> = _event

    private val viewState = MutableLiveData<OnboardingViewState>()
    val viewStateData: LiveData<OnboardingViewState> = viewState

    init {
        when (val onboardingParam = arguments.cardReaderOnboardingParam) {
            is Check -> refreshState(onboardingParam.pluginType)
            is Failed -> {
                cardReaderTracker.trackOnboardingState(onboardingParam.onboardingState)
                showOnboardingState(onboardingParam.onboardingState)
            }
        }.exhaustive
    }

    private fun refreshState(pluginType: PluginType? = null) {
        launch {
            pluginType?.let {
                disconnectCardReader()
            }
            viewState.value = OnboardingViewState.LoadingState
            val state = cardReaderChecker.getOnboardingState(pluginType)
            cardReaderTracker.trackOnboardingState(state)
            showOnboardingState(state)
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
                continueFlow(state.countryCode)
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
                    WCPayNotInstalledState(::refreshState, ::onLearnMoreClicked)
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
                    WCPayNotActivatedState(::refreshState, ::onLearnMoreClicked)
            is SetupNotCompleted ->
                viewState.value = when (state.preferredPlugin) {
                    WOOCOMMERCE_PAYMENTS ->
                        WCPayNotSetupState(::refreshState, ::onLearnMoreClicked)
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
                    onButtonActionClicked = { onSkipPendingRequirementsClicked(state.countryCode) },
                    dueDate = formatDueDate(state)
                )
            is StripeAccountOverdueRequirement ->
                viewState.value = StripeAccountOverdueRequirementsState(
                    onContactSupportActionClicked = ::onContactSupportClicked,
                    onLearnMoreActionClicked = ::onLearnMoreClicked
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
                    { (::onCashOnDeliveryEnabledSuccessfully)(state.countryCode) },
                    onEnableCashOnDeliveryClicked = {
                        (::onEnableCashOnDeliveryClicked)(
                            state.countryCode,
                            state.preferredPlugin,
                            state.version
                        )
                    },
                    onLearnMoreActionClicked = ::onLearnMoreClicked
                )
        }.exhaustive
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
        cardReaderTracker.trackOnboardingSkippedState(
            CardReaderOnboardingState.CashOnDeliveryDisabled(
                countryCode,
                preferredPlugin,
                version
            )
        )
        continueFlow(countryCode)
    }

    private fun onCashOnDeliveryEnabledSuccessfully(countryCode: String) {
        continueFlow(countryCode)
    }

    @Suppress("LongMethod")
    private fun onEnableCashOnDeliveryClicked(
        countryCode: String,
        preferredPlugin: PluginType,
        version: String? = null
    ) {
        cardReaderTracker.trackOnboardingCtaTappedState(
            CardReaderOnboardingState.CashOnDeliveryDisabled(
                countryCode,
                preferredPlugin,
                version
            )
        )
        viewState.value = CashOnDeliveryDisabledState(
            onSkipCashOnDeliveryClicked = {
                (::onSkipCashOnDeliveryClicked)(
                    countryCode,
                    preferredPlugin,
                    version
                )
            },
            onCashOnDeliveryEnabledSuccessfully = { (::onCashOnDeliveryEnabledSuccessfully)(countryCode) },
            onEnableCashOnDeliveryClicked = {
                (::onEnableCashOnDeliveryClicked)(
                    countryCode,
                    preferredPlugin,
                    version
                )
            },
            onLearnMoreActionClicked = ::onLearnMoreClicked,
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
                cardReaderTracker.trackCashOnDeliveryEnabledSuccess(
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
                        onCashOnDeliveryEnabledSuccessfully = { (::onCashOnDeliveryEnabledSuccessfully)(countryCode) },
                        onEnableCashOnDeliveryClicked = {
                            (::onEnableCashOnDeliveryClicked)(
                                countryCode,
                                preferredPlugin,
                                version
                            )
                        },
                        onLearnMoreActionClicked = ::onLearnMoreClicked,
                        shouldShowProgress = false,
                        cashOnDeliveryEnabledSuccessfully = true
                    )
                )
            } ?: run {
                cardReaderTracker.trackCashOnDeliveryEnabledFailure(
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
                        onCashOnDeliveryEnabledSuccessfully = { (::onCashOnDeliveryEnabledSuccessfully)(countryCode) },
                        onEnableCashOnDeliveryClicked = {
                            (::onEnableCashOnDeliveryClicked)(
                                countryCode,
                                preferredPlugin,
                                version
                            )
                        },
                        onLearnMoreActionClicked = ::onLearnMoreClicked,
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
                OnboardingViewState.SelectPaymentPluginState(
                    onConfirmPaymentMethodClicked = { pluginType ->
                        cardReaderTracker.trackPaymentGatewaySelected(pluginType)
                        (::refreshState)(pluginType)
                    }
                )
        }
    }

    private fun onContactSupportClicked() {
        triggerEvent(OnboardingEvent.NavigateToSupport)
    }

    private fun onLearnMoreClicked() {
        cardReaderTracker.trackOnboardingLearnMoreTapped()
        triggerEvent(NavigateToUrlInGenericWebView(learnMoreUrlProvider.provideLearnMoreUrlFor(IN_PERSON_PAYMENTS)))
    }

    private fun onSkipPendingRequirementsClicked(storeCountryCode: String) {
        continueFlow(storeCountryCode)
    }

    private fun continueFlow(storeCountryCode: String) {
        when (val params = arguments.cardReaderOnboardingParam.cardReaderFlowParam) {
            CardReaderFlowParam.CardReadersHub -> {
                triggerEvent(OnboardingEvent.ContinueToHub(params, storeCountryCode))
            }
            is CardReaderFlowParam.PaymentOrRefund -> {
                triggerEvent(OnboardingEvent.ContinueToConnection(params))
            }
        }.exhaustive
    }

    private fun convertCountryCodeToCountry(countryCode: String?) =
        Locale("", countryCode.orEmpty()).displayName

    private fun formatDueDate(state: CardReaderOnboardingState.StripeAccountPendingRequirement) =
        state.dueDate?.let { Date(it * UNIX_TO_JAVA_TIMESTAMP_OFFSET).formatToMMMMdd() } ?: ""

    sealed class OnboardingEvent : Event() {
        object NavigateToSupport : Event()

        data class NavigateToUrlInWPComWebView(val url: String) : Event()
        data class NavigateToUrlInGenericWebView(val url: String) : Event()

        data class ContinueToHub(val cardReaderFlowParam: CardReaderFlowParam, val storeCountryCode: String) : Event()
        data class ContinueToConnection(val cardReaderFlowParam: CardReaderFlowParam) : Event()
    }

    sealed class OnboardingViewState(@LayoutRes val layoutRes: Int) {
        object LoadingState : OnboardingViewState(R.layout.fragment_card_reader_onboarding_loading) {
            val headerLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_loading)
            val hintLabel: UiString =
                UiString.UiStringRes(R.string.please_wait)

            @DrawableRes
            val illustration: Int = R.drawable.img_hot_air_balloon
        }

        class GenericErrorState(
            val onContactSupportActionClicked: (() -> Unit),
            val onLearnMoreActionClicked: (() -> Unit)
        ) : OnboardingViewState(R.layout.fragment_card_reader_onboarding_generic_error) {
            val contactSupportLabel = UiString.UiStringRes(
                stringRes = R.string.card_reader_onboarding_country_not_supported_contact_support,
                containsHtml = true
            )
            val learnMoreLabel = UiString.UiStringRes(
                stringRes = R.string.card_reader_onboarding_country_not_supported_learn_more,
                containsHtml = true
            )
            val illustration = R.drawable.img_products_error
        }

        data class SelectPaymentPluginState(
            val onConfirmPaymentMethodClicked: ((PluginType) -> Unit),
        ) : OnboardingViewState(R.layout.fragment_card_reader_onboarding_select_payment_gateway) {
            val cardIllustration = R.drawable.ic_credit_card_give
            val headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_choose_payment_provider)
            val choosePluginHintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_choose_plugin_hint)

            val selectWcPayButtonLabel = UiString.UiStringRes(R.string.card_reader_onboarding_choose_wcpayment_button)
            val icWcPayLogo = R.drawable.ic_wcpay
            val icCheckmarkWcPay = R.drawable.ic_menu_action_mode_check
            val selectStripeButtonLabel = UiString.UiStringRes(R.string.card_reader_onboarding_choose_stripe_button)
            val confirmPaymentMethodButtonLabel = UiString
                .UiStringRes(R.string.card_reader_onboarding_confirm_payment_method_button)
        }

        data class CashOnDeliveryDisabledState(
            val onSkipCashOnDeliveryClicked: (() -> Unit),
            val onCashOnDeliveryEnabledSuccessfully: (() -> Unit),
            val onEnableCashOnDeliveryClicked: (() -> Unit),
            val onLearnMoreActionClicked: (() -> Unit),
            val shouldShowProgress: Boolean = false,
            val cashOnDeliveryEnabledSuccessfully: Boolean? = null
        ) : OnboardingViewState(R.layout.fragment_card_reader_onboarding_cod_disabled) {
            val cardIllustration = R.drawable.img_products_error
            val headerLabel = UiString.UiStringRes(
                R.string.card_reader_onboarding_cash_on_delivery_disabled_error_header
            )
            val cashOnDeliveryHintLabel = UiString.UiStringRes(
                R.string.card_reader_onboarding_cash_on_delivery_disabled_error_hint
            )
            val skipCashOnDeliveryButtonLabel = UiString.UiStringRes(
                R.string.skip
            )
            val enableCashOnDeliveryButtonLabel = UiString.UiStringRes(
                R.string.card_reader_onboarding_cash_on_delivery_disabled_button
            )
            val learnMoreLabel = UiString.UiStringRes(
                stringRes = R.string.card_reader_onboarding_country_not_supported_learn_more,
                containsHtml = true
            )
        }

        class NoConnectionErrorState(
            val onRetryButtonActionClicked: (() -> Unit)
        ) : OnboardingViewState(R.layout.fragment_card_reader_onboarding_network_error) {
            val illustration = R.drawable.ic_woo_error_state
        }

        sealed class UnsupportedErrorState(
            val headerLabel: UiString,
        ) : OnboardingViewState(R.layout.fragment_card_reader_onboarding_unsupported) {
            abstract val onContactSupportActionClicked: (() -> Unit)
            abstract val onLearnMoreActionClicked: (() -> Unit)

            val contactSupportLabel = UiString.UiStringRes(
                stringRes = R.string.card_reader_onboarding_country_not_supported_contact_support,
                containsHtml = true
            )
            val learnMoreLabel = UiString.UiStringRes(
                stringRes = R.string.card_reader_onboarding_country_not_supported_learn_more,
                containsHtml = true
            )
            val illustration = R.drawable.img_hot_air_balloon
            val hintLabel = UiString.UiStringRes(
                stringRes = R.string.card_reader_onboarding_country_not_supported_hint
            )

            data class Country(
                val countryDisplayName: String,
                override val onContactSupportActionClicked: (() -> Unit),
                override val onLearnMoreActionClicked: (() -> Unit)
            ) : UnsupportedErrorState(
                headerLabel = UiString.UiStringRes(
                    stringRes = R.string.card_reader_onboarding_country_not_supported_header,
                    params = listOf(UiString.UiStringText(countryDisplayName))
                )
            )

            data class StripeInCountry(
                val countryDisplayName: String,
                override val onContactSupportActionClicked: (() -> Unit),
                override val onLearnMoreActionClicked: (() -> Unit)
            ) : UnsupportedErrorState(
                headerLabel = UiString.UiStringRes(
                    stringRes = R.string.card_reader_onboarding_stripe_unsupported_in_country_header,
                    params = listOf(UiString.UiStringText(countryDisplayName))
                )
            )

            data class WcPayInCountry(
                val countryDisplayName: String,
                override val onContactSupportActionClicked: (() -> Unit),
                override val onLearnMoreActionClicked: (() -> Unit)
            ) : UnsupportedErrorState(
                headerLabel = UiString.UiStringRes(
                    stringRes = R.string.card_reader_onboarding_wcpay_unsupported_in_country_header,
                    params = listOf(UiString.UiStringText(countryDisplayName))
                )
            )

            data class StripeAccountInUnsupportedCountry(
                val countryDisplayName: String,
                override val onContactSupportActionClicked: (() -> Unit),
                override val onLearnMoreActionClicked: (() -> Unit)
            ) : UnsupportedErrorState(
                headerLabel = UiString.UiStringRes(
                    stringRes = R.string.card_reader_onboarding_stripe_account_in_unsupported_country,
                    params = listOf(UiString.UiStringText(countryDisplayName))
                )
            )
        }

        sealed class StripeAcountError(
            val headerLabel: UiString,
            val hintLabel: UiString,
            val buttonLabel: UiString? = null
        ) : OnboardingViewState(R.layout.fragment_card_reader_onboarding_stripe) {
            abstract val onContactSupportActionClicked: (() -> Unit)
            abstract val onLearnMoreActionClicked: (() -> Unit)
            open val onButtonActionClicked: (() -> Unit?)? = null

            @DrawableRes
            val illustration = R.drawable.img_products_error
            val contactSupportLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_contact_support, containsHtml = true)
            val learnMoreLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true)

            data class StripeAccountUnderReviewState(
                override val onContactSupportActionClicked: () -> Unit,
                override val onLearnMoreActionClicked: () -> Unit
            ) : StripeAcountError(
                headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_account_under_review_header),
                hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_account_under_review_hint),
            )

            data class StripeAccountRejectedState(
                override val onContactSupportActionClicked: () -> Unit,
                override val onLearnMoreActionClicked: () -> Unit
            ) : StripeAcountError(
                headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_account_rejected_header),
                hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_account_rejected_hint)
            )

            data class StripeAccountOverdueRequirementsState(
                override val onContactSupportActionClicked: () -> Unit,
                override val onLearnMoreActionClicked: () -> Unit
            ) : StripeAcountError(
                headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_account_overdue_requirements_header),
                hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_account_overdue_requirements_hint)
            )

            data class PluginInTestModeWithLiveAccountState(
                override val onContactSupportActionClicked: () -> Unit,
                override val onLearnMoreActionClicked: () -> Unit
            ) : StripeAcountError(
                headerLabel = UiString
                    .UiStringRes(R.string.card_reader_onboarding_wcpay_in_test_mode_with_live_account_header),
                hintLabel = UiString
                    .UiStringRes(R.string.card_reader_onboarding_wcpay_in_test_mode_with_live_account_hint)
            )

            data class StripeAccountPendingRequirementsState(
                override val onContactSupportActionClicked: () -> Unit,
                override val onLearnMoreActionClicked: () -> Unit,
                override val onButtonActionClicked: () -> Unit,
                val dueDate: String
            ) : StripeAcountError(
                headerLabel = UiString
                    .UiStringRes(R.string.card_reader_onboarding_account_pending_requirements_header),
                hintLabel = UiString.UiStringRes(
                    R.string.card_reader_onboarding_account_pending_requirements_hint,
                    listOf(UiString.UiStringText(dueDate))
                ),
                buttonLabel = UiString.UiStringRes(R.string.skip)
            )
        }

        sealed class WCPayError(
            val headerLabel: UiString,
            val hintLabel: UiString,
            val learnMoreLabel: UiString,
            val refreshButtonLabel: UiString
        ) : OnboardingViewState(R.layout.fragment_card_reader_onboarding_wcpay) {
            abstract val refreshButtonAction: () -> Unit
            abstract val onLearnMoreActionClicked: (() -> Unit)

            @DrawableRes
            val illustration = R.drawable.img_woo_payments

            data class WCPayNotInstalledState(
                override val refreshButtonAction: () -> Unit,
                override val onLearnMoreActionClicked: (() -> Unit)
            ) : WCPayError(
                headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_installed_header),
                hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_installed_hint),
                learnMoreLabel = UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true),
                refreshButtonLabel = UiString
                    .UiStringRes(R.string.card_reader_onboarding_wcpay_not_installed_refresh_button)
            )

            data class WCPayNotActivatedState(
                override val refreshButtonAction: () -> Unit,
                override val onLearnMoreActionClicked: (() -> Unit)
            ) : WCPayError(
                headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_activated_header),
                hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_activated_hint),
                learnMoreLabel = UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true),
                refreshButtonLabel = UiString
                    .UiStringRes(R.string.card_reader_onboarding_wcpay_not_activated_refresh_button)
            )

            data class WCPayNotSetupState(
                override val refreshButtonAction: () -> Unit,
                override val onLearnMoreActionClicked: (() -> Unit)
            ) : WCPayError(
                headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_setup_header),
                hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_setup_hint),
                learnMoreLabel = UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true),
                refreshButtonLabel = UiString
                    .UiStringRes(R.string.card_reader_onboarding_wcpay_not_setup_refresh_button)
            )

            data class WCPayUnsupportedVersionState(
                override val refreshButtonAction: () -> Unit,
                override val onLearnMoreActionClicked: (() -> Unit)
            ) : WCPayError(
                headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_unsupported_version_header),
                hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_unsupported_version_hint),
                learnMoreLabel = UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true),
                refreshButtonLabel = UiString
                    .UiStringRes(R.string.card_reader_onboarding_wcpay_unsupported_version_refresh_button)
            )
        }

        sealed class StripeExtensionError(
            val headerLabel: UiString,
            val hintLabel: UiString,
            val learnMoreLabel: UiString,
            val refreshButtonLabel: UiString
        ) : OnboardingViewState(R.layout.fragment_card_reader_onboarding_wcpay) {
            abstract val refreshButtonAction: () -> Unit
            abstract val onLearnMoreActionClicked: (() -> Unit)

            @DrawableRes
            val illustration = R.drawable.img_stripe_extension

            data class StripeExtensionNotSetupState(
                override val refreshButtonAction: () -> Unit,
                override val onLearnMoreActionClicked: (() -> Unit)
            ) : StripeExtensionError(
                headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_stripe_extension_not_setup_header),
                hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_stripe_extension_not_setup_hint),
                learnMoreLabel = UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true),
                refreshButtonLabel = UiString
                    .UiStringRes(R.string.card_reader_onboarding_wcpay_not_setup_refresh_button)
            )

            data class StripeExtensionUnsupportedVersionState(
                override val refreshButtonAction: () -> Unit,
                override val onLearnMoreActionClicked: (() -> Unit)
            ) : StripeExtensionError(
                headerLabel = UiString.UiStringRes(
                    R.string.card_reader_onboarding_stripe_extension_unsupported_version_header
                ),
                hintLabel = UiString.UiStringRes(
                    R.string.card_reader_onboarding_stripe_extension_unsupported_version_hint
                ),
                learnMoreLabel = UiString.UiStringRes(
                    R.string.card_reader_onboarding_learn_more, containsHtml = true
                ),
                refreshButtonLabel = UiString
                    .UiStringRes(R.string.card_reader_onboarding_wcpay_unsupported_version_refresh_button)
            )
        }
    }
}
