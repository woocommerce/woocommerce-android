package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentController
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentEvent
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderInteracRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.ui.payments.refunds.PaymentChargeRepository
import com.woocommerce.android.ui.woopos.home.totals.WooPosCardReaderPaymentControllerFactory
import com.woocommerce.android.ui.woopos.orders.WooPosLoadPaymentGateway
import com.woocommerce.android.util.UiStringParser
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCRefundStore
import javax.inject.Inject

private sealed class RefundSubmissionPath {
    data object Interac : RefundSubmissionPath()
    data class Backend(val paymentMethodType: String?) : RefundSubmissionPath()
    data object PaymentMetadataUnavailable : RefundSubmissionPath()
}

@Suppress("LongParameterList")
class WooPosRefundSubmissionProcessor @Inject constructor(
    private val refundStore: WCRefundStore,
    private val selectedSite: SelectedSite,
    private val paymentChargeRepository: PaymentChargeRepository,
    private val loadPaymentGateway: WooPosLoadPaymentGateway,
    private val cardReaderPaymentControllerFactory: WooPosCardReaderPaymentControllerFactory,
    private val resourceProvider: ResourceProvider,
    private val uiStringParser: UiStringParser,
) {
    @Suppress("TooGenericExceptionCaught")
    fun submit(request: WooPosRefundSubmissionRequest): Flow<WooPosRefundSubmissionState> = channelFlow {
        try {
            logSubmissionStarted(request)

            if (request.cardRefundAlreadySucceeded) {
                WooLog.i(
                    WooLog.T.POS,
                    "WooPosRefund: card refund already succeeded; notifying backend only " +
                        "orderId=${request.orderId}"
                )
                notifyBackend(request, retryBackendNotificationOnly = true)
                return@channelFlow
            }

            when (val submissionPath = resolveSubmissionPath(request)) {
                RefundSubmissionPath.Interac -> {
                    WooLog.i(
                        WooLog.T.POS,
                        "WooPosRefund: routing Interac refund through reader orderId=${request.orderId}"
                    )
                    submitInteracRefund(request)
                }
                is RefundSubmissionPath.Backend -> {
                    WooLog.i(
                        WooLog.T.POS,
                        "WooPosRefund: using backend refund path " +
                            "orderId=${request.orderId}, paymentMethodType=${submissionPath.paymentMethodType}"
                    )
                    notifyBackend(request, retryBackendNotificationOnly = false)
                }
                RefundSubmissionPath.PaymentMetadataUnavailable -> {
                    trySendState(
                        WooPosRefundSubmissionState.Failure(
                            message = resourceProvider.getString(R.string.error_generic),
                        )
                    )
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WooLog.e(
                WooLog.T.POS,
                "WooPosRefund: submission failed unexpectedly orderId=${request.orderId}",
                exception
            )
            trySendState(
                WooPosRefundSubmissionState.Failure(
                    message = resourceProvider.getString(R.string.error_generic),
                )
            )
        }
    }

    private fun logSubmissionStarted(request: WooPosRefundSubmissionRequest) {
        WooLog.i(
            WooLog.T.POS,
            "WooPosRefund: submission started " +
                "orderId=${request.orderId}, " +
                "amount=${request.refundAmount}, " +
                "itemCount=${request.refundItems.size}, " +
                "hasChargeId=${request.order.chargeId != null}, " +
                "backendOnlyRetry=${request.cardRefundAlreadySucceeded}"
        )
    }

    private suspend fun resolveSubmissionPath(
        request: WooPosRefundSubmissionRequest
    ): RefundSubmissionPath {
        val chargeId = request.order.chargeId ?: run {
            WooLog.w(
                WooLog.T.POS,
                "WooPosRefund: order has no charge id; using backend refund path orderId=${request.orderId}"
            )
            return RefundSubmissionPath.Backend(paymentMethodType = null)
        }

        return when (val result = paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)) {
            PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Error -> {
                WooLog.w(
                    WooLog.T.POS,
                    "WooPosRefund: failed to fetch payment charge metadata; failing refund " +
                        "orderId=${request.orderId}"
                )
                RefundSubmissionPath.PaymentMetadataUnavailable
            }
            is PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success -> {
                WooLog.i(
                    WooLog.T.POS,
                    "WooPosRefund: fetched payment charge metadata " +
                        "orderId=${request.orderId}, paymentMethodType=${result.paymentMethodType}"
                )
                if (result.paymentMethodType == INTERAC_PRESENT) {
                    RefundSubmissionPath.Interac
                } else {
                    RefundSubmissionPath.Backend(paymentMethodType = result.paymentMethodType)
                }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun ProducerScope<WooPosRefundSubmissionState>.notifyBackend(
        request: WooPosRefundSubmissionRequest,
        retryBackendNotificationOnly: Boolean,
    ) {
        try {
            sendBackendNotificationStarted(retryBackendNotificationOnly)

            val shouldAutoRefund = resolveShouldAutoRefund(
                request = request,
                retryBackendNotificationOnly = retryBackendNotificationOnly
            ) ?: return

            val result = createBackendRefund(
                request = request,
                shouldAutoRefund = shouldAutoRefund,
                retryBackendNotificationOnly = retryBackendNotificationOnly
            )
            handleBackendRefundResult(
                request = request,
                result = result,
                retryBackendNotificationOnly = retryBackendNotificationOnly
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WooLog.e(
                WooLog.T.POS,
                "WooPosRefund: backend refund creation failed unexpectedly " +
                    "orderId=${request.orderId}, backendOnlyRetry=$retryBackendNotificationOnly",
                exception
            )
            trySendState(
                WooPosRefundSubmissionState.Failure(
                    message = resourceProvider.getString(R.string.error_generic),
                    retryBackendNotificationOnly = retryBackendNotificationOnly,
                )
            )
        }
    }

    private fun ProducerScope<WooPosRefundSubmissionState>.sendBackendNotificationStarted(
        retryBackendNotificationOnly: Boolean,
    ) {
        trySendState(
            if (retryBackendNotificationOnly) {
                WooPosRefundSubmissionState.NotifyingStore
            } else {
                WooPosRefundSubmissionState.Processing
            }
        )
    }

    private suspend fun ProducerScope<WooPosRefundSubmissionState>.resolveShouldAutoRefund(
        request: WooPosRefundSubmissionRequest,
        retryBackendNotificationOnly: Boolean,
    ): Boolean? {
        if (retryBackendNotificationOnly) {
            return false
        }

        val paymentGatewayResult = loadPaymentGateway(request.order)
        if (paymentGatewayResult.isFailure) {
            WooLog.e(
                WooLog.T.POS,
                "WooPosRefund: failed to load payment gateway " +
                    "orderId=${request.orderId}, backendOnlyRetry=false",
                paymentGatewayResult.exceptionOrNull()
            )
            trySendState(
                WooPosRefundSubmissionState.Failure(
                    message = resourceProvider.getString(R.string.woopos_refund_error_gateway_not_found),
                )
            )
            return null
        }
        return paymentGatewayResult.getOrThrow().supportsRefunds
    }

    private suspend fun createBackendRefund(
        request: WooPosRefundSubmissionRequest,
        shouldAutoRefund: Boolean,
        retryBackendNotificationOnly: Boolean,
    ): WooResult<WCRefundModel> {
        WooLog.i(
            WooLog.T.POS,
            "WooPosRefund: creating backend refund " +
                "orderId=${request.orderId}, " +
                "amount=${request.refundAmount}, " +
                "itemCount=${request.refundItems.size}, " +
                "autoRefund=$shouldAutoRefund, " +
                "backendOnlyRetry=$retryBackendNotificationOnly"
        )

        return refundStore.createItemsRefund(
            site = selectedSite.get(),
            orderId = request.orderId,
            amount = request.refundAmount,
            reason = request.refundReason,
            restockItems = true,
            autoRefund = shouldAutoRefund,
            items = request.refundItems
        )
    }

    private fun ProducerScope<WooPosRefundSubmissionState>.handleBackendRefundResult(
        request: WooPosRefundSubmissionRequest,
        result: WooResult<WCRefundModel>,
        retryBackendNotificationOnly: Boolean,
    ) {
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
            trySendState(
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
            trySendState(WooPosRefundSubmissionState.Success)
        }
    }

    private suspend fun ProducerScope<WooPosRefundSubmissionState>.submitInteracRefund(
        request: WooPosRefundSubmissionRequest,
    ) {
        val controller = createInteracRefundController(request)
        val refundSessionState = InteracRefundSessionState()
        val stateJob = listenToInteracRefundState(request, controller, refundSessionState)
        val eventJob = listenToInteracRefundEvents(request, controller, refundSessionState)

        WooLog.i(WooLog.T.POS, "WooPosRefund: starting Interac reader refund orderId=${request.orderId}")
        controller.start()

        awaitClose {
            WooLog.i(WooLog.T.POS, "WooPosRefund: closing Interac reader refund orderId=${request.orderId}")
            stateJob.cancel()
            eventJob.cancel()
            refundSessionState.exitFallbackJob?.cancel()
            controller.stop()
        }
    }

    private fun createInteracRefundController(
        request: WooPosRefundSubmissionRequest,
    ): CardReaderPaymentController {
        val ttpPaymentProgress = object {
            var isInProgress = false
        }
        WooLog.i(
            WooLog.T.POS,
            "WooPosRefund: creating Interac reader refund controller orderId=${request.orderId}"
        )
        return cardReaderPaymentControllerFactory.createRefund(
            orderId = request.orderId,
            refundAmount = request.refundAmount,
            isTTPPaymentInProgress = ttpPaymentProgress::isInProgress,
        )
    }

    private fun ProducerScope<WooPosRefundSubmissionState>.listenToInteracRefundState(
        request: WooPosRefundSubmissionRequest,
        controller: CardReaderPaymentController,
        refundSessionState: InteracRefundSessionState,
    ): Job {
        return launch {
            controller.paymentState.collect { state ->
                handleInteracRefundState(request, state, refundSessionState)
            }
        }
    }

    private suspend fun ProducerScope<WooPosRefundSubmissionState>.handleInteracRefundState(
        request: WooPosRefundSubmissionRequest,
        state: CardReaderPaymentOrRefundState,
        refundSessionState: InteracRefundSessionState,
    ) {
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
                handleSuccessfulInteracRefund(request, refundSessionState)
            }

            is CardReaderInteracRefundState.InteracRefundFailure -> {
                handleFailedInteracRefund(request, state, refundSessionState)
            }

            is CardReaderPaymentState.LoadingData -> {
                WooLog.i(
                    WooLog.T.POS,
                    "WooPosRefund: ignoring initial payment loading state for refund orderId=${request.orderId}"
                )
            }

            is CardReaderPaymentState -> {
                handleUnexpectedPaymentState(request, state, refundSessionState)
            }
        }
    }

    private suspend fun ProducerScope<WooPosRefundSubmissionState>.handleSuccessfulInteracRefund(
        request: WooPosRefundSubmissionRequest,
        refundSessionState: InteracRefundSessionState,
    ) {
        if (refundSessionState.terminalRefundSucceeded) {
            return
        }

        WooLog.i(
            WooLog.T.POS,
            "WooPosRefund: reader refund succeeded; notifying backend orderId=${request.orderId}"
        )
        refundSessionState.terminalRefundSucceeded = true
        notifyBackend(
            request = request.copy(cardRefundAlreadySucceeded = true),
            retryBackendNotificationOnly = true
        )
        refundSessionState.markTerminalStateSent()
        close()
    }

    private fun ProducerScope<WooPosRefundSubmissionState>.handleFailedInteracRefund(
        request: WooPosRefundSubmissionRequest,
        state: CardReaderInteracRefundState.InteracRefundFailure,
        refundSessionState: InteracRefundSessionState,
    ) {
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
                canRetry = state.onRetry != null,
            )
        )
        refundSessionState.markTerminalStateSent()
        close()
    }

    private fun ProducerScope<WooPosRefundSubmissionState>.handleUnexpectedPaymentState(
        request: WooPosRefundSubmissionRequest,
        state: CardReaderPaymentState,
        refundSessionState: InteracRefundSessionState,
    ) {
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
        refundSessionState.markTerminalStateSent()
        close()
    }

    private fun ProducerScope<WooPosRefundSubmissionState>.listenToInteracRefundEvents(
        request: WooPosRefundSubmissionRequest,
        controller: CardReaderPaymentController,
        refundSessionState: InteracRefundSessionState,
    ): Job {
        return launch {
            controller.event.collect { event ->
                handleInteracRefundEvent(request, event, refundSessionState)
            }
        }
    }

    private fun ProducerScope<WooPosRefundSubmissionState>.handleInteracRefundEvent(
        request: WooPosRefundSubmissionRequest,
        event: CardReaderPaymentEvent,
        refundSessionState: InteracRefundSessionState,
    ) {
        when (event) {
            is CardReaderPaymentEvent.ShowErrorMessage -> {
                handleReaderErrorEvent(request, event, refundSessionState)
            }

            is CardReaderPaymentEvent.ShowPaymentErrorMessage -> {
                handleReaderPaymentErrorEvent(request, event, refundSessionState)
            }

            CardReaderPaymentEvent.Exit -> {
                handleReaderExitEvent(request, refundSessionState)
            }

            CardReaderPaymentEvent.InteracRefundSuccessful,
            CardReaderPaymentEvent.PlaySuccessfulPaymentSound,
            is CardReaderPaymentEvent.ContactSupportTapped,
            is CardReaderPaymentEvent.EnableNfcTapped,
            is CardReaderPaymentEvent.PrintReceiptTapped,
            is CardReaderPaymentEvent.PurchaseCardReaderTapped -> Unit
        }
    }

    private fun ProducerScope<WooPosRefundSubmissionState>.handleReaderErrorEvent(
        request: WooPosRefundSubmissionRequest,
        event: CardReaderPaymentEvent.ShowErrorMessage,
        refundSessionState: InteracRefundSessionState,
    ) {
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
        refundSessionState.markTerminalStateSent()
        close()
    }

    private fun ProducerScope<WooPosRefundSubmissionState>.handleReaderPaymentErrorEvent(
        request: WooPosRefundSubmissionRequest,
        event: CardReaderPaymentEvent.ShowPaymentErrorMessage,
        refundSessionState: InteracRefundSessionState,
    ) {
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
        refundSessionState.markTerminalStateSent()
        close()
    }

    private fun ProducerScope<WooPosRefundSubmissionState>.handleReaderExitEvent(
        request: WooPosRefundSubmissionRequest,
        refundSessionState: InteracRefundSessionState,
    ) {
        if (refundSessionState.terminalStateSent || refundSessionState.terminalRefundSucceeded) {
            WooLog.i(
                WooLog.T.POS,
                "WooPosRefund: reader refund controller exit event ignored orderId=${request.orderId}"
            )
            return
        }

        WooLog.i(
            WooLog.T.POS,
            "WooPosRefund: reader refund controller exit event received; " +
                "waiting for paired error orderId=${request.orderId}"
        )
        refundSessionState.exitFallbackJob?.cancel()
        refundSessionState.exitFallbackJob = launch {
            delay(EXIT_EVENT_FALLBACK_DELAY_MS)
            if (!refundSessionState.terminalStateSent && !refundSessionState.terminalRefundSucceeded) {
                WooLog.w(
                    WooLog.T.POS,
                    "WooPosRefund: reader refund controller exited without terminal state " +
                        "orderId=${request.orderId}"
                )
                refundSessionState.terminalStateSent = true
                refundSessionState.exitFallbackJob = null
                trySendState(
                    WooPosRefundSubmissionState.Failure(
                        message = resourceProvider.getString(R.string.error_generic),
                        canRetry = false,
                    )
                )
                close()
            }
        }
    }

    private fun ProducerScope<WooPosRefundSubmissionState>.trySendState(
        state: WooPosRefundSubmissionState
    ) {
        trySend(state)
    }

    private class InteracRefundSessionState {
        var terminalRefundSucceeded = false
        var terminalStateSent = false
        var exitFallbackJob: Job? = null

        fun markTerminalStateSent() {
            terminalStateSent = true
            exitFallbackJob?.cancel()
            exitFallbackJob = null
        }
    }

    private companion object {
        private const val INTERAC_PRESENT = "interac_present"
        private const val EXIT_EVENT_FALLBACK_DELAY_MS = 100L
    }
}
