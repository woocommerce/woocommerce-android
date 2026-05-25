package com.woocommerce.android.ui.woopos.paymentsuccess

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.cardpayment.WooPosCardPaymentAnalyticsTracker
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsRepository
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosPaymentSuccessViewModel @Inject constructor(
    private val analyticsTracker: WooPosCardPaymentAnalyticsTracker,
    private val totalsRepository: WooPosTotalsRepository,
    private val priceFormat: WooPosFormatPrice,
    private val resourceProvider: ResourceProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val orderId: Long = requireNotNull(savedStateHandle[PAYMENT_SUCCESS_ORDER_ID_KEY])
    private val source: PaymentSuccessSource = savedStateHandle.get<String>(PAYMENT_SUCCESS_SOURCE_KEY)
        ?.let { runCatching { PaymentSuccessSource.valueOf(it) }.getOrNull() }
        ?: PaymentSuccessSource.CARD_CHECKOUT

    private val _state = MutableStateFlow(
        PaymentSuccessViewState(
            orderTotalText = "",
            receiptSentMessage = null,
        )
    )
    val state: StateFlow<PaymentSuccessViewState> = _state.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<WooPosNavigationEvent>()
    val navigationEvent: SharedFlow<WooPosNavigationEvent> = _navigationEvent.asSharedFlow()

    init {
        loadOrderDetails()
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
            _state.value = PaymentSuccessViewState(
                orderTotalText = orderTotalText,
                receiptSentMessage = receiptSentMessage,
            )
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

    private suspend fun emitNavigateBack() {
        when (source) {
            PaymentSuccessSource.CARD_CHECKOUT,
            PaymentSuccessSource.SCAN_TO_PAY,
            PaymentSuccessSource.MARK_ORDER_AS_COMPLETE -> {
                _navigationEvent.emit(WooPosNavigationEvent.GoBack)
            }
        }
    }
}

data class PaymentSuccessViewState(
    val orderTotalText: String,
    val receiptSentMessage: String?,
)
