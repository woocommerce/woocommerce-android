package com.woocommerce.android.ui.cardreader.manuals

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CardReaderManualsViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    val manualState = getManualItems()

    private fun getManualItems(): List<ManualItem> = listOf(
        ManualItem(
            icon = R.drawable.ic_card_reader_manual,
            label = R.string.card_reader_bbpos_manual_card_reader,
            onManualClicked = ::onBbposManualClicked
        ),
        ManualItem(
            icon = R.drawable.ic_card_reader_manual,
            label = R.string.card_reader_m2_manual_card_reader,
            onManualClicked = ::onM2ManualClicked
        )
    )

    private fun onBbposManualClicked() {
        triggerEvent(ManualEvents.NavigateToCardReaderManualLink(AppUrls.BBPOS_MANUAL_CARD_READER))
    }

    private fun onM2ManualClicked() {
        triggerEvent(ManualEvents.NavigateToCardReaderManualLink(AppUrls.M2_MANUAL_CARD_READER))
    }

    sealed class ManualEvents : MultiLiveEvent.Event() {
        data class NavigateToCardReaderManualLink(val url: String) : ManualEvents()
    }

    data class ManualItem(
        val icon: Int,
        val label: Int,
        val onManualClicked: () -> Unit
    )
}
