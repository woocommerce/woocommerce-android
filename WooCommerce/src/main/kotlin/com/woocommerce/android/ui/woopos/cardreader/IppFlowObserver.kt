package com.woocommerce.android.ui.woopos.cardreader

import com.woocommerce.android.ui.payments.cardreader.payment.ViewState as CardReaderPaymentViewState
import androidx.lifecycle.ViewModel
import com.woocommerce.android.ui.payments.cardreader.connect.CardReaderConnectViewModel
import com.woocommerce.android.ui.payments.cardreader.connect.CardReaderConnectViewState
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
        _state.value =
            when (event) {
                is CardReaderPaymentViewState -> {
                    when (event) {
                        is ViewState.LoadingDataState -> DataLoading {
                            (viewModel as CardReaderPaymentViewModel).onBackPressed()
                        }

                        is ViewState.ExternalReaderCapturingPaymentState -> PaymentCapturing
                        is ViewState.ExternalReaderCollectPaymentState -> CollectPayment(event) {
                            (viewModel as CardReaderPaymentViewModel).onBackPressed()
                        }

                        is ViewState.ExternalReaderFailedPaymentState -> PaymentFailed(event) {
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
                is CardReaderConnectViewState -> {
                    when (event) {
                        is CardReaderConnectViewState.BuiltInReaderScanningState -> TODO("Not yet implemented")
                        is CardReaderConnectViewState.ExternalReaderScanningState -> PaymentFlowState.ScanningForReader
                        is CardReaderConnectViewState.ExternalReaderFoundState -> PaymentFlowState.ReaderFound
                        is CardReaderConnectViewState.MultipleExternalReadersFoundState -> PaymentFlowState.MultipleReadersFound
                        is CardReaderConnectViewState.ExternalReaderConnectingState -> PaymentFlowState.ConnectingToReader
                        is CardReaderConnectViewState.BuiltInReaderConnectingState -> TODO("Not yet implemented")
                        is CardReaderConnectViewState.ScanningFailedState -> PaymentFlowState.ScanningForReaderFailed
                        is CardReaderConnectViewState.ConnectingFailedState -> PaymentFlowState.ConnectingToReaderFailed
                        is CardReaderConnectViewState.LocationPermissionRationale -> TODO("Not yet implemented")
                        is CardReaderConnectViewState.MissingLocationPermissionsError -> TODO("Not yet implemented")
                        is CardReaderConnectViewState.LocationDisabledError -> TODO("Not yet implemented")
                        is CardReaderConnectViewState.BluetoothDisabledError -> TODO("Not yet implemented")
                        is CardReaderConnectViewState.MissingBluetoothPermissionsError -> TODO("Not yet implemented")
                        is CardReaderConnectViewState.MissingMerchantAddressError -> TODO("Not yet implemented")
                        is CardReaderConnectViewState.InvalidMerchantAddressPostCodeError -> TODO("Not yet implemented")
                    }
                }
                else -> {
                    throw IllegalStateException("Unhandled event type: $event")
                }
            }
    }

    sealed class PaymentFlowState {
        data object Idle : PaymentFlowState()
        data object ScanningForReader : PaymentFlowState()
        data object ScanningForReaderFailed : PaymentFlowState()
        data object ReaderFound : PaymentFlowState()
        data object MultipleReadersFound : PaymentFlowState()
        data object ConnectingToReader : PaymentFlowState()
        data object ConnectingToReaderFailed : PaymentFlowState()
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