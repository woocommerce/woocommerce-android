package com.woocommerce.android.ui.woopos.cardreader

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderActivity.Companion.WOO_POS_CARD_READER_MODE_KEY
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WooPosCardReaderActivityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    init {
        when (savedStateHandle.get<WooPosCardReaderMode>(WOO_POS_CARD_READER_MODE_KEY)) {
            is WooPosCardReaderMode.Connection -> {
                triggerEvent(
                    StartCardReaderConnectionFlow(
                        cardReaderFlowParam = CardReaderFlowParam.CardReadersHub(),
                        cardReaderType = CardReaderType.EXTERNAL
                    )
                )
            }

            is WooPosCardReaderMode.Payment -> {
                // TODO: Implement
            }

            null -> error("No card reader mode provided")
        }
    }
}
