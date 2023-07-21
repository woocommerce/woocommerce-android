package com.woocommerce.android.ui.payments.cardreader.hub

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.AppUrls
import com.woocommerce.android.AppUrls.STRIPE_TAP_TO_PAY_DEVICE_REQUIREMENTS
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.feedback.FeedbackRepository
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.ui.payments.cardreader.CardReaderTracker
import com.woocommerce.android.ui.payments.cardreader.CashOnDeliverySettingsRepository
import com.woocommerce.android.ui.payments.cardreader.LearnMoreUrlProvider
import com.woocommerce.android.ui.payments.cardreader.LearnMoreUrlProvider.LearnMoreUrlType.CASH_ON_DELIVERY
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubEvents.ShowToast
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubEvents.ShowToastString
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CashOnDeliverySource.PAYMENTS_HUB
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewState.ListItem
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewState.ListItem.GapBetweenSections
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewState.ListItem.HeaderItem
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewState.ListItem.LearnMoreListItem
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewState.ListItem.NonToggleableListItem
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewState.ListItem.ToggleableListItem
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewState.OnboardingErrorAction
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.CardReadersHub
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.CardReadersHub.OpenInHub.NONE
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.CardReadersHub.OpenInHub.TAP_TO_PAY_SUMMARY
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.OnboardingCompleted
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.StripeAccountPendingRequirement
import com.woocommerce.android.ui.payments.taptopay.TapToPayAvailabilityStatus
import com.woocommerce.android.ui.payments.taptopay.isAvailable
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
class CardReaderHubViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val wooStore: WooCommerceStore,
    private val cardReaderChecker: CardReaderOnboardingChecker,
    private val cashOnDeliverySettingsRepository: CashOnDeliverySettingsRepository,
    private val learnMoreUrlProvider: LearnMoreUrlProvider,
    cardReaderCountryConfigProvider: CardReaderCountryConfigProvider,
    private val cardReaderTracker: CardReaderTracker,
    @Named("payment-menu") private val paymentMenuUtmProvider: UtmProvider,
    private val tapToPayAvailabilityStatus: TapToPayAvailabilityStatus,
    private val appPrefs: AppPrefs,
    private val feedbackRepository: FeedbackRepository,
    private val tapToPayUnavailableHandler: CardReaderHubTapToPayUnavailableHandler
) : ScopedViewModel(savedState) {
    private val arguments: CardReaderHubFragmentArgs by savedState.navArgs()
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

    private val initialState
        get() = CardReaderHubViewState(
            rows = createHubListWhenSinglePluginInstalled(
                isOnboardingComplete = false,
                cashOnDeliveryItem = cashOnDeliveryState.value!!
            ),
            isLoading = true,
            onboardingErrorAction = null,
        )
    private val viewState = MutableLiveData(initialState)

    val viewStateData: LiveData<CardReaderHubViewState> = viewState
        .map { state ->
            state.copy(rows = state.rows.sortedBy { it.index })
        }

    init {
        handleOpenInHubParameter()
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
            viewState.value = when (val state = cardReaderChecker.getOnboardingState()) {
                is OnboardingCompleted -> createOnboardingCompleteState()
                is StripeAccountPendingRequirement -> createOnboardingWithPendingRequirementsState(state)
                else -> createOnboardingFailedState(state)
            }
        }
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
        HeaderItem(
            label = UiStringRes(R.string.card_reader_hub_actions_category_header),
            index = 0
        ),
        NonToggleableListItem(
            icon = R.drawable.ic_gridicons_money_on_surface,
            label = UiStringRes(R.string.card_reader_hub_collect_payment),
            index = 1,
            onClick = ::onCollectPaymentClicked
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
        addCardReaderManuals()
        addLearnMoreAboutIPP()
    }

    private fun MutableList<ListItem>.addTapToPay() {
        if (tapToPayAvailabilityStatus().isAvailable) {
            add(GapBetweenSections(index = 4))
            add(
                NonToggleableListItem(
                    icon = R.drawable.ic_baseline_contactless,
                    label = UiStringRes(R.string.card_reader_test_tap_to_pay),
                    description = UiStringRes(R.string.card_reader_tap_to_pay_description),
                    index = 5,
                    onClick = ::onTapToPayClicked,
                    shortDivider = shouldShowTTPFeedbackRequest,
                    iconBadge = R.drawable.ic_badge_new,
                )
            )
            if (shouldShowTTPFeedbackRequest) {
                add(
                    NonToggleableListItem(
                        icon = R.drawable.ic_feedback_banner_logo,
                        label = UiStringRes(R.string.card_reader_tap_to_pay_share_feedback),
                        index = 6,
                        onClick = ::onTapToPayFeedbackClicked
                    )
                )
            }
        }
    }

    private fun MutableList<ListItem>.addCardReaderManuals() {
        if (countryConfig is CardReaderConfigForSupportedCountry) {
            add(
                NonToggleableListItem(
                    icon = R.drawable.ic_card_reader_manual,
                    label = UiStringRes(R.string.settings_card_reader_manuals),
                    index = 10,
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
                index = 11,
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

    private fun createOnboardingCompleteState(): CardReaderHubViewState {
        return CardReaderHubViewState(
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

    private fun createOnboardingFailedState(state: CardReaderOnboardingState): CardReaderHubViewState {
        return CardReaderHubViewState(
            rows = createHubListWhenSinglePluginInstalled(false, cashOnDeliveryState.value!!),
            isLoading = false,
            onboardingErrorAction = OnboardingErrorAction(
                text = UiStringRes(R.string.card_reader_onboarding_not_finished, containsHtml = true),
                onClick = { onOnboardingErrorClicked(state) }
            ),
        )
    }

    private fun onCollectPaymentClicked() {
        trackEvent(AnalyticsEvent.PAYMENTS_HUB_COLLECT_PAYMENT_TAPPED)
        triggerEvent(CardReaderHubEvents.NavigateToPaymentCollectionScreen)
    }

    private fun onManageCardReaderClicked() {
        trackEvent(AnalyticsEvent.PAYMENTS_HUB_MANAGE_CARD_READERS_TAPPED)
        triggerEvent(CardReaderHubEvents.NavigateToCardReaderDetail(arguments.cardReaderFlowParam))
    }

    private fun onPurchaseCardReaderClicked() {
        trackEvent(AnalyticsEvent.PAYMENTS_HUB_ORDER_CARD_READER_TAPPED)
        triggerEvent(
            CardReaderHubEvents.NavigateToPurchaseCardReaderFlow(
                url = paymentMenuUtmProvider.getUrlWithUtmParams(cardReaderPurchaseUrl),
                titleRes = R.string.card_reader_purchase_card_reader
            )
        )
    }

    private fun onLearnMoreCodClicked() {
        cardReaderTracker.trackCashOnDeliveryLearnMoreTapped()
        triggerEvent(
            CardReaderHubEvents.OpenGenericWebView(
                learnMoreUrlProvider.provideLearnMoreUrlFor(
                    CASH_ON_DELIVERY
                )
            )
        )
    }

    private fun onLearnMoreIppClicked() {
        cardReaderTracker.trackIPPLearnMoreClicked(SOURCE)
        triggerEvent(
            CardReaderHubEvents.OpenGenericWebView(
                learnMoreUrlProvider.provideLearnMoreUrlFor(
                    LearnMoreUrlProvider.LearnMoreUrlType.IN_PERSON_PAYMENTS
                )
            )
        )
    }

    private fun onTapToPayClicked() {
        trackEvent(AnalyticsEvent.PAYMENTS_HUB_TAP_TO_PAY_TAPPED)
        triggerEvent(CardReaderHubEvents.NavigateToTapTooPaySummaryScreen)
    }

    private fun onTapToPayFeedbackClicked() {
        trackEvent(AnalyticsEvent.PAYMENTS_HUB_TAP_TO_PAY_FEEDBACK_TAPPED)
        feedbackRepository.saveFeatureFeedback(
            FeatureFeedbackSettings.Feature.TAP_TO_PAY,
            FeatureFeedbackSettings.FeedbackState.GIVEN
        )
        triggerEvent(CardReaderHubEvents.NavigateToTapTooPaySurveyScreen)
    }

    private fun onCardReaderManualsClicked(countryConfig: CardReaderConfigForSupportedCountry) {
        trackEvent(AnalyticsEvent.PAYMENTS_HUB_CARD_READER_MANUALS_TAPPED)
        triggerEvent(CardReaderHubEvents.NavigateToCardReaderManualsScreen(countryConfig))
    }

    private fun onCardReaderPaymentProviderClicked() {
        cardReaderChecker.invalidateCache()
        trackEvent(AnalyticsEvent.SETTINGS_CARD_PRESENT_SELECT_PAYMENT_GATEWAY_TAPPED)
        clearPluginExplicitlySelectedFlag()
        triggerEvent(
            CardReaderHubEvents.NavigateToCardReaderOnboardingScreen(
                CardReaderOnboardingState.ChoosePaymentGatewayProvider
            )
        )
    }

    private fun onCashOnDeliveryToggled(isChecked: Boolean) {
        cardReaderTracker.trackCashOnDeliveryToggled(isChecked)
        launch {
            updateCashOnDeliveryOptionState(
                cashOnDeliveryState.value?.copy(isEnabled = false, isChecked = isChecked)!!
            )
            val result = cashOnDeliverySettingsRepository.toggleCashOnDeliveryOption(isChecked)
            if (!result.isError) {
                if (isChecked) {
                    cardReaderTracker.trackCashOnDeliveryEnabledSuccess(
                        PAYMENTS_HUB
                    )
                } else {
                    cardReaderTracker.trackCashOnDeliveryDisabledSuccess(
                        PAYMENTS_HUB
                    )
                }
                updateCashOnDeliveryOptionState(
                    cashOnDeliveryState.value?.copy(isEnabled = true, isChecked = isChecked)!!
                )
            } else {
                if (isChecked) {
                    cardReaderTracker.trackCashOnDeliveryEnabledFailure(
                        PAYMENTS_HUB,
                        result.error.message
                    )
                } else {
                    cardReaderTracker.trackCashOnDeliveryDisabledFailure(
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
        triggerEvent(CardReaderHubEvents.NavigateToCardReaderOnboardingScreen(state))
    }

    private fun handleOpenInHubParameter() {
        when (val params = arguments.cardReaderFlowParam) {
            is CardReadersHub -> {
                when (params.openInHub) {
                    TAP_TO_PAY_SUMMARY -> {
                        val ttpAvailabilityStatus = tapToPayAvailabilityStatus()
                        if (ttpAvailabilityStatus is TapToPayAvailabilityStatus.Result.NotAvailable) {
                            cardReaderTracker.trackTapToPayNotAvailableReason(
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
                                CardReaderHubEvents.NavigateToTapTooPaySummaryScreen
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
        }.exhaustive
    }

    private fun handlePositiveButtonClickTTPUnavailable(
        actionType: CardReaderHubTapToPayUnavailableHandler.ActionType
    ) = when (actionType) {
        CardReaderHubTapToPayUnavailableHandler.ActionType.PURCHASE_READER -> {
            onPurchaseCardReaderClicked()
        }

        CardReaderHubTapToPayUnavailableHandler.ActionType.TAP_TO_PAY_REQUIREMENTS -> {
            triggerEvent(
                CardReaderHubEvents.OpenGenericWebView(
                    STRIPE_TAP_TO_PAY_DEVICE_REQUIREMENTS
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

    private val shouldShowTTPFeedbackRequest: Boolean
        get() {
            val featureFeedbackSetting = feedbackRepository.getFeatureFeedbackSetting(
                FeatureFeedbackSettings.Feature.TAP_TO_PAY
            )
            return appPrefs.isTTPWasUsedAtLeastOnce() && (
                featureFeedbackSetting.feedbackState == FeatureFeedbackSettings.FeedbackState.UNANSWERED ||
                    !featureFeedbackSetting.isFeedbackGivenMoreThanDaysAgo(SHOW_FEEDBACK_AFTER_USAGE_DAYS)
                )
        }

    sealed class CardReaderHubEvents : MultiLiveEvent.Event() {
        data class NavigateToCardReaderDetail(val cardReaderFlowParam: CardReaderFlowParam) : CardReaderHubEvents()
        data class NavigateToPurchaseCardReaderFlow(
            val url: String,
            @StringRes val titleRes: Int
        ) : CardReaderHubEvents()

        object NavigateToPaymentCollectionScreen : CardReaderHubEvents()
        object NavigateToTapTooPaySummaryScreen : CardReaderHubEvents()
        object NavigateToTapTooPaySurveyScreen : CardReaderHubEvents()
        data class NavigateToCardReaderManualsScreen(
            val countryConfig: CardReaderConfigForSupportedCountry
        ) : CardReaderHubEvents()

        data class NavigateToCardReaderOnboardingScreen(val onboardingState: CardReaderOnboardingState) :
            CardReaderHubEvents()

        data class OpenGenericWebView(val url: String) : CardReaderHubEvents()
        data class ShowToastString(val message: String) : CardReaderHubEvents()
        data class ShowToast(@StringRes val message: Int) : CardReaderHubEvents()
    }

    enum class CashOnDeliverySource {
        ONBOARDING,
        PAYMENTS_HUB
    }

    companion object {
        const val UTM_CAMPAIGN = "payments_menu_item"
        const val UTM_SOURCE = "payments_menu"
        private const val SOURCE = "payments_menu"

        private const val SHOW_FEEDBACK_AFTER_USAGE_DAYS = 30
    }
}
