package com.woocommerce.android.ui.woopos.cardreader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderActivity.Companion.WOO_POS_CARD_READER_MODE_KEY
import com.woocommerce.android.util.WooLog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WooPosCardReaderViewModel @Inject constructor(val savedStateHandle: SavedStateHandle) :
    ViewModel() {
    val cardReaderMode: WooPosCardReaderMode
        get() = savedStateHandle.get<WooPosCardReaderMode>(WOO_POS_CARD_READER_MODE_KEY).run {
            when (this) {
                is WooPosCardReaderMode.Connection -> {
                    WooPosCardReaderMode.Connection
                }

                is WooPosCardReaderMode.Payment -> {
                    if (orderId != -1L) {
                        WooPosCardReaderMode.Payment(orderId)
                    } else {
                        val errorMessage = "Tried collecting payment with invalid orderId"
                        WooLog.e(WooLog.T.POS, "Error in WooPosCardReaderViewModel - $errorMessage")
                        error(errorMessage)
                    }
                }

                null -> {
                    val errorMessage = "WooPosCardReaderMode not found in savedStateHandle"
                    WooLog.e(WooLog.T.POS, "Error in WooPosCardReaderViewModel - $errorMessage")
                    error(errorMessage)
                }
            }
        }
}
