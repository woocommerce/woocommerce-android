package com.woocommerce.android.ui.cardreader.hub

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
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.cardreader.InPersonPaymentsCanadaFeatureFlag
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.cardreader.onboarding.PluginType.STRIPE_EXTENSION_GATEWAY
import com.woocommerce.android.ui.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CardReaderHubViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val inPersonPaymentsCanadaFeatureFlag: InPersonPaymentsCanadaFeatureFlag,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedState) {
    private val arguments: CardReaderHubFragmentArgs by savedState.navArgs()

    private val cardReaderHubListWhenSinglePluginInstalled = mutableListOf(
        CardReaderHubListItemViewState(
            icon = R.drawable.ic_shopping_cart,
            label = UiString.UiStringRes(R.string.card_reader_purchase_card_reader),
            onItemClicked = ::onPurchaseCardReaderClicked
        ),
        CardReaderHubListItemViewState(
            icon = R.drawable.ic_manage_card_reader,
            label = UiString.UiStringRes(R.string.card_reader_manage_card_reader),
            onItemClicked = ::onManageCardReaderClicked
        ),
        CardReaderHubListItemViewState(
            icon = R.drawable.ic_card_reader_manual,
            label = UiString.UiStringRes(R.string.settings_card_reader_manuals),
            onItemClicked = ::onCardReaderManualsClicked
        )
    )

    private val cardReaderHubListWhenMultiplePluginsInstalled = cardReaderHubListWhenSinglePluginInstalled +
        mutableListOf(
            CardReaderHubListItemViewState(
                icon = R.drawable.ic_payment_provider,
                label = UiString.UiStringRes(R.string.card_reader_manage_payment_provider),
                onItemClicked = ::onCardReaderPaymentProviderClicked
            )
        )

    private val cardReaderPurchaseUrl: String by lazy {
        if (inPersonPaymentsCanadaFeatureFlag.isEnabled()) {
            // todo fix the URL when decided
            "${AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}${arguments.storeCountryCode}"
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

    private val viewState = MutableLiveData<CardReaderHubViewState>(
        createInitialState()
    )

    private fun createInitialState(): CardReaderHubViewState.Content {
        val site = selectedSite.get()
        return if (
            appPrefsWrapper.isCardReaderPluginExplicitlySelected(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId,
            )
        ) {
            CardReaderHubViewState.Content(cardReaderHubListWhenMultiplePluginsInstalled)
        } else {
            CardReaderHubViewState.Content(cardReaderHubListWhenSinglePluginInstalled)
        }
    }

    val viewStateData: LiveData<CardReaderHubViewState> = viewState

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

    sealed class CardReaderHubEvents : MultiLiveEvent.Event() {
        data class NavigateToCardReaderDetail(val cardReaderFlowParam: CardReaderFlowParam) : CardReaderHubEvents()
        data class NavigateToPurchaseCardReaderFlow(val url: String) : CardReaderHubEvents()
        object NavigateToCardReaderManualsScreen : CardReaderHubEvents()
        object NavigateToCardReaderOnboardingScreen : CardReaderHubEvents()
    }

    sealed class CardReaderHubViewState {
        abstract val rows: List<CardReaderHubListItemViewState>

        data class Content(override val rows: List<CardReaderHubListItemViewState>) : CardReaderHubViewState()
    }

    data class CardReaderHubListItemViewState(
        @DrawableRes val icon: Int,
        val label: UiString,
        val onItemClicked: () -> Unit
    )
}
