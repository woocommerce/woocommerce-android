package com.woocommerce.android.ui.payments

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import javax.inject.Inject

class CardReaderTypeSelectionViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val navArgs: CardReaderTypeSelectionDialogFragmentArgs by savedState.navArgs()

    fun onUseBluetoothReaderSelected() {
        _event.value = NavigateToCardReaderPaymentFlow(navArgs.cardReaderFlowParam)
    }

    data class NavigateToCardReaderPaymentFlow(
        val cardReaderFlowParam: CardReaderFlowParam
    ) : MultiLiveEvent.Event()
}
