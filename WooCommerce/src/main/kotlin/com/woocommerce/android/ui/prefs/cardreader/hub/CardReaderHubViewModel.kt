package com.woocommerce.android.ui.prefs.cardreader.hub

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
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
                UiString.UiStringRes(R.string.card_reader_purchase_card_reader),
                ::onPurchaseCardReaderClicked
            ),
            CardReaderHubListItemViewState(
                UiString.UiStringRes(R.string.card_reader_manage_card_reader),
                ::onManageCardReaderClicked
            )
        )
    )

    val viewStateData: LiveData<CardReaderHubViewState> = viewState

    private fun onManageCardReaderClicked() {
        triggerEvent(CardReaderHubEvents.NavigateToCardReaderDetail)
    }

    private fun onPurchaseCardReaderClicked() {
        triggerEvent(CardReaderHubEvents.NavigateToPurchaseCardReaderFlow)
    }

    sealed class CardReaderHubEvents : MultiLiveEvent.Event() {
        object NavigateToCardReaderDetail : CardReaderHubEvents()
        object NavigateToPurchaseCardReaderFlow : CardReaderHubEvents()
    }

    sealed class CardReaderHubViewState {
        abstract val rows: List<CardReaderHubListItemViewState>

        data class Content(override val rows: List<CardReaderHubListItemViewState>) : CardReaderHubViewState()
    }

    data class CardReaderHubListItemViewState(val label: UiString, val onItemClicked: () -> Unit)
}
