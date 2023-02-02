package com.woocommerce.android.ui.payments

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.payments.cardreader.CardReaderTracker
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.ui.payments.taptopay.IsTapToPayAvailable
import com.woocommerce.android.ui.payments.taptopay.IsTapToPayAvailable.Result.Available
import com.woocommerce.android.ui.payments.taptopay.IsTapToPayAvailable.Result.NotAvailable
import com.woocommerce.android.ui.payments.taptopay.IsTapToPayEnabled
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CardReaderTypeSelectionViewModel
@Inject constructor(
    savedState: SavedStateHandle,
    isTapToPayAvailable: IsTapToPayAvailable,
    isTapToPayEnabled: IsTapToPayEnabled,
    private val tracker: CardReaderTracker,
) : ScopedViewModel(savedState) {
    private val navArgs: CardReaderTypeSelectionDialogFragmentArgs by savedState.navArgs()

    init {
        when (val result = isTapToPayAvailable(navArgs.countryCode, isTapToPayEnabled)) {
            Available -> onUseBluetoothReaderSelected()
            is NotAvailable -> tracker.trackTapToPayNotAvailable(result)
        }
    }

    fun onUseTapToPaySelected() {
        tracker.trackSelectReaderTypeBuiltInTapped()
        navigateToConnectionFlow(CardReaderType.BUILT_IN)
    }

    fun onUseBluetoothReaderSelected() {
        tracker.trackSelectReaderTypeBluetoothTapped()
        navigateToConnectionFlow(CardReaderType.EXTERNAL)
    }

    private fun navigateToConnectionFlow(cardReaderType: CardReaderType) {
        _event.value = NavigateToCardReaderPaymentFlow(
            navArgs.cardReaderFlowParam,
            cardReaderType
        )
    }

    data class NavigateToCardReaderPaymentFlow(
        val cardReaderFlowParam: CardReaderFlowParam,
        val cardReaderType: CardReaderType,
    ) : MultiLiveEvent.Event()
}
