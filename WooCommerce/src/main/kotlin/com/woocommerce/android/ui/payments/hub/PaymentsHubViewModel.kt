package com.woocommerce.android.ui.payments.hub

import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.AppUrls
import com.woocommerce.android.AppUrls.STRIPE_TAP_TO_PAY_DEVICE_REQUIREMENTS
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateAvailability
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.ui.payments.cardreader.CashOnDeliverySettingsRepository
import com.woocommerce.android.ui.payments.cardreader.ClearCardReaderDataAction
import com.woocommerce.android.ui.payments.cardreader.LearnMoreUrlProvider
import com.woocommerce.android.ui.payments.cardreader.LearnMoreUrlProvider.LearnMoreUrlType.CASH_ON_DELIVERY
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.CardReadersHub
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.CardReadersHub.OpenInHub.NONE
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.CardReadersHub.OpenInHub.TAP_TO_PAY_SUMMARY
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.OnboardingCompleted
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.StripeAccountPendingRequirement
import com.woocommerce.android.ui.payments.hub.PaymentsHubViewModel.CashOnDeliverySource.PAYMENTS_HUB
import com.woocommerce.android.ui.payments.hub.PaymentsHubViewModel.PaymentsHubEvents.ShowToast
import com.woocommerce.android.ui.payments.hub.PaymentsHubViewModel.PaymentsHubEvents.ShowToastString
import com.woocommerce.android.ui.payments.hub.PaymentsHubViewState.ListItem
import com.woocommerce.android.ui.payments.hub.PaymentsHubViewState.ListItem.HeaderItem
import com.woocommerce.android.ui.payments.hub.PaymentsHubViewState.ListItem.LearnMoreListItem
import com.woocommerce.android.ui.payments.hub.PaymentsHubViewState.ListItem.NonToggleableListItem
import com.woocommerce.android.ui.payments.hub.PaymentsHubViewState.ListItem.PayoutSummaryListItem
import com.woocommerce.android.ui.payments.hub.PaymentsHubViewState.ListItem.ToggleableListItem
import com.woocommerce.android.ui.payments.hub.PaymentsHubViewState.OnboardingErrorAction
import com.woocommerce.android.ui.payments.taptopay.TapToPayAvailabilityStatus
import com.woocommerce.android.ui.payments.taptopay.isAvailable
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.ui.prefs.developer.DeveloperOptionsRepository
import com.woocommerce.android.util.UtmProvider
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.CARD_READER
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class PaymentsHubViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val wooStore: WooCommerceStore,
    private val cardReaderChecker: CardReaderOnboardingChecker,
    private val cashOnDeliverySettingsRepository: CashOnDeliverySettingsRepository,
    private val learnMoreUrlProvider: LearnMoreUrlProvider,
    cardReaderCountryConfigProvider: CardReaderCountryConfigProvider,
    private val paymentsFlowTracker: PaymentsFlowTracker,
    @Named("payment-menu") private val paymentMenuUtmProvider: UtmProvider,
    private val tapToPayAvailabilityStatus: TapToPayAvailabilityStatus,
    private val tapToPayUnavailableHandler: PaymentsHubTapToPayUnavailableHandler,
    private val cardReaderDataAction: ClearCardReaderDataAction,
    private val cardReaderManager: CardReaderManager,
    private val developerOptionsRepository: DeveloperOptionsRepository,
) : ScopedViewModel(savedState) {
    private val arguments: PaymentsHubFragmentArgs by savedState.navArgs()
    private val storeCountryCode = wooStore.getStoreCountryCode(selectedSite.get())
    private val countryConfig = cardReaderCountryConfigProvider.provideCountryConfigFor(
        storeCountryCode
    )
    private val cashOnDeliveryState = MutableLiveData(
        ToggleableListItem(
            icon = R.drawable.ic_gridicons_credit_card,
            label = UiStringRes(R.string.card_reader_enable_pay_in_person),
            description = UiStringRes(
                R.string.card_reader_enable_pay_in_person_description,
                containsHtml = true
            ),
            index = 2,
            isChecked = false,
            onToggled = { (::onCashOnDeliveryToggled)(it) },
            onLearnMoreClicked = ::onLearnMoreCodClicked
        )
    )

    private fun listenForSoftwareUpdateAvailability() {
        launch {
            cardReaderManager.softwareUpdateAvailability.collect(
                ::handleSoftwareUpdateAvailability
            )
        }
    }

    private fun handleSoftwareUpdateAvailability(updateStatus: SoftwareUpdateAvailability) {
        val readerStatus = cardReaderManager.readerStatus.value
        if (readerStatus !is CardReaderStatus.Connected) return
        when (updateStatus) {
            SoftwareUpdateAvailability.Available -> {
                paymentsFlowTracker.trackSoftwareUpdateAlertShown()
                triggerEvent(
                    PaymentsHubEvents.CardReaderUpdateAvailable(
                        message = R.string.card_reader_payment_update_available,
                        onClick = {
                            paymentsFlowTracker.trackSoftwareUpdateAlertInstallClicked()
                            triggerEvent(PaymentsHubEvents.CardReaderUpdateScreen)
                        }
                    )
                )
            }
            SoftwareUpdateAvailability.NotAvailable -> {
                // no op
            }
        }
    }

    private val initialState
        get() = PaymentsHubViewState(
            rows = createHubListWhenSinglePluginInstalled(
                isOnboardingComplete = false,
                cashOnDeliveryItem = cashOnDeliveryState.value!!
            ),
            isLoading = true,
            onboardingErrorAction = null,
        )
    private val viewState = MutableLiveData(initialState)

    val viewStateData: LiveData<PaymentsHubViewState> = viewState
        .map { state ->
            state.copy(rows = state.rows.sortedBy { it.index })
        }

    init {
        // Must run before anything below reads Tap to Pay availability — Stripe cannot answer
        // whether the device supports it until Terminal is initialized.
        initializeCardReaderManagerIfNeeded()
        handleOpenInHubParameter()
        listenForSoftwareUpdateAvailability()
    }

    private suspend fun checkAndUpdateCashOnDeliveryOptionState() {
        val isCashOnDeliveryEnabled = cashOnDeliverySettingsRepository.isCashOnDeliveryEnabled()
        updateCashOnDeliveryOptionState(
            cashOnDeliveryState.value?.copy(
                isChecked = isCashOnDeliveryEnabled
            )!!
        )
    }

    fun onViewVisible() {
        viewState.value = initialState
        launch {
            checkAndUpdateCashOnDeliveryOptionState()
        }
        launch {
            val state = cardReaderChecker.getOnboardingState()
            viewState.value = when (state) {
                is OnboardingCompleted -> createOnboardingCompleteState()
                is StripeAccountPendingRequirement -> createOnboardingWithPendingRequirementsState(state)
                else -> createOnboardingFailedState(state)
            }
        }
    }

    private fun initializeCardReaderManagerIfNeeded() {
        if (cardReaderManager.initialized) return
        cardReaderManager.initialize(
            updateFrequency = developerOptionsRepository.getUpdateSimulatedReaderOption(),
            useInterac = developerOptionsRepository.isInteracPaymentEnabled(),
            useEftpos = developerOptionsRepository.isEftposPaymentEnabled(),
            isDebug = BuildConfig.DEBUG,
        )
    }

    private val cardReaderPurchaseUrl: String by lazy {
        val storeCountryCode = wooStore.getStoreCountryCode(selectedSite.get()) ?: null.also {
            WooLog.e(CARD_READER, "Store's country code not found.")
        }
        "${AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}$storeCountryCode"
    }

    private fun createHubListWhenSinglePluginInstalled(
        isOnboardingComplete: Boolean,
        cashOnDeliveryItem: ToggleableListItem
    ): List<ListItem> = mutableListOf(
        PayoutSummaryListItem(index = 0),
        HeaderItem(
            label = UiStringRes(R.string.card_reader_settings_header),
            index = 1,
        ),
        cashOnDeliveryItem,
        HeaderItem(
            label = UiStringRes(R.string.card_reader_card_readers_header),
            index = 7,
        ),
        NonToggleableListItem(
            icon = R.drawable.ic_shopping_cart,
            label = UiStringRes(R.string.card_reader_purchase_card_reader),
            index = 8,
            onClick = ::onPurchaseCardReaderClicked
        ),
        NonToggleableListItem(
            icon = R.drawable.ic_manage_card_reader,
            label = UiStringRes(R.string.card_reader_manage_card_reader),
            isEnabled = isOnboardingComplete,
            index = 9,
            onClick = ::onManageCardReaderClicked
        )
    ).apply {
        addTapToPay()
        addCardReaderMode()
        addCardReaderManuals()
        addLearnMoreAboutIPP()
    }

    private val isPhoneEligibleAsCardReader: Boolean
        get() = tapToPayAvailabilityStatus().isAvailable

    private fun MutableList<ListItem>.addTapToPay() {
        val status = tapToPayAvailabilityStatus()
        when {
            status.isAvailable -> {
                add(
                    HeaderItem(
                        label = UiStringRes(R.string.card_reader_tap_to_pay_header),
                        index = 4
                    )
                )
                add(
                    NonToggleableListItem(
                        icon = R.drawable.ic_baseline_contactless,
                        label = UiStringRes(R.string.card_reader_test_tap_to_pay),
                        description = UiStringRes(R.string.card_reader_tap_to_pay_description),
                        index = 5,
                        onClick = ::onTapToPayClicked,
                        iconBadge = R.drawable.ic_badge_new,
                    )
                )
                add(
                    NonToggleableListItem(
                        icon = R.drawable.ic_tintable_info_outline_24dp,
                        label = UiStringRes(R.string.card_reader_about_tap_to_pay),
                        index = 6,
                        onClick = { onAboutTTPClicked(countryConfig as CardReaderConfigForSupportedCountry) },
                    )
                )
            }

            status is TapToPayAvailabilityStatus.Result.NotAvailable.DeviceNotSupported -> {
                add(
                    HeaderItem(
                        label = UiStringRes(R.string.card_reader_tap_to_pay_header),
                        index = 4
                    )
                )
                add(
                    NonToggleableListItem(
                        icon = R.drawable.ic_tintable_info_outline_24dp,
                        label = UiStringRes(R.string.card_reader_about_tap_to_pay),
                        index = 6,
                        onClick = { onTapToPayUnavailableClicked(status) },
                    )
                )
            }
        }
    }

    private fun onTapToPayUnavailableClicked(status: TapToPayAvailabilityStatus.Result.NotAvailable) {
        trackEvent(AnalyticsEvent.PAYMENTS_HUB_TAP_TO_PAY_ABOUT_TAPPED)
        paymentsFlowTracker.trackTapToPayNotAvailableReason(status, SOURCE)
        tapToPayUnavailableHandler.handleTTPUnavailable(
            status,
            ::triggerEvent,
        ) { actionType -> handlePositiveButtonClickTTPUnavailable(actionType) }
    }

    private fun MutableList<ListItem>.addCardReaderMode() {
        if (!isPhoneEligibleAsCardReader) return
        add(
            NonToggleableListItem(
                icon = R.drawable.ic_smartphone_24dp,
                label = UiStringRes(R.string.card_reader_mode_settings_row_label),
                index = 10,
                onClick = ::onCardReaderModeClicked,
            )
        )
    }

    private fun onCardReaderModeClicked() {
        triggerEvent(PaymentsHubEvents.NavigateToCardReaderMode)
    }

    private fun MutableList<ListItem>.addCardReaderManuals() {
        if (countryConfig is CardReaderConfigForSupportedCountry) {
            add(
                NonToggleableListItem(
                    icon = R.drawable.ic_card_reader_manual,
                    label = UiStringRes(R.string.settings_card_reader_manuals),
                    index = 11,
                    onClick = { onCardReaderManualsClicked(countryConfig) }
                )
            )
        }
    }

    private fun MutableList<ListItem>.addLearnMoreAboutIPP() {
        add(
            LearnMoreListItem(
                icon = R.drawable.ic_info_outline_20dp,
                label = UiStringRes(R.string.card_reader_detail_learn_more, containsHtml = true),
                index = 12,
                onClick = ::onLearnMoreIppClicked
            )
        )
    }

    private fun createAdditionalItemWhenMultiplePluginsInstalled() =
        NonToggleableListItem(
            icon = R.drawable.ic_payment_provider,
            label = UiStringRes(R.string.card_reader_manage_payment_provider),
            index = 3,
            onClick = ::onCardReaderPaymentProviderClicked
        )

    private fun updateCashOnDeliveryOptionState(cashOnDeliveryListItem: ToggleableListItem) {
        cashOnDeliveryState.value = cashOnDeliveryListItem
        viewState.value = viewState.value?.copy(
            rows = (getNonTogggleableItems()!! + cashOnDeliveryListItem)
        )
    }

    private fun getNonTogggleableItems(): List<ListItem>? {
        return viewState.value?.rows?.filter {
            it !is ToggleableListItem
        }
    }

    private fun createOnboardingCompleteState(): PaymentsHubViewState {
        return PaymentsHubViewState(
            rows = if (isCardReaderPluginExplicitlySelected()) {
                (
                    createHubListWhenSinglePluginInstalled(true, cashOnDeliveryState.value!!) +
                        createAdditionalItemWhenMultiplePluginsInstalled()
                    )
            } else {
                createHubListWhenSinglePluginInstalled(true, cashOnDeliveryState.value!!)
            },
            isLoading = false,
            onboardingErrorAction = null,
        )
    }

    private fun createOnboardingWithPendingRequirementsState(state: CardReaderOnboardingState) =
        createOnboardingCompleteState().copy(
            onboardingErrorAction = OnboardingErrorAction(
                text = UiStringRes(R.string.card_reader_onboarding_with_pending_requirements, containsHtml = true),
                onClick = { onOnboardingErrorClicked(state) }
            )
        )

    private fun createOnboardingFailedState(state: CardReaderOnboardingState): PaymentsHubViewState {
        return PaymentsHubViewState(
            rows = createHubListWhenSinglePluginInstalled(false, cashOnDeliveryState.value!!),
            isLoading = false,
            onboardingErrorAction = OnboardingErrorAction(
                text = UiStringRes(R.string.card_reader_onboarding_not_finished, containsHtml = true),
                onClick = { onOnboardingErrorClicked(state) }
            ),
        )
    }

    private fun onManageCardReaderClicked() {
        trackEvent(AnalyticsEvent.PAYMENTS_HUB_MANAGE_CARD_READERS_TAPPED)
        triggerEvent(PaymentsHubEvents.NavigateToCardReaderDetail(arguments.cardReaderFlowParam))
    }

    private fun onPurchaseCardReaderClicked() {
        trackEvent(AnalyticsEvent.PAYMENTS_HUB_ORDER_CARD_READER_TAPPED)
        triggerEvent(
            MultiLiveEvent.Event.LaunchUrlInAuthenticatedWebView(
                url = paymentMenuUtmProvider.getUrlWithUtmParams(cardReaderPurchaseUrl),
                screenTitle = UiStringRes(R.string.card_reader_purchase_card_reader)
            )
        )
    }

    private fun onLearnMoreCodClicked() {
        paymentsFlowTracker.trackCashOnDeliveryLearnMoreTapped()
        triggerEvent(
            MultiLiveEvent.Event.LaunchUrlInChromeTab(
                learnMoreUrlProvider.provideLearnMoreUrlFor(
                    CASH_ON_DELIVERY
                )
            )
        )
    }

    private fun onLearnMoreIppClicked() {
        paymentsFlowTracker.trackIPPLearnMoreClicked(SOURCE)
        triggerEvent(
            MultiLiveEvent.Event.LaunchUrlInChromeTab(
                learnMoreUrlProvider.provideLearnMoreUrlFor(
                    LearnMoreUrlProvider.LearnMoreUrlType.IN_PERSON_PAYMENTS
                )
            )
        )
    }

    private fun onTapToPayClicked() {
        trackEvent(AnalyticsEvent.PAYMENTS_HUB_TAP_TO_PAY_TAPPED)
        triggerEvent(PaymentsHubEvents.NavigateToTapToPaySummaryScreen)
    }

    private fun onAboutTTPClicked(countryConfig: CardReaderConfigForSupportedCountry) {
        trackEvent(AnalyticsEvent.PAYMENTS_HUB_TAP_TO_PAY_ABOUT_TAPPED)
        triggerEvent(PaymentsHubEvents.NavigateToAboutTapToPay(countryConfig))
    }

    private fun onCardReaderManualsClicked(countryConfig: CardReaderConfigForSupportedCountry) {
        trackEvent(AnalyticsEvent.PAYMENTS_HUB_CARD_READER_MANUALS_TAPPED)
        triggerEvent(PaymentsHubEvents.NavigateToCardReaderManualsScreen(countryConfig))
    }

    private fun onCardReaderPaymentProviderClicked() {
        disconnectCardReader()
        trackEvent(AnalyticsEvent.SETTINGS_CARD_PRESENT_SELECT_PAYMENT_GATEWAY_TAPPED)
        clearPluginExplicitlySelectedFlag()
        triggerEvent(
            PaymentsHubEvents.NavigateToCardReaderOnboardingScreen(
                CardReaderOnboardingState.ChoosePaymentGatewayProvider
            )
        )
    }

    private fun disconnectCardReader() {
        launch {
            cardReaderDataAction()
        }
    }

    private fun onCashOnDeliveryToggled(isChecked: Boolean) {
        paymentsFlowTracker.trackCashOnDeliveryToggled(isChecked)
        launch {
            updateCashOnDeliveryOptionState(
                cashOnDeliveryState.value?.copy(isEnabled = false, isChecked = isChecked)!!
            )
            val result = cashOnDeliverySettingsRepository.toggleCashOnDeliveryOption(isChecked)
            if (!result.isError) {
                if (isChecked) {
                    paymentsFlowTracker.trackCashOnDeliveryEnabledSuccess(
                        PAYMENTS_HUB
                    )
                } else {
                    paymentsFlowTracker.trackCashOnDeliveryDisabledSuccess(
                        PAYMENTS_HUB
                    )
                }
                updateCashOnDeliveryOptionState(
                    cashOnDeliveryState.value?.copy(isEnabled = true, isChecked = isChecked)!!
                )
            } else {
                if (isChecked) {
                    paymentsFlowTracker.trackCashOnDeliveryEnabledFailure(
                        PAYMENTS_HUB,
                        result.error.message
                    )
                } else {
                    paymentsFlowTracker.trackCashOnDeliveryDisabledFailure(
                        PAYMENTS_HUB,
                        result.error.message
                    )
                }
                updateCashOnDeliveryOptionState(
                    cashOnDeliveryState.value?.copy(isEnabled = true, isChecked = !isChecked)!!
                )
                if (result.error.message.isNullOrEmpty()) {
                    triggerEvent(ShowToast(R.string.something_went_wrong_try_again))
                } else {
                    triggerEvent(
                        ShowToastString(result.error.message!!)
                    )
                }
            }
        }
    }

    private fun onOnboardingErrorClicked(state: CardReaderOnboardingState) {
        trackEvent(AnalyticsEvent.PAYMENTS_HUB_ONBOARDING_ERROR_TAPPED)
        triggerEvent(PaymentsHubEvents.NavigateToCardReaderOnboardingScreen(state))
    }

    private fun handleOpenInHubParameter() {
        when (val params = arguments.cardReaderFlowParam) {
            is CardReadersHub -> {
                when (params.openInHub) {
                    TAP_TO_PAY_SUMMARY -> {
                        val ttpAvailabilityStatus = tapToPayAvailabilityStatus()
                        if (ttpAvailabilityStatus is TapToPayAvailabilityStatus.Result.NotAvailable) {
                            paymentsFlowTracker.trackTapToPayNotAvailableReason(
                                ttpAvailabilityStatus,
                                SOURCE,
                            )
                            tapToPayUnavailableHandler.handleTTPUnavailable(
                                ttpAvailabilityStatus,
                                ::triggerEvent
                            ) { actionType ->
                                handlePositiveButtonClickTTPUnavailable(actionType)
                            }
                        } else {
                            triggerEvent(
                                PaymentsHubEvents.NavigateToTapToPaySummaryScreen
                            )
                        }
                    }
                    NONE -> {
                        // no-op
                    }
                }
            }
            is PaymentOrRefund -> {
                // no-op
            }
            CardReaderFlowParam.WooPosConnection -> {
                // no-op
            }
        }
    }

    private fun handlePositiveButtonClickTTPUnavailable(
        actionType: PaymentsHubTapToPayUnavailableHandler.ActionType
    ) = when (actionType) {
        PaymentsHubTapToPayUnavailableHandler.ActionType.PURCHASE_READER -> {
            onPurchaseCardReaderClicked()
        }

        PaymentsHubTapToPayUnavailableHandler.ActionType.TAP_TO_PAY_REQUIREMENTS -> {
            triggerEvent(
                MultiLiveEvent.Event.LaunchUrlInChromeTab(
                    STRIPE_TAP_TO_PAY_DEVICE_REQUIREMENTS
                )
            )
        }

        PaymentsHubTapToPayUnavailableHandler.ActionType.TAP_TO_PAY_LEARN_MORE -> {
            triggerEvent(
                MultiLiveEvent.Event.LaunchUrlInChromeTab(
                    AppUrls.LEARN_MORE_ABOUT_TAP_TO_PAY
                )
            )
        }
    }

    private fun trackEvent(event: AnalyticsEvent) {
        analyticsTrackerWrapper.track(event)
    }

    private fun clearPluginExplicitlySelectedFlag() {
        val site = selectedSite.get()
        appPrefsWrapper.setIsCardReaderPluginExplicitlySelectedFlag(
            site.id,
            site.siteId,
            site.selfHostedSiteId,
            false
        )
    }

    private fun isCardReaderPluginExplicitlySelected() =
        appPrefsWrapper.isCardReaderPluginExplicitlySelected(
            localSiteId = selectedSite.get().id,
            remoteSiteId = selectedSite.get().siteId,
            selfHostedSiteId = selectedSite.get().selfHostedSiteId,
        )

    sealed class PaymentsHubEvents : MultiLiveEvent.Event() {
        data class NavigateToCardReaderDetail(val cardReaderFlowParam: CardReaderFlowParam) : PaymentsHubEvents()

        data object NavigateToTapToPaySummaryScreen : PaymentsHubEvents()
        data class NavigateToCardReaderManualsScreen(
            val countryConfig: CardReaderConfigForSupportedCountry
        ) : PaymentsHubEvents()

        data class NavigateToCardReaderOnboardingScreen(val onboardingState: CardReaderOnboardingState) :
            PaymentsHubEvents()

        data class NavigateToAboutTapToPay(
            val countryConfig: CardReaderConfigForSupportedCountry
        ) : PaymentsHubEvents()

        data class ShowToastString(val message: String) : PaymentsHubEvents()
        data class ShowToast(@StringRes val message: Int) : PaymentsHubEvents()

        data class CardReaderUpdateAvailable(
            val message: Int,
            val onClick: View.OnClickListener,
        ) : PaymentsHubEvents()

        object CardReaderUpdateScreen : PaymentsHubEvents()

        data object NavigateToCardReaderMode : PaymentsHubEvents()
    }

    enum class CashOnDeliverySource {
        ONBOARDING,
        PAYMENTS_HUB
    }

    companion object {
        const val UTM_CAMPAIGN = "payments_menu_item"
        const val UTM_SOURCE = "payments_menu"
        private const val SOURCE = "payments_menu"
    }
}
