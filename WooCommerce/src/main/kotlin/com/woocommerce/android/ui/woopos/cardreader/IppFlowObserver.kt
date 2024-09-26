package com.woocommerce.android.ui.woopos.cardreader

import com.woocommerce.android.ui.payments.cardreader.payment.ViewState as CardReaderPaymentViewState
import androidx.lifecycle.ViewModel
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentViewModel
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState
import com.woocommerce.android.ui.woopos.cardreader.IppFlowObserver.PaymentFlowState.CollectPayment
import com.woocommerce.android.ui.woopos.cardreader.IppFlowObserver.PaymentFlowState.DataLoading
import com.woocommerce.android.ui.woopos.cardreader.IppFlowObserver.PaymentFlowState.PaymentCapturing
import com.woocommerce.android.ui.woopos.cardreader.IppFlowObserver.PaymentFlowState.PaymentFailed
import com.woocommerce.android.ui.woopos.cardreader.IppFlowObserver.PaymentFlowState.PaymentProcessing
import com.woocommerce.android.ui.woopos.cardreader.IppFlowObserver.PaymentFlowState.PaymentSuccessful
import com.woocommerce.android.ui.woopos.cardreader.IppFlowObserver.PaymentFlowState.RefreshingOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IppFlowObserver @Inject constructor() {
    private val _state = MutableStateFlow<PaymentFlowState>(PaymentFlowState.Idle)
    val state: StateFlow<PaymentFlowState> = _state

    fun notify(event: Any, viewModel: ViewModel) {
        when (event) {
            is CardReaderPaymentViewState -> {
                _state.value = when (event) {
                    is ViewState.LoadingDataState -> DataLoading {
                        (viewModel as CardReaderPaymentViewModel).onBackPressed()
                    }
                    is ViewState.ExternalReaderCapturingPaymentState -> PaymentCapturing
                    is ViewState.ExternalReaderCollectPaymentState -> CollectPayment(event) {
                        (viewModel as CardReaderPaymentViewModel).onBackPressed()
                    }
                    is ViewState.ExternalReaderFailedPaymentState -> PaymentFailed(event) {
                        TODO("Not yet implemented")
                    }
                    is ViewState.ExternalReaderPaymentSuccessfulState -> PaymentSuccessful
                    is ViewState.ExternalReaderProcessingPaymentState -> PaymentProcessing {
                        (viewModel as CardReaderPaymentViewModel).onBackPressed()
                    }
                    ViewState.ReFetchingOrderState -> RefreshingOrder

                    is ViewState.BuiltInReaderCapturingPaymentState,
                    is ViewState.BuiltInReaderCollectPaymentState,
                    is ViewState.BuiltInReaderFailedPaymentState,
                    is ViewState.BuiltInReaderPaymentSuccessfulReceiptSentAutomaticallyState,
                    is ViewState.BuiltInReaderPaymentSuccessfulState,
                    is ViewState.BuiltInReaderProcessingPaymentState,
                    is ViewState.CollectRefundState,
                    is ViewState.ExternalReaderPaymentSuccessfulReceiptSentAutomaticallyState,
                    is ViewState.FailedRefundState,
                    is ViewState.PrintingReceiptState,
                    is ViewState.ProcessingRefundState,
                    is ViewState.RefundLoadingDataState,
                    is ViewState.RefundSuccessfulState,
                    ViewState.SharingReceiptState -> TODO("Not yet implemented")
                }
            }
        }
    }

    sealed class PaymentFlowState {
        data object Idle : PaymentFlowState()
        data class DataLoading(val cancelPayment: () -> Unit) : PaymentFlowState()
        data class CollectPayment(val state: ViewState, val cancelPayment: () -> Unit) : PaymentFlowState()
        data class PaymentFailed(val state: ViewState, val tryAgain: () -> Unit) : PaymentFlowState()
        data class PaymentProcessing(val cancelPayment: () -> Unit) : PaymentFlowState()
        data object PaymentCapturing : PaymentFlowState()
        data object RefreshingOrder : PaymentFlowState()
        data object PaymentSuccessful : PaymentFlowState()
    }
}