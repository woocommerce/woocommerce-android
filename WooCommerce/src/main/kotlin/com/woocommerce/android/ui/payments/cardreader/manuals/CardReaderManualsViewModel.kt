package com.woocommerce.android.ui.payments.cardreader.manuals

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.cardreader.connection.SpecificReader.Chipper2X
import com.woocommerce.android.cardreader.connection.SpecificReader.StripeM2
import com.woocommerce.android.cardreader.connection.SpecificReader.WisePade3
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@HiltViewModel
class CardReaderManualsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    selectedSite: SelectedSite,
    wooStore: WooCommerceStore,
    cardReaderManualsSupportedReadersMapper: CardReaderManualsSupportedReadersMapper,
    cardReaderCountryConfigProvider: CardReaderCountryConfigProvider

) : ScopedViewModel(savedState) {
    val storeCountryCode = wooStore.getStoreCountryCode(selectedSite.get())
    private val cardReaderConfig = cardReaderCountryConfigProvider.provideCountryConfigFor(storeCountryCode)
    val manualState = when (cardReaderConfig) {
        is CardReaderConfigForSupportedCountry -> {
            cardReaderManualsSupportedReadersMapper.mapSupportedReadersToManualItems(
                cardReaderConfig,
                mapOf(
                    Chipper2X to ::onBbposManualClicked,
                    StripeM2 to ::onM2ManualClicked,
                    WisePade3 to ::onWisePad3ManualCardReaderClicked
                )
            )
        }
        else -> null
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
