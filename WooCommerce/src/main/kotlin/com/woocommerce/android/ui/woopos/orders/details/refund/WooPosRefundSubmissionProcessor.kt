package com.woocommerce.android.ui.woopos.orders.details.refund

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentEvent
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderInteracRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.ui.payments.refunds.PaymentChargeRepository
import com.woocommerce.android.ui.woopos.home.totals.WooPosCardReaderPaymentControllerFactory
import com.woocommerce.android.ui.woopos.orders.WooPosLoadPaymentGateway
import com.woocommerce.android.util.UiStringParser
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.refunds.RefundRequestItem
import org.wordpress.android.fluxc.store.WCRefundStore
import java.math.BigDecimal
import javax.inject.Inject

interface WooPosRefundSubmissionProcessor {
    fun submit(request: WooPosRefundSubmissionRequest): Flow<WooPosRefundSubmissionState>
}

data class WooPosRefundSubmissionRequest(
    val order: Order,
    val refundAmount: BigDecimal,
    val refundReason: String,
    val refundItems: List<RefundRequestItem>,
    val cardRefundAlreadySucceeded: Boolean = false,
) {
    val orderId: Long = order.id
}

sealed class WooPosRefundSubmissionState {
    data object Processing : WooPosRefundSubmissionState()
    data object PreparingReader : WooPosRefundSubmissionState()
    data object ReaderConnectionRequired : WooPosRefundSubmissionState()
    data class WaitingForCard(@StringRes val cardReaderHint: Int? = null) : WooPosRefundSubmissionState()
    data object ProcessingReaderRefund : WooPosRefundSubmissionState()
    data object NotifyingStore : WooPosRefundSubmissionState()
    data object Success : WooPosRefundSubmissionState()
    data class Failure(
        val message: String,
        val retryBackendNotificationOnly: Boolean = false,
        val retryCardRefund: Boolean = false,
        val canRetry: Boolean = !retryBackendNotificationOnly,
    ) : WooPosRefundSubmissionState()
}

@Suppress("LongParameterList")
class WooPosRefundSubmissionProcessorImpl @Inject constructor(
    private val refundStore: WCRefundStore,
    private val selectedSite: SelectedSite,
    private val paymentChargeRepository: PaymentChargeRepository,
    private val loadPaymentGateway: WooPosLoadPaymentGateway,
    private val cardReaderPaymentControllerFactory: WooPosCardReaderPaymentControllerFactory,
    private val resourceProvider: ResourceProvider,
    private val uiStringParser: UiStringParser,
) : WooPosRefundSubmissionProcessor {
    private var isTTPPaymentInProgress = false

    override fun submit(request: WooPosRefundSubmissionRequest): Flow<WooPosRefundSubmissionState> = channelFlow {
        WooLog.i(
            WooLog.T.POS,
            "WooPosRefund: submission started " +
                "orderId=${request.orderId}, " +
                "amount=${request.refundAmount}, " +
                "itemCount=${request.refundItems.size}, " +
                "hasChargeId=${request.order.chargeId != null}, " +
                "backendOnlyRetry=${request.cardRefundAlreadySucceeded}"
        )

        if (request.cardRefundAlreadySucceeded) {
            WooLog.i(
                WooLog.T.POS,
                "WooPosRefund: card refund already succeeded; retrying backend notification only " +
                    "orderId=${request.orderId}"
            )
            notifyBackend(request, retryBackendNotificationOnly = true)
            return@channelFlow
        }

        val paymentMethodType = request.order.chargeId?.let { chargeId ->
            when (val result = paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)) {
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Error -> {
                    WooLog.w(
                        WooLog.T.POS,
                        "WooPosRefund: failed to fetch payment charge metadata orderId=${request.orderId}"
                    )
                    null
                }
                is PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success -> {
                    WooLog.i(
                        WooLog.T.POS,
                        "WooPosRefund: fetched payment charge metadata " +
                            "orderId=${request.orderId}, paymentMethodType=${result.paymentMethodType}"
                    )
                    result.paymentMethodType
                }
            }
        } ?: run {
            WooLog.w(
                WooLog.T.POS,
                "WooPosRefund: order has no charge id; using backend refund path orderId=${request.orderId}"
            )
            null
        }

        if (paymentMethodType == INTERAC_PRESENT) {
            WooLog.i(WooLog.T.POS, "WooPosRefund: routing Interac refund through reader orderId=${request.orderId}")
            submitInteracRefund(request)
        } else {
            WooLog.i(
                WooLog.T.POS,
                "WooPosRefund: using backend refund path " +
                    "orderId=${request.orderId}, paymentMethodType=$paymentMethodType"
            )
            notifyBackend(request, retryBackendNotificationOnly = false)
        }
    }

    @Suppress("LongMethod")
    private suspend fun ProducerScope<WooPosRefundSubmissionState>.notifyBackend(
        request: WooPosRefundSubmissionRequest,
        retryBackendNotificationOnly: Boolean,
    ) {
        send(
            if (retryBackendNotificationOnly) {
                WooPosRefundSubmissionState.NotifyingStore
            } else {
                WooPosRefundSubmissionState.Processing
            }
        )

        val paymentGatewayResult = loadPaymentGateway(request.order)
        if (paymentGatewayResult.isFailure) {
            WooLog.e(
                WooLog.T.POS,
                "WooPosRefund: failed to load payment gateway " +
                    "orderId=${request.orderId}, backendOnlyRetry=$retryBackendNotificationOnly",
                paymentGatewayResult.exceptionOrNull()
            )
            send(
                WooPosRefundSubmissionState.Failure(
                    message = resourceProvider.getString(R.string.woopos_refund_error_gateway_not_found),
                    retryBackendNotificationOnly = retryBackendNotificationOnly,
                )
            )
            return
        }
        val paymentGateway = paymentGatewayResult.getOrThrow()

        WooLog.i(
            WooLog.T.POS,
            "WooPosRefund: creating backend refund " +
                "orderId=${request.orderId}, " +
                "amount=${request.refundAmount}, " +
                "itemCount=${request.refundItems.size}, " +
                "autoRefund=${paymentGateway.supportsRefunds}, " +
                "backendOnlyRetry=$retryBackendNotificationOnly"
        )

        val result = refundStore.createItemsRefund(
            site = selectedSite.get(),
            orderId = request.orderId,
            amount = request.refundAmount,
            reason = request.refundReason,
            restockItems = true,
            autoRefund = paymentGateway.supportsRefunds,
            items = request.refundItems
        )

        if (result.isError) {
            val error = result.error
            WooLog.e(
                WooLog.T.POS,
                "WooPosRefund: backend refund creation failed " +
                    "orderId=${request.orderId}, " +
                    "backendOnlyRetry=$retryBackendNotificationOnly, " +
                    "type=${error.type}, " +
                    "original=${error.original}, " +
                    "apiErrorCode=${error.apiErrorCode}, " +
                    "message=${error.message}, " +
                    "errorData=${error.errorData}"
            )
            send(
                WooPosRefundSubmissionState.Failure(
                    message = result.error.message ?: resourceProvider.getString(R.string.error_generic),
                    retryBackendNotificationOnly = retryBackendNotificationOnly,
                )
            )
        } else {
            WooLog.i(
                WooLog.T.POS,
                "WooPosRefund: backend refund creation succeeded " +
                    "orderId=${request.orderId}, backendOnlyRetry=$retryBackendNotificationOnly"
            )
            send(WooPosRefundSubmissionState.Success)
        }
    }

    @Suppress("LongMethod")
    private suspend fun ProducerScope<WooPosRefundSubmissionState>.submitInteracRefund(
        request: WooPosRefundSubmissionRequest,
    ) {
        isTTPPaymentInProgress = false
        WooLog.i(
            WooLog.T.POS,
            "WooPosRefund: creating Interac reader refund controller orderId=${request.orderId}"
        )
        val controller = cardReaderPaymentControllerFactory.createRefund(
            orderId = request.orderId,
            refundAmount = request.refundAmount,
            cardReaderType = CardReaderType.EXTERNAL,
            isTTPPaymentInProgress = ::isTTPPaymentInProgress,
        )
        var terminalRefundSucceeded = false

        val stateJob = launch {
            controller.paymentState.collect { state ->
                when (state) {
                    is CardReaderInteracRefundState.LoadingData -> {
                        WooLog.i(WooLog.T.POS, "WooPosRefund: reader refund loading data orderId=${request.orderId}")
                        trySendState(WooPosRefundSubmissionState.PreparingReader)
                    }

                    is CardReaderInteracRefundState.CollectingInteracRefund -> {
                        WooLog.i(
                            WooLog.T.POS,
                            "WooPosRefund: reader refund collecting card " +
                                "orderId=${request.orderId}, hint=${state.cardReaderHint}"
                        )
                        trySendState(WooPosRefundSubmissionState.WaitingForCard(state.cardReaderHint))
                    }

                    is CardReaderInteracRefundState.ProcessingInteracRefund -> {
                        WooLog.i(WooLog.T.POS, "WooPosRefund: reader refund processing orderId=${request.orderId}")
                        trySendState(WooPosRefundSubmissionState.ProcessingReaderRefund)
                    }

                    is CardReaderInteracRefundState.InteracRefundSuccessful -> {
                        if (!terminalRefundSucceeded) {
                            WooLog.i(
                                WooLog.T.POS,
                                "WooPosRefund: reader refund succeeded; notifying backend orderId=${request.orderId}"
                            )
                            terminalRefundSucceeded = true
                            notifyBackend(
                                request = request.copy(cardRefundAlreadySucceeded = true),
                                retryBackendNotificationOnly = true
                            )
                            close()
                        }
                    }

                    is CardReaderInteracRefundState.InteracRefundFailure -> {
                        WooLog.e(
                            WooLog.T.POS,
                            "WooPosRefund: reader refund failed " +
                                "orderId=${request.orderId}, " +
                                "errorType=${state.errorType}, " +
                                "retryable=${state.onRetry != null}"
                        )
                        trySendState(
                            WooPosRefundSubmissionState.Failure(
                                message = uiStringParser.asString(state.errorType.message),
                                retryCardRefund = state.onRetry != null,
                            )
                        )
                        close()
                    }

                    is CardReaderPaymentState.LoadingData -> {
                        WooLog.i(
                            WooLog.T.POS,
                            "WooPosRefund: ignoring initial payment loading state for refund orderId=${request.orderId}"
                        )
                    }

                    is CardReaderPaymentState -> {
                        WooLog.e(
                            WooLog.T.POS,
                            "WooPosRefund: payment state emitted during refund " +
                                "orderId=${request.orderId}, state=${state::class.simpleName}"
                        )
                        trySendState(
                            WooPosRefundSubmissionState.Failure(
                                message = resourceProvider.getString(R.string.error_generic),
                            )
                        )
                        close()
                    }
                }
            }
        }

        val eventJob = launch {
            controller.event.collect { event ->
                when (event) {
                    is CardReaderPaymentEvent.ShowErrorMessage -> {
                        val message = resourceProvider.getString(event.message)
                        if (event.message == R.string.card_reader_payment_reader_not_connected) {
                            WooLog.i(
                                WooLog.T.POS,
                                "WooPosRefund: reader connection required for refund " +
                                    "orderId=${request.orderId}, messageRes=${event.message}, message=$message"
                            )
                            trySendState(WooPosRefundSubmissionState.ReaderConnectionRequired)
                        } else {
                            WooLog.e(
                                WooLog.T.POS,
                                "WooPosRefund: reader refund controller error event " +
                                    "orderId=${request.orderId}, messageRes=${event.message}, message=$message"
                            )
                            trySendState(
                                WooPosRefundSubmissionState.Failure(
                                    message = message,
                                )
                            )
                        }
                        close()
                    }

                    is CardReaderPaymentEvent.ShowPaymentErrorMessage -> {
                        val message = resourceProvider.getString(event.message)
                        WooLog.e(
                            WooLog.T.POS,
                            "WooPosRefund: reader refund controller payment error event " +
                                "orderId=${request.orderId}, messageRes=${event.message}, message=$message"
                        )
                        trySendState(
                            WooPosRefundSubmissionState.Failure(
                                message = message,
                            )
                        )
                        close()
                    }

                    CardReaderPaymentEvent.Exit -> {
                        WooLog.i(
                            WooLog.T.POS,
                            "WooPosRefund: reader refund controller exit event ignored orderId=${request.orderId}"
                        )
                    }

                    CardReaderPaymentEvent.InteracRefundSuccessful,
                    CardReaderPaymentEvent.PlaySuccessfulPaymentSound,
                    is CardReaderPaymentEvent.ContactSupportTapped,
                    is CardReaderPaymentEvent.EnableNfcTapped,
                    is CardReaderPaymentEvent.PrintReceiptTapped,
                    is CardReaderPaymentEvent.PurchaseCardReaderTapped -> Unit
                }
            }
        }

        WooLog.i(WooLog.T.POS, "WooPosRefund: starting Interac reader refund orderId=${request.orderId}")
        controller.start()

        awaitClose {
            WooLog.i(WooLog.T.POS, "WooPosRefund: closing Interac reader refund orderId=${request.orderId}")
            stateJob.cancel()
            eventJob.cancel()
            controller.stop()
        }
    }

    private fun ProducerScope<WooPosRefundSubmissionState>.trySendState(
        state: WooPosRefundSubmissionState
    ) {
        trySend(state)
    }

    private companion object {
        private const val INTERAC_PRESENT = "interac_present"
    }
}
