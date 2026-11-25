package com.woocommerce.android.ui.woopos.paymentsuccess

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.cardpayment.WooPosCardPaymentAnalyticsTracker
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsRepository
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosPrintReceiptButtonState
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosPrintReceiptCoordinator
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosPrintReceiptEvent
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosPrintReceiptFailure
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosPrintReceiptFailureAction
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosPaymentSuccessViewModel @Inject constructor(
    private val analyticsTracker: WooPosCardPaymentAnalyticsTracker,
    private val totalsRepository: WooPosTotalsRepository,
    private val priceFormat: WooPosFormatPrice,
    private val resourceProvider: ResourceProvider,
    printReceiptCoordinatorFactory: WooPosPrintReceiptCoordinator.Factory,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val orderId: Long = requireNotNull(savedStateHandle[PAYMENT_SUCCESS_ORDER_ID_KEY])
    private val source: PaymentSuccessSource = savedStateHandle.get<String>(PAYMENT_SUCCESS_SOURCE_KEY)
        ?.let { runCatching { PaymentSuccessSource.valueOf(it) }.getOrNull() }
        ?: PaymentSuccessSource.CARD_CHECKOUT

    private val _state = MutableStateFlow(
        PaymentSuccessViewState(
            orderTotalText = "",
            receiptSentMessage = null,
            isReceiptPrinterSetupDialogVisible = savedStateHandle[KEY_RECEIPT_PRINTER_SETUP_DIALOG_VISIBLE] ?: false,
            printReceiptFailure = savedStateHandle[KEY_PRINT_RECEIPT_FAILURE],
        )
    )
    val state: StateFlow<PaymentSuccessViewState> = _state.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<WooPosNavigationEvent>()
    val navigationEvent: SharedFlow<WooPosNavigationEvent> = _navigationEvent.asSharedFlow()

    private val printReceiptCoordinator = printReceiptCoordinatorFactory.create(viewModelScope)
    val printReceiptButtonState: StateFlow<WooPosPrintReceiptButtonState> = printReceiptCoordinator.buttonState

    init {
        loadOrderDetails()
        listenPrintReceiptEvents()
    }

    private fun listenPrintReceiptEvents() {
        viewModelScope.launch {
            printReceiptCoordinator.events.collect { event ->
                when (event) {
                    WooPosPrintReceiptEvent.ShowPrinterSetup -> setReceiptPrinterSetupDialogVisible(true)

                    is WooPosPrintReceiptEvent.PrintFailed -> setPrintReceiptFailure(event.failure)
                }
            }
        }
    }

    private fun loadOrderDetails() {
        viewModelScope.launch {
            val order = totalsRepository.getOrderById(orderId)
            val orderTotalText = if (order != null) {
                val priceText = priceFormat(order.total)
                val stringRes = when (source) {
                    PaymentSuccessSource.CARD_CHECKOUT -> R.string.woopos_totals_success_payment_card
                    PaymentSuccessSource.SCAN_TO_PAY -> R.string.woopos_totals_success_payment_qr
                    PaymentSuccessSource.MARK_ORDER_AS_COMPLETE -> R.string.woopos_totals_success_payment_external
                }
                resourceProvider.getString(stringRes, priceText)
            } else {
                resourceProvider.getString(R.string.woopos_payment_successful_label)
            }
            val receiptSentMessage = when (source) {
                PaymentSuccessSource.CARD_CHECKOUT ->
                    order?.billingAddress?.email
                        ?.takeIf { it.isNotBlank() }
                        ?.let { email ->
                            resourceProvider.getString(R.string.woopos_receipt_sent_to_customer, email)
                        }
                PaymentSuccessSource.SCAN_TO_PAY,
                PaymentSuccessSource.MARK_ORDER_AS_COMPLETE -> null
            }
            _state.update {
                it.copy(
                    orderTotalText = orderTotalText,
                    receiptSentMessage = receiptSentMessage,
                )
            }
        }
    }

    fun onDoneClicked() {
        viewModelScope.launch {
            emitNavigateBack()
        }
    }

    fun onBackPressed() {
        viewModelScope.launch {
            emitNavigateBack()
        }
    }

    fun onEmailReceiptClicked() {
        printReceiptCoordinator.ignorePendingFailure()
        viewModelScope.launch {
            analyticsTracker.trackEmailReceiptTapped()
            val receiptAlreadySent =
                (_state.value as? PaymentSuccessViewState)?.receiptSentMessage != null
            _navigationEvent.emit(
                WooPosNavigationEvent.OpenEmailReceipt(
                    orderId = orderId,
                    receiptAlreadySent = receiptAlreadySent,
                )
            )
        }
    }

    fun onPrintReceiptClicked() {
        printReceiptCoordinator.onPrintReceiptClicked(orderId)
    }

    fun onReceiptPrinterSetupDialogDismissed() {
        setReceiptPrinterSetupDialogVisible(false)
    }

    fun onReceiptPrinterConnected() {
        setReceiptPrinterSetupDialogVisible(false)
        printReceiptCoordinator.onPrinterConnected(orderId)
    }

    private fun setReceiptPrinterSetupDialogVisible(isVisible: Boolean) {
        savedStateHandle[KEY_RECEIPT_PRINTER_SETUP_DIALOG_VISIBLE] = isVisible
        _state.update { it.copy(isReceiptPrinterSetupDialogVisible = isVisible) }
    }

    fun onPrintReceiptFailureDialogDismissed() {
        setPrintReceiptFailure(null)
    }

    fun onPrintReceiptFailureActionSelected(action: WooPosPrintReceiptFailureAction) {
        onPrintReceiptFailureDialogDismissed()
        when (action) {
            WooPosPrintReceiptFailureAction.Retry -> onPrintReceiptClicked()
            WooPosPrintReceiptFailureAction.EmailReceipt -> onEmailReceiptClicked()
        }
    }

    private fun setPrintReceiptFailure(failure: WooPosPrintReceiptFailure?) {
        savedStateHandle[KEY_PRINT_RECEIPT_FAILURE] = failure
        _state.update { it.copy(printReceiptFailure = failure) }
    }

    private suspend fun emitNavigateBack() {
        when (source) {
            PaymentSuccessSource.CARD_CHECKOUT,
            PaymentSuccessSource.SCAN_TO_PAY,
            PaymentSuccessSource.MARK_ORDER_AS_COMPLETE -> {
                _navigationEvent.emit(WooPosNavigationEvent.GoBack)
            }
        }
    }

    private companion object {
        const val KEY_RECEIPT_PRINTER_SETUP_DIALOG_VISIBLE = "receipt_printer_setup_dialog_visible"
        const val KEY_PRINT_RECEIPT_FAILURE = "print_receipt_failure"
    }
}

data class PaymentSuccessViewState(
    val orderTotalText: String,
    val receiptSentMessage: String?,
    val isReceiptPrinterSetupDialogVisible: Boolean = false,
    val printReceiptFailure: WooPosPrintReceiptFailure? = null,
)
