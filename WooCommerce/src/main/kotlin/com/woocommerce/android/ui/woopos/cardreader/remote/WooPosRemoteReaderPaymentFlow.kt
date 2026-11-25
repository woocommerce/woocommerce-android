package com.woocommerce.android.ui.woopos.cardreader.remote

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.cardreader.describeWithCauses
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentMethodType
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.cardreader.payments.StatementDescriptor
import com.woocommerce.android.cardreader.remote.CardReaderRemoteConnectionLostException
import com.woocommerce.android.cardreader.remote.CardReaderRemoteError
import com.woocommerce.android.cardreader.remote.CollectPaymentOutcome
import com.woocommerce.android.cardreader.remote.RemotePaymentCancellation
import com.woocommerce.android.di.PointOfSaleMode
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentOrderHelper
import com.woocommerce.android.ui.payments.cardreader.payment.TerminalPaymentIntentConfig
import com.woocommerce.android.ui.payments.cardreader.payment.TerminalPaymentPreparationResolver
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptHelper
import com.woocommerce.android.ui.payments.tracking.CardReaderTrackingInfoKeeper
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.CardReaderTransport
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.TAP_TO_PAY_READER_MODEL
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooPosRemoteReaderPaymentFlow @Inject constructor(
    private val cardReaderStore: CardReaderStore,
    private val remoteReaderSession: WooPosRemoteReaderSession,
    private val selectedSite: SelectedSite,
    private val cardReaderPaymentOrderHelper: CardReaderPaymentOrderHelper,
    private val paymentReceiptHelper: PaymentReceiptHelper,
    private val wooStore: WooCommerceStore,
    private val resourceProvider: ResourceProvider,
    private val logger: WooPosLogWrapper,
    private val errorMapper: WooPosRemoteReaderErrorMapper,
    private val trackingInfoKeeper: CardReaderTrackingInfoKeeper,
    @PointOfSaleMode private val paymentsFlowTracker: PaymentsFlowTracker,
    private val appPrefs: AppPrefs = AppPrefs,
    private val cardReaderFacade: WooPosCardReaderFacade,
    private val featureFlags: FeatureFlagRepository,
    private val orderRepository: OrderDetailRepository,
) {
    private val terminalPaymentPreparationResolver = TerminalPaymentPreparationResolver(wooStore, appPrefs)

    suspend fun collect(
        order: Order,
        onCaptureStarting: suspend () -> Unit = {},
        onPaymentReconciled: suspend () -> Unit = {},
    ): Result {
        val attempt = PaymentAttempt(order, onPaymentReconciled)
        val trackPayment = featureFlags.isEnabled(FeatureFlag.WOO_POS_ROLES) || cardReaderFacade.isPaymentPending
        if (trackPayment && !attempt.acquire()) return Result.Failed(genericFailureMessage(), "payment_unresolved")
        return try {
            collectPayment(order, {
                attempt.readerCompleted = true
                onCaptureStarting()
            }, attempt::beginSend).also { attempt.finish(it) }
        } catch (error: CancellationException) {
            attempt.owner?.let(cardReaderFacade::markUnresolved)
            throw error
        } finally {
            attempt.finished.complete(Unit)
        }
    }

    private inner class PaymentAttempt(
        private val order: Order,
        private val onPaymentReconciled: suspend () -> Unit,
    ) {
        val finished = CompletableDeferred<Unit>()
        private val siteId = selectedSite.getOrNull()?.id
        private var cancellationRequested = false
        private var requestSent = false
        var readerCompleted = false
        private var paymentReported = false
        var owner: WooPosCardReaderFacade.PaymentOwner? = null
            private set

        fun acquire(): Boolean {
            owner = cardReaderFacade.beginPayment(::cancel)
            return owner != null
        }

        fun beginSend(): Boolean {
            if (cancellationRequested || selectedSite.getOrNull()?.id != siteId) return false
            requestSent = true
            return true
        }

        suspend fun finish(result: Result) {
            owner?.let {
                if (result is Result.Failed && result.isUnresolved) {
                    cardReaderFacade.markUnresolved(it)
                } else {
                    cardReaderFacade.finishPayment(it, successful = result == Result.Completed)
                }
            }
            if (result == Result.Completed) paymentReported = true
        }

        private suspend fun reportPayment() {
            if (paymentReported || selectedSite.getOrNull()?.id != siteId) return
            paymentReported = true
            onPaymentReconciled()
        }

        private suspend fun reconcilePaidOrder(): Boolean {
            if (!readerCompleted || !finished.isCompleted || selectedSite.getOrNull()?.id != siteId) return false
            val paid = orderRepository.fetchOrderById(order.id)?.isOrderPaid == true
            if (!paid || selectedSite.getOrNull()?.id != siteId) return false
            owner?.let { cardReaderFacade.finishPayment(it, successful = true) }
            reportPayment()
            return true
        }

        private suspend fun cancel(): Boolean {
            cancellationRequested = true
            if (!requestSent) return true
            if (isSimulatedReaderConnected()) {
                finished.await()
                return true
            }
            return when (remoteReaderSession.cancelPayment()) {
                RemotePaymentCancellation.CONFIRMED -> {
                    finished.await()
                    true
                }
                RemotePaymentCancellation.FAILED -> reconcilePaidOrder()
                RemotePaymentCancellation.UNSUPPORTED -> {
                    finished.await()
                    reconcilePaidOrder()
                }
                RemotePaymentCancellation.UNCONFIRMED -> {
                    owner?.let(cardReaderFacade::markCancellationUnconfirmed)
                    false
                }
            }
        }
    }

    private suspend fun collectPayment(
        order: Order,
        onCaptureStarting: suspend () -> Unit,
        canSend: () -> Boolean,
    ): Result {
        trackingInfoKeeper.setCurrency(order.currency)
        trackingInfoKeeper.setPaymentMethodType(PaymentMethodType.UNKNOWN.stringRepresentation)
        trackingInfoKeeper.setCardReaderModel(TAP_TO_PAY_READER_MODEL)
        trackingInfoKeeper.setTransport(CardReaderTransport.WIFI_LAN.value)
        val result = collectInternal(order, onCaptureStarting, canSend)
        when (result) {
            Result.Completed -> paymentsFlowTracker.trackPaymentSucceeded(order)
            is Result.Failed -> paymentsFlowTracker.trackPaymentFailed(
                errorMessage = result.errorDescription,
                errorType = result.errorType,
                stripeErrorCode = result.stripeErrorCode,
            )
        }
        return result
    }

    private suspend fun collectInternal(
        order: Order,
        onCaptureStarting: suspend () -> Unit,
        canSend: () -> Boolean,
    ): Result {
        if (isSimulatedReaderConnected()) {
            return if (canSend()) simulatePayment(onCaptureStarting) else canceledBeforeSend()
        }

        val site = selectedSite.get()
        val countryCode = wooStore.getStoreCountryCode(site) ?: run {
            logger.e("Remote payment aborted: store country code unavailable")
            return Result.Failed(genericFailureMessage(), "store_country_code_unavailable")
        }

        val paymentInfo = PaymentInfo(
            paymentDescription = cardReaderPaymentOrderHelper.getPaymentDescription(order),
            statementDescriptor = StatementDescriptor(
                appPrefs.getCardReaderStatementDescriptor(
                    localSiteId = site.id,
                    remoteSiteId = site.siteId,
                    selfHostedSiteId = site.selfHostedSiteId,
                )
            ),
            orderId = order.id,
            amount = order.total,
            currency = order.currency,
            orderKey = order.orderKey,
            customerEmail = order.billingAddress.email.ifEmpty { null },
            isPluginCanSendReceipt = paymentReceiptHelper.isPluginCanSendReceipt(site),
            customerName = "${order.billingAddress.firstName} ${order.billingAddress.lastName}"
                .trim()
                .ifBlank { null },
            storeName = site.name.ifEmpty { null },
            siteUrl = site.url.ifEmpty { null },
            countryCode = countryCode,
            feeAmount = TerminalPaymentIntentConfig.calculateApplicationFeeInCents(
                countryCode = countryCode,
                orderTotal = order.total,
                currencyCode = order.currency,
            ),
            channel = PaymentInfo.PaymentChannel.Pos,
            cardPresentCaptureMethod = TerminalPaymentIntentConfig.cardPresentCaptureMethod(countryCode),
            terminalPaymentPreparation = terminalPaymentPreparationResolver.resolve(
                countryCode = countryCode,
                site = site,
                onRouteCheckFailed = ::logTerminalPaymentPreparationRouteCheckFailed,
            ),
        )

        return if (canSend()) {
            collectRemotePayment(site.id, order.id, paymentInfo, onCaptureStarting)
        } else {
            canceledBeforeSend()
        }
    }

    private suspend fun collectRemotePayment(
        localSiteId: Int,
        orderId: Long,
        paymentInfo: PaymentInfo,
        onCaptureStarting: suspend () -> Unit,
    ): Result {
        return when (val outcome = remoteReaderSession.sendCollectPayment(paymentInfo)) {
            is CollectPaymentOutcome.Success -> {
                if (selectedSite.getOrNull()?.id != localSiteId) {
                    return Result.Failed(genericFailureMessage(), "payment_store_changed", isUnresolved = true)
                }
                if (outcome.status == "requires_capture" || outcome.status == "succeeded") {
                    paymentsFlowTracker.onPaymentCapturing()
                }
                trackingInfoKeeper.setPaymentMethodType(
                    (outcome.paymentMethodType ?: PaymentMethodType.UNKNOWN).stringRepresentation
                )
                onCaptureStarting()
                if (selectedSite.getOrNull()?.id != localSiteId) {
                    Result.Failed(genericFailureMessage(), "payment_store_changed", isUnresolved = true)
                } else {
                    capture(orderId, outcome.paymentIntentId)
                }
            }
            is CollectPaymentOutcome.Rejected -> {
                logger.e(
                    "Remote payment rejected: ${outcome.error.code} " +
                        "(stripe: ${outcome.stripeErrorCode}) - ${outcome.description}"
                )
                Result.Failed(
                    message = errorMapper.toUserMessage(
                        error = outcome.error,
                        fallback = R.string.woopos_remote_payment_failed_generic,
                    ),
                    errorDescription = "remote_rejected: ${outcome.error.code} - ${outcome.description}",
                    errorType = outcome.error.toPaymentErrorType(),
                    stripeErrorCode = outcome.stripeErrorCode,
                    isUnresolved = outcome.error == CardReaderRemoteError.UnexpectedReply ||
                        outcome.error is CardReaderRemoteError.Unknown ||
                        (
                            outcome.error == CardReaderRemoteError.CollectFailed && outcome.stripeErrorCode !in setOf(
                                "DECLINED_BY_STRIPE_API", "DECLINED_BY_READER",
                            )
                            ),
                )
            }
            CollectPaymentOutcome.TimedOut -> {
                logger.e("Remote payment timed out waiting for phone reader")
                Result.Failed(
                    message = genericFailureMessage(),
                    errorDescription = "remote_timed_out: phone reader did not reply in time",
                    isUnresolved = true,
                )
            }
            is CollectPaymentOutcome.Failed -> {
                logger.e("Remote payment failed - ${outcome.cause.message}", outcome.cause)
                Result.Failed(
                    message = mapFailureToUserMessage(outcome.cause),
                    errorDescription = "remote_failed: ${outcome.cause.describeWithCauses()}",
                    isUnresolved = true,
                )
            }
        }
    }

    private fun canceledBeforeSend() = Result.Failed(
        genericFailureMessage(),
        "payment_canceled_before_send",
        CardPaymentStatusErrorType.Canceled,
    )

    private fun CardReaderRemoteError.toPaymentErrorType(): CardPaymentStatusErrorType = when (this) {
        CardReaderRemoteError.Canceled -> CardPaymentStatusErrorType.Canceled
        CardReaderRemoteError.CardReadTimedOut -> CardPaymentStatusErrorType.CardReadTimeOut
        CardReaderRemoteError.PhoneNotEligible,
        CardReaderRemoteError.NfcDisabled,
        CardReaderRemoteError.InsecureEnvironment,
        CardReaderRemoteError.TokenInvalid,
        CardReaderRemoteError.ConnectFailed,
        CardReaderRemoteError.CollectFailed,
        CardReaderRemoteError.CreateIntentFailed,
        CardReaderRemoteError.UnexpectedReply,
        is CardReaderRemoteError.Unknown -> CardPaymentStatusErrorType.Generic
    }

    private fun mapFailureToUserMessage(cause: Throwable): String =
        when (cause) {
            is CardReaderRemoteConnectionLostException ->
                resourceProvider.getString(R.string.woopos_remote_payment_failed_connection_lost)
            else -> genericFailureMessage()
        }

    private fun isSimulatedReaderConnected(): Boolean =
        (remoteReaderSession.state.value as? WooPosRemoteReaderSession.State.Connected)?.reader?.isSimulated == true

    private fun genericFailureMessage(): String =
        resourceProvider.getString(R.string.woopos_remote_payment_failed_generic)

    private fun logTerminalPaymentPreparationRouteCheckFailed() {
        logger.d("Skipping terminal payment preparation because the WCPay endpoint could not be verified.")
    }

    private suspend fun capture(orderId: Long, paymentIntentId: String): Result =
        when (val response = cardReaderStore.capturePaymentIntent(orderId, paymentIntentId)) {
            is CapturePaymentResponse.Successful -> Result.Completed
            is CapturePaymentResponse.Error -> Result.Failed(
                message = response.message,
                errorDescription = "capture_failed: ${response::class.java.simpleName} - ${response.message}",
                isUnresolved = response is CapturePaymentResponse.Error.GenericError ||
                    response is CapturePaymentResponse.Error.ServerError ||
                    response is CapturePaymentResponse.Error.NetworkError ||
                    response is CapturePaymentResponse.Error.GatewayError,
            )
        }

    private suspend fun simulatePayment(onCaptureStarting: suspend () -> Unit): Result {
        trackingInfoKeeper.setPaymentMethodType(PaymentMethodType.CARD_PRESENT.stringRepresentation)
        onCaptureStarting()
        delay(SIMULATED_PAYMENT_DELAY_MS)
        return Result.Completed
    }

    sealed class Result {
        data object Completed : Result()

        data class Failed(
            val message: String,
            val errorDescription: String = message,
            val errorType: CardPaymentStatusErrorType = CardPaymentStatusErrorType.Generic,
            val stripeErrorCode: String? = null,
            val isUnresolved: Boolean = false,
        ) : Result()
    }

    private companion object {
        const val SIMULATED_PAYMENT_DELAY_MS = 1_500L
    }
}
