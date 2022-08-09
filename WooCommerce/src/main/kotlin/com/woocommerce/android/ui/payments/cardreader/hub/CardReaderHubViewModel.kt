package com.woocommerce.android.ui.payments.cardreader.hub

import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.InPersonPaymentsCanadaFeatureFlag
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubViewState.ListItem
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubViewState.OnboardingErrorAction
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.OnboardingCompleted
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.STRIPE_EXTENSION_GATEWAY
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.CARD_READER
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@HiltViewModel
class CardReaderHubViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val inPersonPaymentsCanadaFeatureFlag: InPersonPaymentsCanadaFeatureFlag,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val wooStore: WooCommerceStore,
    private val cardReaderChecker: CardReaderOnboardingChecker,
) : ScopedViewModel(savedState) {
    private val arguments: CardReaderHubFragmentArgs by savedState.navArgs()

    private val viewState = MutableLiveData(
        CardReaderHubViewState(
            rows = createHubListWhenSinglePluginInstalled(isOnboardingComplete = false),
            isLoading = true,
            onboardingErrorAction = null
        )
    )

    fun onViewVisible() {
        launch {
            viewState.value = when (cardReaderChecker.getOnboardingState()) {
                is OnboardingCompleted -> createOnboardingCompleteState()
                else -> createOnboardingFailedState()
            }
        }
    }

    private val cardReaderPurchaseUrl: String by lazy {
        if (inPersonPaymentsCanadaFeatureFlag.isEnabled()) {
            val storeCountryCode = wooStore.getStoreCountryCode(selectedSite.get()) ?: null.also {
                WooLog.e(CARD_READER, "Store's country code not found.")
            }
            "${AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}$storeCountryCode"
        } else {
            val preferredPlugin = appPrefsWrapper.getCardReaderPreferredPlugin(
                selectedSite.get().id,
                selectedSite.get().siteId,
                selectedSite.get().selfHostedSiteId
            )
            when (preferredPlugin) {
                STRIPE_EXTENSION_GATEWAY -> AppUrls.STRIPE_M2_PURCHASE_CARD_READER
                WOOCOMMERCE_PAYMENTS, null -> AppUrls.WOOCOMMERCE_M2_PURCHASE_CARD_READER
            }
        }
    }

    private fun createHubListWhenSinglePluginInstalled(isOnboardingComplete: Boolean) =
        listOf(
            ListItem(
                icon = R.drawable.ic_gridicons_money_on_surface,
                label = UiStringRes(R.string.card_reader_collect_payment),
                onClick = ::onCollectPaymentClicked
            ),
            ListItem(
                icon = R.drawable.ic_shopping_cart,
                label = UiStringRes(R.string.card_reader_purchase_card_reader),
                onClick = ::onPurchaseCardReaderClicked
            ),
            ListItem(
                icon = R.drawable.ic_card_reader_manual,
                label = UiStringRes(R.string.settings_card_reader_manuals),
                onClick = ::onCardReaderManualsClicked
            ),
            ListItem(
                icon = R.drawable.ic_manage_card_reader,
                label = UiStringRes(R.string.card_reader_manage_card_reader),
                isEnabled = isOnboardingComplete,
                onClick = ::onManageCardReaderClicked
            ),
        )

    private fun createAdditionalItemWhenMultiplePluginsInstalled() =
        ListItem(
            icon = R.drawable.ic_payment_provider,
            label = UiStringRes(R.string.card_reader_manage_payment_provider),
            onClick = ::onCardReaderPaymentProviderClicked
        )

    private fun createOnboardingCompleteState() = CardReaderHubViewState(
        rows = if (isCardReaderPluginExplicitlySelected()) {
            createHubListWhenSinglePluginInstalled(isOnboardingComplete = true).toMutableList() +
                createAdditionalItemWhenMultiplePluginsInstalled()
        } else {
            createHubListWhenSinglePluginInstalled(isOnboardingComplete = true)
        },
        isLoading = false,
        onboardingErrorAction = null,
    )

    private fun createOnboardingFailedState() = CardReaderHubViewState(
        rows = createHubListWhenSinglePluginInstalled(isOnboardingComplete = false),
        isLoading = false,
        onboardingErrorAction = OnboardingErrorAction(
            text = UiStringRes(R.string.card_reader_onboarding_not_finished, containsHtml = true),
            onClick = ::onOnboardingErrorClicked
        )
    )

    val viewStateData: LiveData<CardReaderHubViewState> = viewState

    private fun onCollectPaymentClicked() {
        triggerEvent(CardReaderHubEvents.NavigateToPaymentCollectionScreen)
    }

    private fun onManageCardReaderClicked() {
        triggerEvent(CardReaderHubEvents.NavigateToCardReaderDetail(arguments.cardReaderFlowParam))
    }

    private fun onPurchaseCardReaderClicked() {
        triggerEvent(CardReaderHubEvents.NavigateToPurchaseCardReaderFlow(cardReaderPurchaseUrl))
    }

    private fun onCardReaderManualsClicked() {
        triggerEvent(CardReaderHubEvents.NavigateToCardReaderManualsScreen)
    }

    private fun onCardReaderPaymentProviderClicked() {
        trackPaymentProviderClickedEvent()
        clearPluginExplicitlySelectedFlag()
        triggerEvent(CardReaderHubEvents.NavigateToCardReaderOnboardingScreen)
    }

    private fun onOnboardingErrorClicked() {
        triggerEvent(CardReaderHubEvents.NavigateToCardReaderOnboardingScreen)
    }

    private fun trackPaymentProviderClickedEvent() {
        analyticsTrackerWrapper.track(AnalyticsEvent.SETTINGS_CARD_PRESENT_SELECT_PAYMENT_GATEWAY_TAPPED)
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

    sealed class CardReaderHubEvents : MultiLiveEvent.Event() {
        data class NavigateToCardReaderDetail(val cardReaderFlowParam: CardReaderFlowParam) : CardReaderHubEvents()
        data class NavigateToPurchaseCardReaderFlow(val url: String) : CardReaderHubEvents()
        object NavigateToPaymentCollectionScreen : CardReaderHubEvents()
        object NavigateToCardReaderManualsScreen : CardReaderHubEvents()
        object NavigateToCardReaderOnboardingScreen : CardReaderHubEvents()
    }

    data class CardReaderHubViewState(
        val rows: List<ListItem>,
        val isLoading: Boolean,
        val onboardingErrorAction: OnboardingErrorAction?,
    ) {
        data class ListItem(
            @DrawableRes val icon: Int,
            val label: UiString,
            val isEnabled: Boolean = true,
            val onClick: () -> Unit
        )

        data class OnboardingErrorAction(
            val text: UiString?,
            val onClick: () -> Unit,
        )
    }
}
