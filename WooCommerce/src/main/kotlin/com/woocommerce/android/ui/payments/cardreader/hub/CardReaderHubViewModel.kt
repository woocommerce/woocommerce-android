package com.woocommerce.android.ui.payments.cardreader.hub

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.ui.payments.cardreader.CardReaderTracker
import com.woocommerce.android.ui.payments.cardreader.CashOnDeliverySettingsRepository
import com.woocommerce.android.ui.payments.cardreader.LearnMoreUrlProvider
import com.woocommerce.android.ui.payments.cardreader.LearnMoreUrlProvider.LearnMoreUrlType.CASH_ON_DELIVERY
import com.woocommerce.android.ui.payments.cardreader.LearnMoreUrlProvider.LearnMoreUrlType.IN_PERSON_PAYMENTS
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubEvents.NavigateToTapTooPaySummaryScreen
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubEvents.ShowToast
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubEvents.ShowToastString
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubViewState.ListItem
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubViewState.ListItem.HeaderItem
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubViewState.ListItem.NonToggleableListItem
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubViewState.ListItem.ToggleableListItem
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubViewState.OnboardingErrorAction
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CashOnDeliverySource.PAYMENTS_HUB
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.CardReadersHub
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.CardReadersHub.OpenInHub.NONE
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.CardReadersHub.OpenInHub.TAP_TO_PAY_SUMMARY
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.OnboardingCompleted
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.StripeAccountPendingRequirement
import com.woocommerce.android.ui.payments.taptopay.IsTapToPayAvailable
import com.woocommerce.android.ui.payments.taptopay.IsTapToPayAvailable.Result.Available
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
    private val isTapToPayAvailable: IsTapToPayAvailable,
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
            index = 3,
            isChecked = false,
            onToggled = { (::onCashOnDeliveryToggled)(it) },
            onLearnMoreClicked = ::onLearnMoreClicked
        )
    )

    private val learnMoreIppState = CardReaderHubViewState.LearnMoreIppState(
        label = UiStringRes(R.string.card_reader_connect_learn_more, containsHtml = true),
        onClick = ::onLearnMoreIppClicked
    )

    private val viewState = MutableLiveData(
        CardReaderHubViewState(
            rows = createHubListWhenSinglePluginInstalled(
                isOnboardingComplete = false,
                cashOnDeliveryItem = cashOnDeliveryState.value!!
            ),
            isLoading = true,
            onboardingErrorAction = null,
            learnMoreIppState = learnMoreIppState,
        )
    )

    val viewStateData: LiveData<CardReaderHubViewState> = viewState
        .map { state ->
            state.copy(rows = state.rows.sortedBy { it.index })
        }

    init {
        handleOpenInHubParameter()
    }

    private fun onLearnMoreClicked() {
        cardReaderTracker.trackCashOnDeliveryLearnMoreTapped()
        triggerEvent(
            CardReaderHubEvents.OpenGenericWebView(
                learnMoreUrlProvider.provideLearnMoreUrlFor(
                    CASH_ON_DELIVERY
                )
            )
        )
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
            label = UiStringRes(R.string.card_reader_collect_payment),
            index = 1,
            onClick = ::onCollectPaymentClicked
        ),
        cashOnDeliveryItem,
        HeaderItem(
            label = UiStringRes(R.string.card_reader_card_readers_header),
            index = 5,
        ),
        NonToggleableListItem(
            icon = R.drawable.ic_shopping_cart,
            label = UiStringRes(R.string.card_reader_purchase_card_reader),
            index = 6,
            onClick = ::onPurchaseCardReaderClicked
        ),
        NonToggleableListItem(
            icon = R.drawable.ic_manage_card_reader,
            label = UiStringRes(R.string.card_reader_manage_card_reader),
            isEnabled = isOnboardingComplete,
            index = 7,
            onClick = ::onManageCardReaderClicked
        )
    ).apply {
        addCardReaderManuals()
        addTapToPay()
    }

    private fun MutableList<ListItem>.addCardReaderManuals() {
        if (countryConfig is CardReaderConfigForSupportedCountry) {
            add(
                NonToggleableListItem(
                    icon = R.drawable.ic_card_reader_manual,
                    label = UiStringRes(R.string.settings_card_reader_manuals),
                    index = 8,
                    onClick = { onCardReaderManualsClicked(countryConfig) }
                )
            )
        }
    }

    private fun MutableList<ListItem>.addTapToPay() {
        if (isTTPAvailable()) {
            add(
                NonToggleableListItem(
                    icon = R.drawable.ic_baseline_contactless,
                    label = UiStringRes(R.string.card_reader_tap_to_pay),
                    description = UiStringRes(R.string.card_reader_tap_to_pay_description),
                    index = 2,
                    onClick = ::onTapTooPayClicked
                )
            )
        }
    }

    private fun createAdditionalItemWhenMultiplePluginsInstalled() =
        NonToggleableListItem(
            icon = R.drawable.ic_payment_provider,
            label = UiStringRes(R.string.card_reader_manage_payment_provider),
            index = 4,
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
            learnMoreIppState = learnMoreIppState,
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
            learnMoreIppState = learnMoreIppState,
        )
    }

    private fun onLearnMoreIppClicked() {
        cardReaderTracker.trackIPPLearnMoreClicked(LEARN_MORE_SOURCE)
        triggerEvent(
            CardReaderHubEvents.OpenGenericWebView(
                learnMoreUrlProvider.provideLearnMoreUrlFor(
                    IN_PERSON_PAYMENTS
                )
            )
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

    private fun onTapTooPayClicked() {
        triggerEvent(CardReaderHubEvents.NavigateToTapTooPaySummaryScreen)
    }

    private fun onCardReaderManualsClicked(countryConfig: CardReaderConfigForSupportedCountry) {
        trackEvent(AnalyticsEvent.PAYMENTS_HUB_CARD_READER_MANUALS_TAPPED)
        triggerEvent(CardReaderHubEvents.NavigateToCardReaderManualsScreen(countryConfig))
    }

    private fun onCardReaderPaymentProviderClicked() {
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
                        if (isTTPAvailable()) {
                            triggerEvent(NavigateToTapTooPaySummaryScreen)
                        } else {
                            triggerEvent(ShowToast(R.string.card_reader_tap_to_pay_not_available_error))
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

    private fun isTTPAvailable() = storeCountryCode != null &&
        isTapToPayAvailable(storeCountryCode) == Available

    sealed class CardReaderHubEvents : MultiLiveEvent.Event() {
        data class NavigateToCardReaderDetail(val cardReaderFlowParam: CardReaderFlowParam) : CardReaderHubEvents()
        data class NavigateToPurchaseCardReaderFlow(
            val url: String,
            @StringRes val titleRes: Int
        ) : CardReaderHubEvents()

        object NavigateToPaymentCollectionScreen : CardReaderHubEvents()
        object NavigateToTapTooPaySummaryScreen : CardReaderHubEvents()
        data class NavigateToCardReaderManualsScreen(
            val countryConfig: CardReaderConfigForSupportedCountry
        ) : CardReaderHubEvents()

        data class NavigateToCardReaderOnboardingScreen(val onboardingState: CardReaderOnboardingState) :
            CardReaderHubEvents()

        data class OpenGenericWebView(val url: String) : CardReaderHubEvents()
        data class ShowToastString(val message: String) : CardReaderHubEvents()
        data class ShowToast(@StringRes val message: Int) : CardReaderHubEvents()
    }

    data class CardReaderHubViewState(
        val rows: List<ListItem>,
        val isLoading: Boolean,
        val onboardingErrorAction: OnboardingErrorAction?,
        val learnMoreIppState: LearnMoreIppState?,
    ) {
        sealed class ListItem {
            abstract val label: UiString
            abstract val icon: Int?
            abstract val onClick: (() -> Unit)?
            abstract val index: Int
            abstract var isEnabled: Boolean

            data class NonToggleableListItem(
                @DrawableRes override val icon: Int,
                override val label: UiString,
                val description: UiString? = null,
                override var isEnabled: Boolean = true,
                override val index: Int,
                override val onClick: () -> Unit
            ) : ListItem()

            data class ToggleableListItem(
                @DrawableRes override val icon: Int,
                override val label: UiString,
                val description: UiString,
                override var isEnabled: Boolean = true,
                val isChecked: Boolean,
                override val index: Int,
                override val onClick: (() -> Unit)? = null,
                val onToggled: (Boolean) -> Unit,
                val onLearnMoreClicked: () -> Unit
            ) : ListItem()

            data class HeaderItem(
                @DrawableRes override val icon: Int? = null,
                override val label: UiString,
                override val index: Int,
                override var isEnabled: Boolean = false,
                override val onClick: (() -> Unit)? = null
            ) : ListItem()
        }

        data class OnboardingErrorAction(
            val text: UiString?,
            val onClick: () -> Unit,
        )

        data class LearnMoreIppState(
            val label: UiString,
            val onClick: () -> Unit,
        )
    }

    enum class CashOnDeliverySource {
        ONBOARDING,
        PAYMENTS_HUB
    }

    companion object {
        const val UTM_CAMPAIGN = "payments_menu_item"
        const val UTM_SOURCE = "payments_menu"
        const val LEARN_MORE_SOURCE = "payments_menu"
    }
}
