package com.woocommerce.android.ui.woopos.cardreader

import com.woocommerce.android.ui.payments.cardreader.payment.ViewState as CardReaderPaymentViewState
import androidx.lifecycle.ViewModel
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentViewModel
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState
import com.woocommerce.android.ui.woopos.cardreader.IppPaymentStateObserver.PaymentFlowState.CollectPayment
import com.woocommerce.android.ui.woopos.cardreader.IppPaymentStateObserver.PaymentFlowState.DataLoading
import com.woocommerce.android.ui.woopos.cardreader.IppPaymentStateObserver.PaymentFlowState.PaymentCapturing
import com.woocommerce.android.ui.woopos.cardreader.IppPaymentStateObserver.PaymentFlowState.PaymentFailed
import com.woocommerce.android.ui.woopos.cardreader.IppPaymentStateObserver.PaymentFlowState.PaymentProcessing
import com.woocommerce.android.ui.woopos.cardreader.IppPaymentStateObserver.PaymentFlowState.PaymentSuccessful
import com.woocommerce.android.ui.woopos.cardreader.IppPaymentStateObserver.PaymentFlowState.RefreshingOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IppPaymentStateObserver @Inject constructor() {
    private val _state = MutableStateFlow<PaymentFlowState>(PaymentFlowState.Idle)
    val state: StateFlow<PaymentFlowState> = _state

    fun notify(state: CardReaderPaymentViewState, viewModel: ViewModel) {
        _state.value =
            when (state) {
                is CardReaderPaymentViewState -> {
                    when (state) {
                        is ViewState.LoadingDataState -> DataLoading {
                            (viewModel as CardReaderPaymentViewModel).onBackPressed()
                        }

                        is ViewState.ExternalReaderCapturingPaymentState -> PaymentCapturing
                        is ViewState.ExternalReaderCollectPaymentState -> CollectPayment(state) {
                            (viewModel as CardReaderPaymentViewModel).onBackPressed()
                        }

                        is ViewState.ExternalReaderFailedPaymentState -> PaymentFailed(state) {
                            // TODO: implement "try again"
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
        data class CollectPayment(val state: ViewState, val cancelPayment: () -> Unit) :
            PaymentFlowState()

        data class PaymentFailed(val state: ViewState, val tryAgain: () -> Unit) :
            PaymentFlowState()

        data class PaymentProcessing(val cancelPayment: () -> Unit) : PaymentFlowState()
        data object PaymentCapturing : PaymentFlowState()
        data object RefreshingOrder : PaymentFlowState()
        data object PaymentSuccessful : PaymentFlowState()
    }
}