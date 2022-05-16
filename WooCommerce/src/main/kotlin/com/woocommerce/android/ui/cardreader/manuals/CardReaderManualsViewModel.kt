package com.woocommerce.android.ui.cardreader.manuals

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.ui.cardreader.InPersonPaymentsCanadaFeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CardReaderManualsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val inPersonPaymentsCanadaFeatureFlag: InPersonPaymentsCanadaFeatureFlag
) : ScopedViewModel(savedState) {
    val manualState = mutableListOf(
        ManualItem(
            icon = R.drawable.ic_chipper_reader,
            label = R.string.card_reader_bbpos_manual_card_reader,
            onManualClicked = ::onBbposManualClicked
        ),
        ManualItem(
            icon = R.drawable.ic_m2_reader,
            label = R.string.card_reader_m2_manual_card_reader,
            onManualClicked = ::onM2ManualClicked
        )
    ).apply {
        if (inPersonPaymentsCanadaFeatureFlag.isEnabled()) {
            add(
                ManualItem(
                    icon = R.drawable.ic_wisepad3_reader,
                    label = R.string.card_reader_wisepad_3_manual_card_reader,
                    onManualClicked = ::onWisePad3ManualCardReaderClicked
                )
            )
        }
    }

    private fun onBbposManualClicked() {
        triggerEvent(ManualEvents.NavigateToCardReaderManualLink(AppUrls.BBPOS_MANUAL_CARD_READER))
    }

    private fun onM2ManualClicked() {
        triggerEvent(ManualEvents.NavigateToCardReaderManualLink(AppUrls.M2_MANUAL_CARD_READER))
    }

    private fun onWisePad3ManualCardReaderClicked() {
        triggerEvent(ManualEvents.NavigateToCardReaderManualLink(AppUrls.WISEPAD_3_MANUAL_CARD_READER))
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
