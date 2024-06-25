package com.woocommerce.android.ui.woopos.cardreader

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderActivity.Companion.WOO_POS_CARD_READER_MODE_KEY
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WooPosCardReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    init {
        when (val mode = savedStateHandle.get<WooPosCardReaderMode>(WOO_POS_CARD_READER_MODE_KEY)) {
            is WooPosCardReaderMode.Connection -> {
                triggerEvent(WooPosCardReaderEvent.Connection)
            }

            is WooPosCardReaderMode.Payment -> {
                if (mode.orderId != -1L) {
                    triggerEvent(WooPosCardReaderEvent.Payment(mode.orderId))
                }
            }

            null -> error("WooPosCardReaderMode not found in savedStateHandle")
        }
    }
}
