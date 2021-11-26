package com.woocommerce.android.ui.prefs.cardreader.hub

import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import javax.inject.Inject

class CardReaderHubViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val viewState = MutableLiveData<CardReaderHubViewState>(
        createInitialState()
    )

    private fun createInitialState() = CardReaderHubViewState.Content(
        listOf(
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
                label = UiString.UiStringRes(R.string.card_reader_bbpos_manual_card_reader),
                onItemClicked = ::onBbposManualCardReaderClicked
            ),
        )
    )

    val viewStateData: LiveData<CardReaderHubViewState> = viewState

    private fun onManageCardReaderClicked() {
        triggerEvent(CardReaderHubEvents.NavigateToCardReaderDetail)
    }

    private fun onPurchaseCardReaderClicked() {
        triggerEvent(CardReaderHubEvents.NavigateToPurchaseCardReaderFlow)
    }

    private fun onBbposManualCardReaderClicked() {
        triggerEvent(CardReaderHubEvents.NavigateToManualCardReaderFlow(AppUrls.BBPOS_MANUAL_CARD_READER))
    }

    private fun onM2ManualCardReaderClicked() {
        triggerEvent(CardReaderHubEvents.NavigateToManualCardReaderFlow(AppUrls.M2_MANUAL_CARD_READER))
    }

    sealed class CardReaderHubEvents : MultiLiveEvent.Event() {
        object NavigateToCardReaderDetail : CardReaderHubEvents()
        object NavigateToPurchaseCardReaderFlow : CardReaderHubEvents() {
            const val url = AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER
        }
        data class NavigateToManualCardReaderFlow(val url: String) : CardReaderHubEvents()
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
