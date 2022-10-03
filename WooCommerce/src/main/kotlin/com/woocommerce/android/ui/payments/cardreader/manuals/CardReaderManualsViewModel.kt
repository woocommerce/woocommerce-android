package com.woocommerce.android.ui.payments.cardreader.manuals

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.cardreader.connection.SpecificReader.Chipper2X
import com.woocommerce.android.cardreader.connection.SpecificReader.StripeM2
import com.woocommerce.android.cardreader.connection.SpecificReader.WisePade3
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CardReaderManualsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    cardReaderManualsSupportedReadersMapper: CardReaderManualsSupportedReadersMapper,
) : ScopedViewModel(savedState) {
    private val navArgs: CardReaderManualsFragmentArgs by savedState.navArgs()
    private val cardReaderConfig = navArgs.cardReaderConfig
    val manualState = cardReaderManualsSupportedReadersMapper.mapSupportedReadersToManualItems(
        cardReaderConfig,
        mapOf(
            Chipper2X to ::onBbposManualClicked,
            StripeM2 to ::onM2ManualClicked,
            WisePade3 to ::onWisePad3ManualCardReaderClicked
        )
    )

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
