package com.woocommerce.android.ui.woopos.receiptprinting

import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardDetails
import com.woocommerce.android.receiptprinter.ReceiptPrinterConnectionStatus
import com.woocommerce.android.receiptprinter.ReceiptPrinterError
import com.woocommerce.android.receiptprinter.ReceiptPrinterResult
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosReceiptPrintingEnabled
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

class WooPosPrintReceiptCoordinator @AssistedInject constructor(
    private val receiptPrinterFacade: WooPosReceiptPrinterFacade,
    private val logger: WooPosLogWrapper,
    private val analyticsTracker: WooPosAnalyticsTracker,
    isReceiptPrintingEnabled: WooPosReceiptPrintingEnabled,
    @Assisted private val scope: CoroutineScope,
    @Assisted currentOrderId: Flow<Long>,
) {
    private val isEnabled = isReceiptPrintingEnabled()
    private val printState = MutableStateFlow<Print?>(null)

    val buttonState: StateFlow<WooPosPrintReceiptButtonState> = combine(currentOrderId, printState) { orderId, print ->
        when {
            !isEnabled -> WooPosPrintReceiptButtonState.Hidden
            print?.orderId == orderId -> print.state
            else -> WooPosPrintReceiptButtonState.Idle
        }
    }.stateIn(scope, SharingStarted.Eagerly, initialButtonState())

    private val _events = MutableSharedFlow<WooPosPrintReceiptEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<WooPosPrintReceiptEvent> = _events.asSharedFlow()

    private var isPrintFailureIgnored = false
    private var cardPayment: CardPayment? = null

    fun onPrintReceiptClicked(orderId: Long) {
        if (!canPrint(orderId)) return
        track(WooPosAnalyticsEvent.Event.ReceiptPrintTapped)
        printOrShowPrinterSetup(orderId)
    }

    fun onPrintRetryClicked(orderId: Long) {
        if (!canPrint(orderId)) return
        track(WooPosAnalyticsEvent.Event.ReceiptPrintRetryTapped)
        printOrShowPrinterSetup(orderId)
    }

    private fun printOrShowPrinterSetup(orderId: Long) {
        if (isPrinterConnected()) {
            print(orderId)
        } else {
            _events.tryEmit(WooPosPrintReceiptEvent.ShowPrinterSetup)
        }
    }

    fun onPrinterConnected(orderId: Long) {
        if (!canPrint(orderId)) return
        print(orderId)
    }

    fun onOrderPaidByCard(orderId: Long, cardDetails: CardDetails?) {
        cardPayment = cardDetails?.let { CardPayment(orderId, it) }
    }

    fun ignorePendingFailure() {
        isPrintFailureIgnored = true
    }

    private fun print(orderId: Long) {
        printState.value = Print(orderId, WooPosPrintReceiptButtonState.Loading)
        isPrintFailureIgnored = false
        val cardDetails = cardPayment?.takeIf { it.orderId == orderId }?.cardDetails
        scope.launch {
            // A receipt that was asked for is printed even if the screen that asked for it is left meanwhile.
            val result = withContext(NonCancellable) {
                receiptPrinterFacade.printReceipt(orderId, cardDetails).also { trackPrintResult(it) }
            }
            when (result) {
                ReceiptPrinterResult.Success -> {
                    updatePrint(orderId) { it.copy(state = WooPosPrintReceiptButtonState.Success) }
                    delay(SUCCESS_DURATION)
                }

                is ReceiptPrinterResult.Failure -> {
                    logger.e("Printing the receipt failed: ${result.error}")
                    if (!isPrintFailureIgnored) {
                        _events.tryEmit(
                            WooPosPrintReceiptEvent.PrintFailed(orderId, WooPosPrintReceiptFailure.from(result.error))
                        )
                    }
                }
            }
            updatePrint(orderId) { null }
        }
    }

    private fun canPrint(orderId: Long) = isEnabled && printState.value?.orderId != orderId

    private fun updatePrint(orderId: Long, transform: (Print) -> Print?) {
        printState.update { current -> if (current?.orderId == orderId) transform(current) else current }
    }

    private fun initialButtonState() =
        if (isEnabled) WooPosPrintReceiptButtonState.Idle else WooPosPrintReceiptButtonState.Hidden

    private suspend fun trackPrintResult(result: ReceiptPrinterResult) {
        val event = when (result) {
            ReceiptPrinterResult.Success -> WooPosAnalyticsEvent.Event.ReceiptPrintSuccess
            is ReceiptPrinterResult.Failure -> WooPosAnalyticsEvent.Error.ReceiptPrintFailed(
                errorContext = this::class,
                errorType = result.error.analyticsType,
                errorDescription = result.error.analyticsDescription,
            )
        }
        analyticsTracker.track(event)
    }

    private fun track(event: WooPosAnalyticsEvent) {
        scope.launch { analyticsTracker.track(event) }
    }

    private fun isPrinterConnected() =
        receiptPrinterFacade.connectionStatus.value is ReceiptPrinterConnectionStatus.Connected

    private data class CardPayment(val orderId: Long, val cardDetails: CardDetails)

    private data class Print(val orderId: Long, val state: WooPosPrintReceiptButtonState)

    @AssistedFactory
    interface Factory {
        fun create(scope: CoroutineScope, currentOrderId: Flow<Long>): WooPosPrintReceiptCoordinator
    }

    private companion object {
        val SUCCESS_DURATION = 2.seconds
    }
}

private val ReceiptPrinterError.analyticsType: String
    get() = when (this) {
        ReceiptPrinterError.DiscoveryFailure -> "discovery_failure"
        ReceiptPrinterError.PrinterNotFound -> "printer_not_found"
        ReceiptPrinterError.PrinterNotConnected -> "printer_not_connected"
        ReceiptPrinterError.PaperEmpty -> "paper_empty"
        ReceiptPrinterError.CoverOpen -> "cover_open"
        is ReceiptPrinterError.ConnectionLost -> "connection_lost"
        is ReceiptPrinterError.PrintFailed -> "print_failed"
    }

// Matches the iOS PrinterError names, which iOS sends as the description of these errors.
private val ReceiptPrinterError.analyticsDescription: String
    get() = when (this) {
        ReceiptPrinterError.DiscoveryFailure -> "discoveryFailure"
        ReceiptPrinterError.PrinterNotFound -> "printerNotFound"
        ReceiptPrinterError.PrinterNotConnected -> "printerNotConnected"
        ReceiptPrinterError.PaperEmpty -> "paperEmpty"
        ReceiptPrinterError.CoverOpen -> "coverOpen"
        is ReceiptPrinterError.ConnectionLost -> cause.analyticsDescription("connectionLost")
        is ReceiptPrinterError.PrintFailed -> cause.analyticsDescription("printFailed")
    }

private fun Throwable?.analyticsDescription(fallback: String): String =
    this?.let { "${it::class.simpleName}: ${it.message}".take(MAX_ERROR_DESCRIPTION_LENGTH) } ?: fallback

private const val MAX_ERROR_DESCRIPTION_LENGTH = 100

enum class WooPosPrintReceiptButtonState { Hidden, Idle, Loading, Success }

sealed interface WooPosPrintReceiptEvent {
    data object ShowPrinterSetup : WooPosPrintReceiptEvent
    data class PrintFailed(val orderId: Long, val failure: WooPosPrintReceiptFailure) : WooPosPrintReceiptEvent
}
