package com.woocommerce.android.ui.woopos.receiptprinting

import com.woocommerce.android.receiptprinter.ReceiptPrinterConnectionStatus
import com.woocommerce.android.receiptprinter.ReceiptPrinterResult
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosReceiptPrintingEnabled
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class WooPosPrintReceiptCoordinator @AssistedInject constructor(
    private val receiptPrinterFacade: WooPosReceiptPrinterFacade,
    private val logger: WooPosLogWrapper,
    isReceiptPrintingEnabled: WooPosReceiptPrintingEnabled,
    @Assisted private val scope: CoroutineScope,
) {
    private val _buttonState = MutableStateFlow(
        if (isReceiptPrintingEnabled()) WooPosPrintReceiptButtonState.Idle else WooPosPrintReceiptButtonState.Hidden
    )
    val buttonState: StateFlow<WooPosPrintReceiptButtonState> = _buttonState.asStateFlow()

    private val _events = MutableSharedFlow<WooPosPrintReceiptEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<WooPosPrintReceiptEvent> = _events.asSharedFlow()

    fun onPrintReceiptClicked(orderId: Long) {
        if (_buttonState.value != WooPosPrintReceiptButtonState.Idle) return
        if (isPrinterConnected()) {
            print(orderId)
        } else {
            _events.tryEmit(WooPosPrintReceiptEvent.ShowPrinterSetup)
        }
    }

    fun onPrinterSetupClosed(orderId: Long) {
        if (isPrinterConnected()) print(orderId)
    }

    private fun print(orderId: Long) {
        _buttonState.value = WooPosPrintReceiptButtonState.Loading
        scope.launch {
            when (val result = receiptPrinterFacade.printReceipt(orderId)) {
                ReceiptPrinterResult.Success -> {
                    _buttonState.value = WooPosPrintReceiptButtonState.Success
                    delay(SUCCESS_DURATION)
                }

                is ReceiptPrinterResult.Failure -> {
                    logger.e("Printing the receipt failed: ${result.error}")
                    _events.tryEmit(
                        WooPosPrintReceiptEvent.PrintFailed(orderId, WooPosPrintReceiptFailure.from(result.error))
                    )
                }
            }
            _buttonState.value = WooPosPrintReceiptButtonState.Idle
        }
    }

    private fun isPrinterConnected() =
        receiptPrinterFacade.connectionStatus.value is ReceiptPrinterConnectionStatus.Connected

    @AssistedFactory
    interface Factory {
        fun create(scope: CoroutineScope): WooPosPrintReceiptCoordinator
    }

    private companion object {
        val SUCCESS_DURATION = 2.seconds
    }
}

enum class WooPosPrintReceiptButtonState { Hidden, Idle, Loading, Success }

sealed interface WooPosPrintReceiptEvent {
    data object ShowPrinterSetup : WooPosPrintReceiptEvent
    data class PrintFailed(val orderId: Long, val failure: WooPosPrintReceiptFailure) : WooPosPrintReceiptEvent
}
