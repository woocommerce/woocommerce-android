package com.woocommerce.android.ui.woopos.cardreader

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderActivity.Companion.WOO_POS_CARD_READER_MODE_KEY
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WooPosCardReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    init {
        when (savedStateHandle.get<WooPosCardReaderMode>(WOO_POS_CARD_READER_MODE_KEY)) {
            is WooPosCardReaderMode.Connection -> {
                triggerEvent(
                    StartCardReaderConnectionFlow(
                        cardReaderFlowParam = CardReaderFlowParam.WooPosConnection,
                        cardReaderType = CardReaderType.EXTERNAL
                    )
                )
            }

            is WooPosCardReaderMode.Payment -> {
                // TODO: Implement
            }

            null -> {

            }
        }
    }

    fun onConnectToReaderResultReceived(connected: Boolean) {
        Log.d("WooPosCardReaderViewModel", "onConnectToReaderResultReceived: $connected")
    }
}
